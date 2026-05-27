#!/usr/bin/env bash
# =============================================================================
# Dual-write reconciler — Task S1
# =============================================================================
# Reads the message_bus_audit table written by O1's MessageBusAuditWriter
# and reports per-queue drift between RMQ and Redis publishes over a
# rolling window. This is the actual health signal that decides whether
# S1 dual-write soak is succeeding and S2 cutover can begin.
#
# How drift is measured
# ---------------------
# Each producer publish writes one row per backend it reached. In `dual`
# mode that's two rows per logical publish (one rmq, one redis), provided
# both adapters succeeded. Drift = |rmq_count - redis_count| / max(rmq_count,1)
# over the configured window. Per migration plan line 573:
#
#   ±0.5% drift over 24h windows is acceptable (MAXLEN trim and AOF
#   replay variance). >1% drift OR any DLQ growth blocks cutover.
#
# This script reports the drift_pct per queue and exits non-zero when the
# threshold is exceeded — wire it into cron / alertmanager-receiver to
# turn an existing accidental drift into a paging signal.
#
# Usage
# -----
#   ./scripts/rmq-redis-reconcile.sh                              # 24h, threshold 0.005
#   ./scripts/rmq-redis-reconcile.sh --window=1h --threshold=0.01
#   ./scripts/rmq-redis-reconcile.sh --queue=queue_log_money       # one queue
#   ./scripts/rmq-redis-reconcile.sh --json                        # machine-readable
#
# Args:
#   --window=<n><unit>   Lookback window for the audit query. Units:
#                        s,m,h,d. Default: 24h. Maps to MySQL INTERVAL.
#   --threshold=<float>  Drift fraction that triggers exit 1.
#                        Default: 0.005 (0.5%, the migration-plan SLO).
#   --queue=<name>       Limit the report to one queue.
#   --json               Emit JSON instead of table. Useful for cron-piped
#                        consumers (alertmanager amtool, jq, etc).
#   --include-zero       Include queues with no rmq+redis activity in the
#                        window (default: hide them — quiet queues are
#                        not informative).
#
# Exit codes
# ----------
#   0  all queues within tolerance
#   1  one or more queues exceeded --threshold
#   2  audit table missing / DB unreachable / preflight failure
# =============================================================================

set -euo pipefail

WINDOW="24h"
THRESHOLD="0.005"
QUEUE_FILTER=""
EMIT_JSON=0
INCLUDE_ZERO=0
for arg in "$@"; do
    case "${arg}" in
        --window=*)    WINDOW="${arg#--window=}" ;;
        --threshold=*) THRESHOLD="${arg#--threshold=}" ;;
        --queue=*)     QUEUE_FILTER="${arg#--queue=}" ;;
        --json)        EMIT_JSON=1 ;;
        --include-zero) INCLUDE_ZERO=1 ;;
        --help|-h)
            sed -n '2,55p' "$0"
            exit 0
            ;;
        *)
            echo "unknown flag: ${arg}" >&2
            exit 2
            ;;
    esac
done

# Translate Nh / Nd / Nm / Ns → INTERVAL N HOUR / DAY / MINUTE / SECOND.
case "${WINDOW}" in
    *s) interval_value="${WINDOW%s}"; interval_unit="SECOND" ;;
    *m) interval_value="${WINDOW%m}"; interval_unit="MINUTE" ;;
    *h) interval_value="${WINDOW%h}"; interval_unit="HOUR"   ;;
    *d) interval_value="${WINDOW%d}"; interval_unit="DAY"    ;;
    *) echo "bad --window=${WINDOW} (expected Ns|Nm|Nh|Nd)" >&2; exit 2 ;;
esac
if ! [[ "${interval_value}" =~ ^[0-9]+$ ]]; then
    echo "bad --window numeric part: ${interval_value}" >&2; exit 2
fi

# ---- env / connection ------------------------------------------------------
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
if [[ ! -f "${ENV_FILE}" ]]; then
    echo "missing ${ENV_FILE}" >&2; exit 2
fi
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sunwinkr-mysql}"
MYSQL_PASSWORD="$(grep -E '^MYSQL_ROOT_PASSWORD=' "${ENV_FILE}" | head -1 | cut -d= -f2-)"
if [[ -z "${MYSQL_PASSWORD}" ]]; then
    echo "MYSQL_ROOT_PASSWORD not in .env" >&2; exit 2
fi

MYSQL() {
    docker exec "${MYSQL_CONTAINER}" \
        mysql -uroot -p"${MYSQL_PASSWORD}" -N -B vinplay "$@" 2>/dev/null
}

# Audit table preflight.
audit_exists=$(MYSQL -e "SELECT COUNT(*) FROM information_schema.tables \
    WHERE table_schema='vinplay' AND table_name='message_bus_audit'" || echo ERR)
if [[ "${audit_exists}" != "1" ]]; then
    echo "ERROR: vinplay.message_bus_audit not found (expected once S1 migration is applied)" >&2
    exit 2
fi

# ---- query ------------------------------------------------------------------
# `success=1` only counts publishes the adapter completed without error.
# Rows where one backend failed and the other succeeded ARE the drift signal,
# so we count both successes — the difference is what matters.
where_q=""
if [[ -n "${QUEUE_FILTER}" ]]; then
    # MySQL escapes single quote by doubling it.
    safe_q=${QUEUE_FILTER//\'/\'\'}
    where_q="AND queue_name = '${safe_q}'"
fi

having_clause=""
if [[ "${INCLUDE_ZERO}" != "1" ]]; then
    having_clause="HAVING (rmq_count + redis_count) > 0"
fi

SQL=$(cat <<EOF
SELECT queue_name,
       SUM(backend='rmq'   AND success=1) AS rmq_count,
       SUM(backend='redis' AND success=1) AS redis_count,
       SUM(backend='rmq'   AND success=0) AS rmq_fail,
       SUM(backend='redis' AND success=0) AS redis_fail,
       ROUND(
         ABS(SUM(backend='rmq' AND success=1) - SUM(backend='redis' AND success=1))
         / GREATEST(SUM(backend='rmq' AND success=1), 1),
         4
       ) AS drift_pct
FROM message_bus_audit
WHERE ts >= NOW() - INTERVAL ${interval_value} ${interval_unit}
  ${where_q}
GROUP BY queue_name
${having_clause}
ORDER BY drift_pct DESC, queue_name ASC;
EOF
)

rows=$(MYSQL -e "${SQL}" || echo ERR)
if [[ "${rows}" == "ERR" ]]; then
    echo "ERROR: reconciler query failed" >&2
    exit 2
fi

# ---- evaluation -------------------------------------------------------------
exceeded=0
total=0
if [[ "${EMIT_JSON}" == "1" ]]; then
    printf '{ "window": "%s", "threshold": %s, "queues": [' "${WINDOW}" "${THRESHOLD}"
    first=1
fi

# tabular header (skip if json)
if [[ "${EMIT_JSON}" != "1" ]]; then
    printf '%-40s  %10s  %10s  %8s  %8s  %8s  %s\n' \
        "queue_name" "rmq_ok" "redis_ok" "rmq_fail" "redis_fail" "drift" "status"
    printf '%-40s  %10s  %10s  %8s  %8s  %8s  %s\n' \
        "----------------------------------------" \
        "----------" "----------" "--------" "--------" "--------" "-------"
fi

while IFS=$'\t' read -r qn rmq_ok redis_ok rmq_fail redis_fail drift; do
    [[ -z "${qn}" ]] && continue
    total=$((total + 1))
    # awk for float compare to avoid bash's lack of float arithmetic.
    over=$(awk -v d="${drift}" -v t="${THRESHOLD}" 'BEGIN { print (d+0 > t+0) ? 1 : 0 }')
    if [[ "${over}" == "1" ]]; then
        status="OVER"
        exceeded=$((exceeded + 1))
    else
        status="ok"
    fi
    if [[ "${EMIT_JSON}" == "1" ]]; then
        if [[ "${first}" == "1" ]]; then first=0; else printf ','; fi
        printf '{"queue":"%s","rmq_ok":%s,"redis_ok":%s,"rmq_fail":%s,"redis_fail":%s,"drift_pct":%s,"over":%s}' \
            "${qn}" "${rmq_ok}" "${redis_ok}" "${rmq_fail}" "${redis_fail}" "${drift}" "${over}"
    else
        printf '%-40s  %10s  %10s  %8s  %8s  %8s  %s\n' \
            "${qn}" "${rmq_ok}" "${redis_ok}" "${rmq_fail}" "${redis_fail}" "${drift}" "${status}"
    fi
done <<< "${rows}"

if [[ "${EMIT_JSON}" == "1" ]]; then
    printf '], "exceeded": %s, "total": %s }\n' "${exceeded}" "${total}"
else
    echo
    if [[ "${total}" == "0" ]]; then
        echo "(no audit rows in window — either no traffic, or audit not yet writing)"
    fi
    echo "queues=${total}  exceeded_threshold(${THRESHOLD})=${exceeded}"
fi

if [[ "${exceeded}" -gt 0 ]]; then
    exit 1
fi
exit 0

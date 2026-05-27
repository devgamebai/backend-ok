#!/usr/bin/env bash
# =============================================================================
# MessageBus soak monitor — cron-friendly drift watcher
# =============================================================================
# Wraps scripts/rmq-redis-reconcile.sh for hands-off cron execution during
# the S1 dual-write soak (1-2 weeks). Runs the reconciler, captures output,
# alerts to Telegram if drift exceeds threshold OR if the script itself
# fails (audit table missing, MySQL down, etc.).
#
# Recommended cron: every hour during the soak window.
#   0 * * * * /root/sunwinkr/sunwinkr-backend/scripts/messagebus-soak-monitor.sh \
#               --window=1h --threshold=0.005 \
#               >> /var/log/messagebus-soak.log 2>&1
#
# After 24h of clean readings, switch to every 4 hours; after a week, every
# 12h. The threshold matches the 0.5% SLO from the migration plan (line 573).
#
# Usage
# -----
#   ./scripts/messagebus-soak-monitor.sh                           # 1h window, 0.5% threshold
#   ./scripts/messagebus-soak-monitor.sh --window=4h --threshold=0.003
#   ./scripts/messagebus-soak-monitor.sh --queue=queue_log_money   # one-queue mode
#   ./scripts/messagebus-soak-monitor.sh --quiet                   # only alert on breach (no clean-run notify)
#
# Args:
#   --window=<n><unit>   Reconciler lookback window (s|m|h|d). Default: 1h.
#   --threshold=<float>  Drift fraction triggering Telegram alert.
#                        Default: 0.005 (0.5%).
#   --queue=<name>       Limit to one queue (default: all).
#   --quiet              Only alert on breach. Default: also send a daily
#                        clean-run summary (see "daily summary" below).
#   --no-telegram        Do not call Telegram; just print + exit code.
#                        Useful for testing / standalone runs.
#
# Telegram alerting
# -----------------
# Reads TELEGRAM_AUDIT_BOT_TOKEN and TELEGRAM_AUDIT_CHAT_ID from .env (the
# same channel SUN-1141 wallet integrity uses). If either is missing, falls
# back to printing the alert without sending — useful for staging dry runs.
#
# Daily summary
# -------------
# To avoid silence-during-clean-soak, this script emits one "clean run"
# message per UTC day (the first invocation each day). Subsequent clean
# runs that day are silent. Use --quiet to suppress the daily summary
# entirely if your channel is noisy.
# =============================================================================

set -euo pipefail

WINDOW="1h"
THRESHOLD="0.005"
QUEUE_FILTER=""
QUIET=0
NO_TELEGRAM=0
for arg in "$@"; do
    case "${arg}" in
        --window=*)    WINDOW="${arg#--window=}" ;;
        --threshold=*) THRESHOLD="${arg#--threshold=}" ;;
        --queue=*)     QUEUE_FILTER="${arg#--queue=}" ;;
        --quiet)       QUIET=1 ;;
        --no-telegram) NO_TELEGRAM=1 ;;
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

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
RECONCILER="${REPO_ROOT}/scripts/rmq-redis-reconcile.sh"
STATE_DIR="${STATE_DIR:-/var/lib/messagebus-soak}"
mkdir -p "${STATE_DIR}" 2>/dev/null || STATE_DIR="/tmp/messagebus-soak"
mkdir -p "${STATE_DIR}"

if [[ ! -x "${RECONCILER}" ]]; then
    echo "ERROR: ${RECONCILER} not found / not executable" >&2; exit 2
fi

if [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^TELEGRAM_AUDIT_(BOT_TOKEN|CHAT_ID)=' "${ENV_FILE}")
fi

now_iso="$(date -u +'%Y-%m-%d %H:%M:%S UTC')"
today_utc="$(date -u +'%Y-%m-%d')"

# ---- run reconciler --------------------------------------------------------
recon_args=("--window=${WINDOW}" "--threshold=${THRESHOLD}" "--json")
if [[ -n "${QUEUE_FILTER}" ]]; then
    recon_args+=("--queue=${QUEUE_FILTER}")
fi

set +e
output=$("${RECONCILER}" "${recon_args[@]}" 2>&1)
rc=$?
set -e

# ---- alert helper ----------------------------------------------------------
telegram_send() {
    local msg="$1"
    if [[ "${NO_TELEGRAM}" == "1" ]]; then
        echo "[would send Telegram]"
        echo "${msg}"
        return
    fi
    if [[ -z "${TELEGRAM_AUDIT_BOT_TOKEN:-}" || -z "${TELEGRAM_AUDIT_CHAT_ID:-}" ]]; then
        echo "[Telegram unconfigured — printing instead]"
        echo "${msg}"
        return
    fi
    curl -fsS -X POST \
        "https://api.telegram.org/bot${TELEGRAM_AUDIT_BOT_TOKEN}/sendMessage" \
        --data-urlencode "chat_id=${TELEGRAM_AUDIT_CHAT_ID}" \
        --data-urlencode "text=${msg}" \
        --data-urlencode "parse_mode=Markdown" \
        >/dev/null || echo "[Telegram send failed; alert text was printed above]"
}

# ---- decide what to send ---------------------------------------------------
if [[ "${rc}" == "2" ]]; then
    # Preflight failure (audit table missing, DB unreachable, etc.).
    msg="*MessageBus soak monitor — PREFLIGHT FAILED*
\`${now_iso}\`

The reconciler script could not run.
\`\`\`
${output}
\`\`\`

Likely causes:
- vinplay.message_bus_audit table missing → run migrations/20260505_message_bus_audit.sql
- MySQL container unreachable
- Producer config not deployed (check MESSAGE_BUS_DEFAULT in .env)"
    telegram_send "${msg}"
    exit 2
fi

if [[ "${rc}" == "1" ]]; then
    # One or more queues exceeded threshold.
    over_summary=$(echo "${output}" | python3 -c '
import json, sys
data = json.load(sys.stdin)
over = [q for q in data.get("queues", []) if q.get("over") == 1]
if not over:
    print("(no queues over threshold; this is unexpected with rc=1)")
else:
    for q in over:
        print(f"- {q[\"queue\"]}: rmq={q[\"rmq_ok\"]} redis={q[\"redis_ok\"]} drift={q[\"drift_pct\"]}")
' 2>/dev/null || echo "(unable to parse JSON output)")
    msg="*MessageBus drift ALERT*
\`${now_iso}\`
window=${WINDOW} threshold=${THRESHOLD}

Queues exceeding threshold:
${over_summary}

Check:
- \`./scripts/rmq-redis-reconcile.sh --window=${WINDOW}\`
- \`./scripts/redis-dlq-inspect.sh <queue>\`
- Grafana: messagebus-* panels"
    telegram_send "${msg}"
    exit 1
fi

# rc == 0: all queues within tolerance.
state_file="${STATE_DIR}/last_clean_summary"
last_summary_date=""
[[ -f "${state_file}" ]] && last_summary_date="$(cat "${state_file}")"

if [[ "${QUIET}" == "1" ]]; then
    # No alert on clean run.
    exit 0
fi

if [[ "${last_summary_date}" == "${today_utc}" ]]; then
    # Already sent today's clean summary.
    exit 0
fi

# First clean run of the day — emit summary.
totals=$(echo "${output}" | python3 -c '
import json, sys
data = json.load(sys.stdin)
qs = data.get("queues", [])
total_q = len(qs)
total_rmq = sum(int(q["rmq_ok"]) for q in qs)
total_redis = sum(int(q["redis_ok"]) for q in qs)
print(f"queues={total_q}  rmq_ok={total_rmq}  redis_ok={total_redis}")
' 2>/dev/null || echo "(parse error)")

msg="*MessageBus soak — daily clean summary*
\`${now_iso}\`
window=${WINDOW} threshold=${THRESHOLD}

${totals}
All queues within tolerance. No action needed."
telegram_send "${msg}"
echo "${today_utc}" > "${state_file}"
exit 0

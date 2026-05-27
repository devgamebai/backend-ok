#!/usr/bin/env bash
# =============================================================================
# MessageBus per-queue flip — S1 dual-write enable / S2 redis cutover
# =============================================================================
# Automates the per-queue flip procedure documented in:
#   - docs/RUNBOOK_S2_CUTOVER.md (S2 cutover gates)
#   - docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md §S1, §S2
#
# Two modes:
#   --mode=dual    — S1 dual-write enable (rmq + redis). Light gates.
#   --mode=redis   — S2 cutover (redis only). Heavy gates including
#                    drift check, RMQ depth zero, DLQ zero.
#   --mode=rmq     — Rollback to rmq-only. No gates.
#
# DRY-RUN BY DEFAULT — requires `--apply` to actually edit .env and restart
# containers. The dry-run path runs all gates and reports pass/fail without
# making changes.
#
# Usage
# -----
#   ./scripts/messagebus-flip-queue.sh --queue=queue_server_info --mode=dual
#   ./scripts/messagebus-flip-queue.sh --queue=queue_server_info --mode=dual --apply
#   ./scripts/messagebus-flip-queue.sh --queue=queue_payment --mode=redis --apply --confirm-hot
#   ./scripts/messagebus-flip-queue.sh --queue=queue_log_money --mode=rmq --apply  # rollback
#
# Args:
#   --queue=<name>          Logical queue name (required).
#   --mode=<rmq|dual|redis> Target mode (required).
#   --apply                 Actually edit .env + restart containers. Default
#                           is dry-run (gates only).
#   --confirm-hot           Required for `--mode=redis` on wallet-hot-path
#                           queues (queue_payment*, queue_log_money*,
#                           queue_log_gsc_bets_async). Belt-and-suspenders.
#   --restart=<containers>  Comma-separated container names to restart after
#                           flip. Default:
#                           sunwinkr-vbee,sunwinkr-backend-api,sunwinkr-portal-api
#                           Some queues need game-* containers too — see
#                           runbook §S2 for the per-queue producer map.
#   --skip-drift            For --mode=redis: skip the drift check gate.
#                           Use only when reconciler is unavailable (e.g.
#                           audit table being recreated).
#
# Exit codes
# ----------
#   0  flip completed (or all gates pass in dry-run)
#   1  one or more gates failed (no change made)
#   2  bad args / preflight (docker not reachable, etc.)
# =============================================================================

set -euo pipefail

QUEUE=""
MODE=""
APPLY=0
CONFIRM_HOT=0
SKIP_DRIFT=0
RESTART_LIST="sunwinkr-vbee,sunwinkr-backend-api,sunwinkr-portal-api"

for arg in "$@"; do
    case "${arg}" in
        --queue=*)    QUEUE="${arg#--queue=}" ;;
        --mode=*)     MODE="${arg#--mode=}" ;;
        --apply)      APPLY=1 ;;
        --confirm-hot) CONFIRM_HOT=1 ;;
        --restart=*)  RESTART_LIST="${arg#--restart=}" ;;
        --skip-drift) SKIP_DRIFT=1 ;;
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

if [[ -z "${QUEUE}" || -z "${MODE}" ]]; then
    echo "usage: $0 --queue=<name> --mode=<rmq|dual|redis> [--apply] [--confirm-hot] [--restart=...] [--skip-drift]" >&2
    exit 2
fi

case "${MODE}" in
    rmq|dual|redis) ;;
    *) echo "ERROR: --mode must be rmq|dual|redis (got '${MODE}')" >&2; exit 2 ;;
esac

# Wallet-hot-path safety latch (only for redis cutover; dual is safe).
case "${QUEUE}" in
    queue_payment|queue_payment_*|queue_log_money|queue_log_money_extra|queue_log_report_user_balance|queue_log_gsc_bets_async)
        if [[ "${MODE}" == "redis" && "${APPLY}" == "1" && "${CONFIRM_HOT}" != "1" ]]; then
            echo "ERROR: '${QUEUE}' is wallet-hot-path. Add --confirm-hot for --mode=redis." >&2
            exit 2
        fi
        ;;
esac

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
ENV_VAR="MESSAGE_BUS_$(echo "${QUEUE}" | tr 'a-z_' 'A-Z_')"

REDIS_CONTAINER="${REDIS_CONTAINER:-sunwinkr-redis}"
RMQ_CONTAINER="${RMQ_CONTAINER:-sunwinkr-rabbitmq}"
REDIS_DB="${REDIS_DB:-1}"

REDIS_PASSWORD="$(grep -E '^REDIS_PASSWORD=' "${ENV_FILE}" | head -1 | cut -d= -f2-)"

REDIS_CLI() {
    if [[ -n "${REDIS_PASSWORD}" ]]; then
        docker exec "${REDIS_CONTAINER}" \
            redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning -n "${REDIS_DB}" "$@"
    else
        docker exec "${REDIS_CONTAINER}" redis-cli -n "${REDIS_DB}" "$@"
    fi
}

fail_count=0
pass() { printf '  [PASS] %s\n' "$*"; }
fail() { printf '  [FAIL] %s\n' "$*" >&2; fail_count=$((fail_count + 1)); }
hdr()  { printf '\n=== %s ===\n' "$*"; }

echo "[flip] queue=${QUEUE} mode=${MODE} apply=${APPLY} env_var=${ENV_VAR}"
echo "[flip] restart_list=${RESTART_LIST}"

# ---- Gates --------------------------------------------------------------
hdr "Gate A: current state"
current=$(grep -E "^${ENV_VAR}=" "${ENV_FILE}" 2>/dev/null | head -1 | cut -d= -f2- || echo "")
if [[ -z "${current}" ]]; then current="rmq (default)"; fi
echo "  current ${ENV_VAR}=${current}"
if [[ "${current}" == "${MODE}" || "${current}" == "${MODE} (default)" ]]; then
    echo "  no-op: already at target mode"
    exit 0
fi

if [[ "${MODE}" == "redis" ]]; then
    hdr "Gate B: drift check (last 24h)"
    if [[ "${SKIP_DRIFT}" == "1" ]]; then
        echo "  skipped (--skip-drift)"
    else
        if "${REPO_ROOT}/scripts/rmq-redis-reconcile.sh" --queue="${QUEUE}" --window=24h >/tmp/flip-drift.txt 2>&1; then
            pass "drift below threshold"
        else
            cat /tmp/flip-drift.txt
            fail "drift exceeds 0.5% threshold OR no audit data — investigate before cutover"
        fi
    fi

    hdr "Gate C: RMQ queue empty"
    rmq_depth=$(docker exec "${RMQ_CONTAINER}" rabbitmqctl list_queues -p / name messages 2>/dev/null \
        | awk -v q="${QUEUE}" '$1==q { print $2 }' || echo "")
    if [[ -z "${rmq_depth}" ]]; then
        fail "queue '${QUEUE}' not found in RabbitMQ"
    elif [[ "${rmq_depth}" -gt 0 ]]; then
        fail "RMQ queue depth=${rmq_depth} (expected 0; wait for consumer to drain)"
    else
        pass "RMQ queue depth=0"
    fi

    hdr "Gate D: Redis stream healthy + DLQ empty"
    stream_len=$(REDIS_CLI XLEN "{${QUEUE}}.stream" 2>/dev/null | tr -d '\r' || echo "0")
    dlq_len=$(REDIS_CLI XLEN "{${QUEUE}}.dlq" 2>/dev/null | tr -d '\r' || echo "0")
    echo "  stream length=${stream_len} dlq=${dlq_len}"
    if [[ "${dlq_len}" -gt 0 ]]; then
        fail "DLQ has ${dlq_len} entries — inspect with redis-dlq-inspect.sh"
    else
        pass "DLQ empty"
    fi
fi

if [[ "${fail_count}" -gt 0 ]]; then
    echo
    echo "[flip] ${fail_count} gate(s) failed; not flipping."
    exit 1
fi

echo
if [[ "${APPLY}" != "1" ]]; then
    echo "[flip] DRY-RUN — all gates pass. Re-run with --apply to flip."
    exit 0
fi

# ---- Apply --------------------------------------------------------------
hdr "Applying flip"

# Idempotent: remove any existing line then append the new one.
sed -i "/^${ENV_VAR}=/d" "${ENV_FILE}"
if [[ "${MODE}" != "rmq" ]]; then
    # rmq is the default per MESSAGE_BUS_DEFAULT — no need to write the line.
    echo "${ENV_VAR}=${MODE}" >> "${ENV_FILE}"
fi
echo "  .env updated: ${ENV_VAR}=${MODE}"

# Restart producers.
echo "  restarting: ${RESTART_LIST}"
IFS=',' read -ra containers <<< "${RESTART_LIST}"
for c in "${containers[@]}"; do
    if docker ps --format '{{.Names}}' | grep -qx "${c}"; then
        docker restart "${c}" >/dev/null
        echo "    restarted ${c}"
    else
        echo "    [WARN] container ${c} not running, skipping"
    fi
done

# Wait for restart settle then verify.
echo "  waiting 60s for restart to settle..."
sleep 60

hdr "Post-flip verification"
if [[ "${MODE}" == "dual" || "${MODE}" == "redis" ]]; then
    new_xlen=$(REDIS_CLI XLEN "{${QUEUE}}.stream" 2>/dev/null | tr -d '\r' || echo "0")
    if [[ "${new_xlen}" -gt "${stream_len:-0}" ]]; then
        pass "Redis stream growing (${stream_len:-0} → ${new_xlen})"
    else
        echo "  [INFO] Redis stream length unchanged (${new_xlen}). Either queue is idle or producer didn't pick up flag."
    fi
fi

if [[ "${MODE}" == "redis" ]]; then
    new_rmq_depth=$(docker exec "${RMQ_CONTAINER}" rabbitmqctl list_queues -p / name messages 2>/dev/null \
        | awk -v q="${QUEUE}" '$1==q { print $2 }' || echo "")
    if [[ "${new_rmq_depth}" == "0" ]]; then
        pass "RMQ queue stays empty (no producers leaking)"
    else
        echo "  [WARN] RMQ depth=${new_rmq_depth} (some producer still publishing to RMQ — check container restart list)"
    fi
fi

echo
echo "[flip] done. ${ENV_VAR}=${MODE}"
echo "[flip] watch for soak window:"
echo "  - ${MODE} → wait $([[ "${MODE}" == "redis" ]] && echo 24h || echo "1-2h"), then run reconciler:"
echo "      ./scripts/rmq-redis-reconcile.sh --queue=${QUEUE} --window=24h"
echo "  - rollback if needed:"
echo "      ./scripts/messagebus-flip-queue.sh --queue=${QUEUE} --mode=$([[ "${MODE}" == "redis" ]] && echo dual || echo rmq) --apply"

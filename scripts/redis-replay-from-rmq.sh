#!/usr/bin/env bash
# =============================================================================
# RMQ → Redis Streams replay — Task O3
# =============================================================================
# Drains the named RMQ queue and re-publishes each message to the matching
# Redis stream `{queue}.stream` on DB 1, preserving the wire format that
# RedisStreamMessageBus uses (b=BaseMessage.toBytes(), c=command id as UTF-8
# digits). Replayed entries are indistinguishable to consumers from
# producer-published ones.
#
# DRY-RUN BY DEFAULT — requires `--apply` to actually drain RMQ and write to
# Redis. The dry-run path peeks one batch from RMQ with requeue=true so no
# message state is lost while you eyeball the script's plan.
#
# When to use
# -----------
# During the dual-write soak (S1) Redis may be unhealthy at publish time
# while RMQ keeps accepting writes. The DualWriteMessageBus swallows the
# Redis-side failure (best-effort, see DualWriteMessageBus.java); the audit
# table will show `rmq=success, redis=failure` rows for the affected window.
# After Redis recovers, this script catches Redis up by draining the still-
# queued RMQ messages and XADD-ing them in.
#
# Operator MUST stop RMQ consumers for the queue first (e.g. by stopping the
# `vbee` container or pausing the consumer at the framework). Otherwise live
# consumers will drain the messages from RMQ before this script can read
# them, and the messages will be lost from Redis permanently.
#
# Wire-format caveat
# ------------------
# AMQP body bytes are passed straight into `b`. BaseMessage.toBytes() on this
# codebase emits UTF-8 JSON (content-type=text/plain, content-encoding=UTF-8
# per RMQPublishTask), so all 41 in-tree queues are safe with the
# bash+redis-cli path used here. If a future queue introduces non-UTF-8
# binary payloads, this script will mangle them — fall back to the Java
# replay tool (TBD; not built today).
#
# Usage
# -----
#   ./scripts/redis-replay-from-rmq.sh <queue>                  # dry-run
#   ./scripts/redis-replay-from-rmq.sh <queue> --apply          # replay all
#   ./scripts/redis-replay-from-rmq.sh <queue> --apply --count=200
#   ./scripts/redis-replay-from-rmq.sh queue_payment --apply --confirm-hot
#
# Args:
#   <queue>                 Logical queue name (no `{}` braces — the script
#                           wraps it as the cluster hashtag, matching
#                           StreamNames.java).
#   --apply                 Actually drain RMQ and write to Redis. Default
#                           is dry-run (peek + requeue, no XADD).
#   --count=<n>             Hard cap on entries replayed (default: all).
#   --confirm-hot           Required when targeting a wallet-hot-path queue
#                           (queue_payment, queue_log_money,
#                           queue_log_gsc_bets_async). Belt-and-suspenders
#                           against an operator running this on the wrong
#                           queue.
#
# Auth & target
# -------------
# Reads RABBITMQ_USER, RABBITMQ_PASSWORD, REDIS_PASSWORD from .env. Both
# RMQ and Redis are reached via `docker exec` into their respective
# containers (sunwinkr-rabbitmq, sunwinkr-redis), so this script works from
# the host regardless of container network reachability.
# =============================================================================

set -euo pipefail

# ---- args -------------------------------------------------------------------
QUEUE=""
APPLY=0
COUNT=0
CONFIRM_HOT=0
for arg in "$@"; do
    case "${arg}" in
        --apply) APPLY=1 ;;
        --count=*) COUNT="${arg#--count=}" ;;
        --confirm-hot) CONFIRM_HOT=1 ;;
        --help|-h)
            sed -n '2,55p' "$0"
            exit 0
            ;;
        --*)
            echo "unknown flag: ${arg}" >&2
            exit 2
            ;;
        *)
            if [[ -n "${QUEUE}" ]]; then
                echo "extra positional arg: ${arg}" >&2
                exit 2
            fi
            QUEUE="${arg}"
            ;;
    esac
done

if [[ -z "${QUEUE}" ]]; then
    echo "usage: $0 <queue> [--apply] [--count=N] [--confirm-hot]" >&2
    exit 2
fi

# Hot-path safety latch.
case "${QUEUE}" in
    queue_payment|queue_payment_*|queue_log_money|queue_log_money_extra|queue_log_report_user_balance|queue_log_gsc_bets_async)
        if [[ "${APPLY}" == "1" && "${CONFIRM_HOT}" != "1" ]]; then
            echo "ERROR: '${QUEUE}' is a wallet-hot-path queue. Add --confirm-hot to proceed." >&2
            exit 2
        fi
        ;;
esac

# ---- env --------------------------------------------------------------------
RMQ_CONTAINER="${RMQ_CONTAINER:-sunwinkr-rabbitmq}"
REDIS_CONTAINER="${REDIS_CONTAINER:-sunwinkr-redis}"
REDIS_DB="${REDIS_DB:-1}"

ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env"
if [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^(RABBITMQ_(USER|PASSWORD)|REDIS_PASSWORD)=' "${ENV_FILE}")
fi
RUSER="${RABBITMQ_USER:-guest}"
RPASS="${RABBITMQ_PASSWORD:-guest}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

STREAM="{${QUEUE}}.stream"

# ---- helpers ----------------------------------------------------------------
ADMIN() {
    docker exec "${RMQ_CONTAINER}" rabbitmqadmin -u "${RUSER}" -p "${RPASS}" "$@"
}
REDIS() {
    if [[ -n "${REDIS_PASSWORD}" ]]; then
        docker exec -i "${REDIS_CONTAINER}" \
            redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning -n "${REDIS_DB}" "$@"
    else
        docker exec -i "${REDIS_CONTAINER}" redis-cli -n "${REDIS_DB}" "$@"
    fi
}

# Sanity: source queue exists.
DEPTH=$(ADMIN list queues -f raw_json 2>/dev/null \
    | jq -r --arg q "${QUEUE}" '.[] | select(.name==$q) | .messages // 0')
if [[ -z "${DEPTH}" ]]; then
    echo "ERROR: queue '${QUEUE}' not found in RabbitMQ" >&2
    exit 1
fi

# Sanity: target stream key reachable on Redis.
if ! REDIS PING >/dev/null; then
    echo "ERROR: Redis not reachable via docker exec ${REDIS_CONTAINER}" >&2
    exit 1
fi

# Show plan.
mode="DRY-RUN"
[[ "${APPLY}" == "1" ]] && mode="APPLY"
echo "[replay] mode=${mode} queue=${QUEUE} rmq_depth=${DEPTH} count=${COUNT:-all} -> ${STREAM}"

if [[ "${APPLY}" != "1" ]]; then
    # Dry-run: peek one message, requeue, print summary.
    msg=$(ADMIN get queue="${QUEUE}" count=1 ackmode=ack_requeue_true --format=raw_json 2>/dev/null || echo '[]')
    if [[ "${msg}" == "[]" || -z "${msg}" ]]; then
        echo "[replay] queue is empty; nothing to do."
        exit 0
    fi
    cmd=$(echo "${msg}" | jq -r '.[0].properties.message_id // "0"')
    enc=$(echo "${msg}" | jq -r '.[0].payload_encoding // "string"')
    blen=$(echo "${msg}" | jq -r '.[0].payload | length')
    echo "[dry] sample msg: cmd=${cmd} payload_encoding=${enc} payload_chars=${blen}"
    echo "[dry] re-run with --apply to drain ${DEPTH} message(s) into ${STREAM}"
    exit 0
fi

# ---- apply path -------------------------------------------------------------
published=0
failed=0
while :; do
    if [[ "${COUNT}" -gt 0 && "${published}" -ge "${COUNT}" ]]; then
        break
    fi
    msg=$(ADMIN get queue="${QUEUE}" count=1 ackmode=ack_requeue_false --format=raw_json 2>/dev/null || echo '[]')
    if [[ "${msg}" == "[]" || -z "${msg}" ]]; then
        break
    fi

    cmd=$(echo "${msg}" | jq -r '.[0].properties.message_id // "0"')
    enc=$(echo "${msg}" | jq -r '.[0].payload_encoding // "string"')
    raw=$(echo "${msg}" | jq -r '.[0].payload')

    if [[ "${enc}" == "base64" ]]; then
        body=$(printf '%s' "${raw}" | base64 -d)
    else
        body="${raw}"
    fi

    # XADD with body via stdin (-x) to keep it binary-safe-ish for UTF-8.
    # No MAXLEN here — replay is a one-shot catch-up; the next normal
    # producer publish will reapply MAXLEN trimming.
    if printf '%s' "${body}" \
        | REDIS -x XADD "${STREAM}" '*' c "${cmd}" b - >/dev/null; then
        published=$((published + 1))
        if (( published % 100 == 0 )); then
            echo "[replay] ${published} published..."
        fi
    else
        failed=$((failed + 1))
        echo "[replay] XADD failed for cmd=${cmd}; RMQ message already acked (lost)" >&2
        if [[ "${failed}" -ge 3 ]]; then
            echo "[replay] aborting after ${failed} XADD failures" >&2
            break
        fi
    fi
done

echo "[replay] done. published=${published} failed=${failed} -> ${STREAM}"
exit 0

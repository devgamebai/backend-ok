#!/usr/bin/env bash
# =============================================================================
# Redis Streams DLQ replay — Task O2
# =============================================================================
# Reads `{queue}.dlq`, optionally filters, and replays selected entries back
# to `{queue}.stream`. DRY-RUN BY DEFAULT — requires `--apply` to actually
# write. Standard ops-script safety latch: an operator running this against
# `queue_payment` and accidentally replaying 1000 dead-letter payments would
# be a real outage.
#
# Usage:
#   ./scripts/redis-replay-dlq.sh <queue>                       # dry-run
#   ./scripts/redis-replay-dlq.sh <queue> --apply               # replay all
#   ./scripts/redis-replay-dlq.sh <queue> --reason=max_deliveries --apply
#   ./scripts/redis-replay-dlq.sh <queue> --since=1730000000000 --apply
#   ./scripts/redis-replay-dlq.sh <queue> --apply --ack-after-replay
#
# Args:
#   <queue>                 Logical queue name (no `{}` braces — the script
#                           wraps it as the cluster hashtag, matching
#                           StreamNames.java).
#   --apply                 Actually write. Default is dry-run.
#   --reason=<str>          Only replay entries whose `reason` field equals
#                           <str>. The DLQ-promotion Lua script in
#                           RedisStreamConsumer always writes
#                           reason=max_deliveries today, but the field is a
#                           filter hook for future failure modes.
#   --since=<ms>            Only replay entries with stream-id >= <ms>-0.
#                           Stream ids are millisecond-prefixed, so `--since`
#                           takes a unix-millis timestamp.
#   --count=<n>             Hard cap on entries replayed (default: all).
#   --ack-after-replay      After successfully XADDing to the live stream,
#                           XACK the DLQ entry too. WARNING: DLQs are
#                           append-only streams without consumer groups in
#                           the current design, so XACK has no effect; this
#                           flag is reserved for the future when DLQs grow
#                           a consumer group for retention. Today it is
#                           a no-op accepted for forward compat.
#
# Auth & target
# -------------
# Reads REDIS_PASSWORD from .env. Connects to redis://redis:6379/1 (DB 1
# per Plan task P3). Falls back to docker exec into sunwinkr-redis if the
# host can't reach the redis container directly.
# =============================================================================

set -euo pipefail

# ---- args -------------------------------------------------------------------
QUEUE=""
APPLY=0
FILTER_REASON=""
SINCE_MS=""
COUNT_LIMIT=""
ACK_AFTER_REPLAY=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --apply) APPLY=1; shift ;;
        --reason=*) FILTER_REASON="${1#--reason=}"; shift ;;
        --since=*) SINCE_MS="${1#--since=}"; shift ;;
        --count=*) COUNT_LIMIT="${1#--count=}"; shift ;;
        --ack-after-replay) ACK_AFTER_REPLAY=1; shift ;;
        -h|--help)
            grep '^#' "$0" | head -45
            exit 0
            ;;
        --*) echo "Unknown flag: $1" >&2; exit 2 ;;
        *)
            if [[ -z "${QUEUE}" ]]; then QUEUE="$1"; else
                echo "Unexpected positional arg: $1" >&2; exit 2
            fi
            shift
            ;;
    esac
done

if [[ -z "${QUEUE}" ]]; then
    echo "usage: $0 <queue> [--apply] [--reason=<str>] [--since=<ms>] [--count=<n>] [--ack-after-replay]" >&2
    exit 2
fi

# ---- locate .env ------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/.env}"

if [[ -z "${REDIS_PASSWORD:-}" ]] && [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^REDIS_PASSWORD=' "${ENV_FILE}")
fi

if [[ -z "${REDIS_PASSWORD:-}" ]]; then
    echo "ERROR: REDIS_PASSWORD not set (env or ${ENV_FILE})" >&2
    exit 2
fi

REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_DB="${REDIS_DB:-1}"
REDIS_CONTAINER="${REDIS_CONTAINER:-sunwinkr-redis}"

# ---- pick redis-cli transport ----------------------------------------------
rcli() {
    if command -v redis-cli >/dev/null 2>&1 && \
       redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" \
                 --no-auth-warning -n "${REDIS_DB}" PING >/dev/null 2>&1; then
        redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" \
                  --no-auth-warning -n "${REDIS_DB}" "$@"
    elif docker inspect "${REDIS_CONTAINER}" >/dev/null 2>&1; then
        docker exec -e REDISCLI_AUTH="${REDIS_PASSWORD}" "${REDIS_CONTAINER}" \
            redis-cli --no-auth-warning -n "${REDIS_DB}" "$@"
    else
        echo "ERROR: cannot reach Redis (no host redis-cli + no ${REDIS_CONTAINER} docker container)" >&2
        exit 3
    fi
}

DLQ_KEY="{${QUEUE}}.dlq"
STREAM_KEY="{${QUEUE}}.stream"

# ---- check DLQ exists & has entries ----------------------------------------
LEN="$(rcli XLEN "${DLQ_KEY}" | tr -d '\r' || echo 0)"
if [[ "${LEN}" == "0" ]]; then
    echo "DLQ ${DLQ_KEY} is empty. Nothing to replay."
    exit 0
fi

MODE="DRY-RUN"
[[ "${APPLY}" == "1" ]] && MODE="APPLY"
echo "==> DLQ: ${DLQ_KEY}    target: ${STREAM_KEY}    length: ${LEN}    mode: ${MODE}"
[[ -n "${FILTER_REASON}" ]] && echo "    filter: reason=${FILTER_REASON}"
[[ -n "${SINCE_MS}" ]] && echo "    filter: since=${SINCE_MS}-0"
[[ -n "${COUNT_LIMIT}" ]] && echo "    cap: count<=${COUNT_LIMIT}"

# ---- read DLQ entries -------------------------------------------------------
# XRANGE returns entries in this redis-cli line format (one entry block):
#   <id>
#   <field1>
#   <value1>
#   <field2>
#   <value2>
#   ...
# We parse line-by-line with a small state machine. NOT json/jq — keeps the
# script dependency-free.
#
# `--since` becomes the XRANGE start id; default `-` (= beginning of stream).
START_ID="-"
[[ -n "${SINCE_MS}" ]] && START_ID="${SINCE_MS}-0"

RANGE_OUT="$(rcli XRANGE "${DLQ_KEY}" "${START_ID}" + | tr -d '\r')"

REPLAYED=0
SKIPPED=0
INSPECTED=0

# Parse loop. State: expecting an entry id (line starts with digit-dash) or
# alternating field/value lines until the next id.
current_id=""
current_body=""
current_command=""
current_orig_id=""
current_reason=""
field_pending=""

flush_entry() {
    [[ -z "${current_id}" ]] && return 0
    INSPECTED=$((INSPECTED + 1))

    # Apply --reason filter.
    if [[ -n "${FILTER_REASON}" ]] && [[ "${current_reason}" != "${FILTER_REASON}" ]]; then
        SKIPPED=$((SKIPPED + 1))
        return 0
    fi

    # Apply --count cap.
    if [[ -n "${COUNT_LIMIT}" ]] && [[ "${REPLAYED}" -ge "${COUNT_LIMIT}" ]]; then
        SKIPPED=$((SKIPPED + 1))
        return 0
    fi

    # Required fields: b (body), c (command). Anything else is informational.
    if [[ -z "${current_body}" ]] || [[ -z "${current_command}" ]]; then
        echo "  [skip] ${current_id} missing required b/c fields" >&2
        SKIPPED=$((SKIPPED + 1))
        return 0
    fi

    local body_preview="${current_body:0:64}"
    [[ ${#current_body} -gt 64 ]] && body_preview="${body_preview}..."

    if [[ "${APPLY}" == "0" ]]; then
        printf '  [dry] would XADD %s * b <%dB> c %s   (orig_id=%s reason=%s)\n' \
               "${STREAM_KEY}" "${#current_body}" "${current_command}" \
               "${current_orig_id}" "${current_reason}"
        REPLAYED=$((REPLAYED + 1))
    else
        # XADD to live stream. Preserves body/command exactly. We do NOT
        # copy the orig_id or reason — the live stream is for fresh
        # processing, those fields belong only on the DLQ.
        if rcli XADD "${STREAM_KEY}" '*' b "${current_body}" c "${current_command}" >/dev/null; then
            REPLAYED=$((REPLAYED + 1))
            printf '  [ok ] XADD %s ← %s   (body=%dB command=%s)\n' \
                   "${STREAM_KEY}" "${current_id}" "${#current_body}" "${current_command}"
            if [[ "${ACK_AFTER_REPLAY}" == "1" ]]; then
                # DLQ has no consumer group today; XACK is a documented
                # no-op kept for forward-compat. See header comment.
                :
            fi
        else
            echo "  [err] XADD failed for ${current_id}" >&2
            SKIPPED=$((SKIPPED + 1))
        fi
    fi
}

while IFS= read -r line; do
    # New entry id? Real Redis stream ids are <millis>-<seq> with at least
    # 10 digits in the millis part (covers 2001-now-and-beyond). Anything
    # shorter is almost certainly a field VALUE that happens to look like
    # a stream id (e.g. an `orig_id` field copied from a test stream that
    # was XADDed with explicit short ids). We also require us NOT to be
    # mid-field: if `field_pending` is set, this line is the value of the
    # previous field, not a new id.
    if [[ -z "${field_pending}" ]] && [[ "${line}" =~ ^[0-9]{10,}-[0-9]+$ ]]; then
        flush_entry
        current_id="${line}"
        current_body=""
        current_command=""
        current_orig_id=""
        current_reason=""
        field_pending=""
        continue
    fi

    # Alternating field/value. We only care about b, c, orig_id, reason.
    if [[ -z "${field_pending}" ]]; then
        field_pending="${line}"
    else
        case "${field_pending}" in
            b) current_body="${line}" ;;
            c) current_command="${line}" ;;
            orig_id) current_orig_id="${line}" ;;
            reason) current_reason="${line}" ;;
            *) : ;;  # ignore unknown fields
        esac
        field_pending=""
    fi
done <<< "${RANGE_OUT}"

# Flush trailing entry.
flush_entry

echo "==> inspected=${INSPECTED}  replayed=${REPLAYED}  skipped=${SKIPPED}"
if [[ "${APPLY}" == "0" ]]; then
    echo "    (dry-run; re-run with --apply to actually replay)"
fi

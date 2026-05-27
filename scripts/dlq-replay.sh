#!/usr/bin/env bash
# Replay messages from DLQ back to original queue.
#
# Drains the DLQ (basic_get with ack), re-publishes each body to the live
# queue. Stops when DLQ is empty or COUNT is reached.
#
# Usage:
#   scripts/dlq-replay.sh queue_log_money              # replay all
#   COUNT=50 scripts/dlq-replay.sh queue_log_money     # replay first 50
#   DRY_RUN=1 scripts/dlq-replay.sh queue_log_money    # peek without ack

set -euo pipefail

QNAME="${1:?usage: $0 <queue_name>  (without _dlq suffix)}"
DLQ="${QNAME}_dlq"
COUNT="${COUNT:-0}"      # 0 = all
DRY_RUN="${DRY_RUN:-0}"
CONTAINER="${RMQ_CONTAINER:-sunwinkr-rabbitmq}"

ENV_FILE="$(dirname "$0")/../.env"
if [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^RABBITMQ_(USER|PASSWORD)=' "${ENV_FILE}")
fi
USER="${RABBITMQ_USER:-guest}"
PASS="${RABBITMQ_PASSWORD:-guest}"

ADMIN() {
    docker exec "${CONTAINER}" rabbitmqadmin -u "${USER}" -p "${PASS}" "$@"
}

# Initial depth
DEPTH=$(docker exec "${CONTAINER}" rabbitmqctl list_queues name messages \
    | awk -v q="${DLQ}" '$1==q{print $2}')

if [[ -z "${DEPTH}" || "${DEPTH}" == "0" ]]; then
    echo "DLQ ${DLQ} is empty. Nothing to replay."
    exit 0
fi

echo "==> Replaying from ${DLQ} → ${QNAME} (depth=${DEPTH}, mode=$([ "${DRY_RUN}" = 1 ] && echo dry-run || echo live))"

REPLAYED=0
BATCH=20

while true; do
    if [[ "${COUNT}" -gt 0 && "${REPLAYED}" -ge "${COUNT}" ]]; then
        echo "Hit COUNT limit (${COUNT}). Stopping."
        break
    fi

    # Fetch batch (with auto-ack so messages are removed from DLQ)
    if [[ "${DRY_RUN}" = 1 ]]; then
        ACKMODE=ack_requeue_true   # requeue, don't remove
    else
        ACKMODE=ack_requeue_false  # consume, remove from DLQ
    fi

    BODIES=$(ADMIN get queue="${DLQ}" count="${BATCH}" ackmode="${ACKMODE}" 2>&1 \
        | tail -n +4 | head -n -1 || true)

    if [[ -z "${BODIES}" ]]; then
        break
    fi

    # rabbitmqadmin output is tabular. Best path: per-msg loop using a single
    # python helper to parse JSON output. For simplicity here, re-publish via
    # rabbitmqadmin "publish" — bodies must be re-extracted from JSON.

    # Use --format=raw_json to get parseable output:
    JSON=$(ADMIN --format=raw_json get queue="${DLQ}" count="${BATCH}" ackmode="${ACKMODE}" 2>/dev/null || echo "[]")

    # Replay loop in container so we don't need jq on host
    REPUBLISHED_THIS_BATCH=$(docker exec -i "${CONTAINER}" sh -c "cat > /tmp/dlq.json && python3 - <<'EOF'
import json, subprocess, sys
USER='${USER}'; PASS='${PASS}'; QNAME='${QNAME}'; DRY=${DRY_RUN}
msgs=json.load(open('/tmp/dlq.json'))
for m in msgs:
    body=m.get('payload','')
    props=m.get('properties',{}) or {}
    if DRY:
        print(f'WOULD REPLAY: {body[:80]}', file=sys.stderr)
        continue
    cmd=['rabbitmqadmin','-u',USER,'-p',PASS,'publish',
         f'routing_key={QNAME}',f'payload={body}','exchange=']
    subprocess.run(cmd, check=True, capture_output=True)
print(len(msgs))
EOF
" <<<"${JSON}" 2>&1 | tail -1)

    if [[ -z "${REPUBLISHED_THIS_BATCH}" || "${REPUBLISHED_THIS_BATCH}" -eq 0 ]]; then
        break
    fi

    REPLAYED=$((REPLAYED + REPUBLISHED_THIS_BATCH))
    echo "  replayed ${REPLAYED}/${DEPTH}"
done

echo "Done. Replayed ${REPLAYED} messages."

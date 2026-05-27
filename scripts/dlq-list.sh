#!/usr/bin/env bash
# List DLQ depth and recent message bodies for inspection.
#
# Usage:
#   scripts/dlq-list.sh                    # show depth of all DLQs
#   scripts/dlq-list.sh queue_log_money    # peek messages in one DLQ

set -euo pipefail

CONTAINER="${RMQ_CONTAINER:-sunwinkr-rabbitmq}"
QNAME="${1:-}"

if [[ -z "${QNAME}" ]]; then
    # Mode: list all DLQs and their depth
    printf "%-50s %10s\n" "DLQ" "MESSAGES"
    printf "%-50s %10s\n" "---" "--------"
    docker exec "${CONTAINER}" rabbitmqctl list_queues name messages 2>&1 \
        | grep -E '_dlq\s' \
        | awk '{ printf "%-50s %10s\n", $1, $2 }' \
        | sort
    exit 0
fi

# Mode: peek bodies in a specific DLQ
DLQ="${QNAME}_dlq"
COUNT="${COUNT:-10}"

# Resolve credentials from .env
ENV_FILE="$(dirname "$0")/../.env"
if [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^RABBITMQ_(USER|PASSWORD)=' "${ENV_FILE}")
fi
USER="${RABBITMQ_USER:-guest}"
PASS="${RABBITMQ_PASSWORD:-guest}"

echo "==> Peeking ${COUNT} messages from ${DLQ} (non-destructive)"
docker exec "${CONTAINER}" rabbitmqadmin -u "${USER}" -p "${PASS}" \
    get queue="${DLQ}" count="${COUNT}" ackmode=ack_requeue_true 2>/dev/null \
    || echo "rabbitmqadmin not installed in container — install with: rabbitmq-plugins enable rabbitmq_management_cli"

#!/usr/bin/env bash
# Idempotent: applies dead-letter-exchange policies to wallet queues that
# were created (by RMQPool) without DLX args. Safe to re-run after every
# deploy; safe to re-run after rabbitmq-data volume reset.
#
# Why: RMQPool.getChannel does queueDeclarePassive then declares without
# DLX. Consumer side (RMQConsumer) tries to add DLX but loses race against
# producer. Policy retrofit is the only non-destructive fix without
# stopping services + draining queues.

set -euo pipefail

CONTAINER="${RMQ_CONTAINER:-sunwinkr-rabbitmq}"
RMQ_VHOST="${RMQ_VHOST:-/}"

# Wallet-critical queues. Side-effect queues (audit log, balance report)
# need DLX so failed messages don't disappear silently.
QUEUES=(
    log_money
    log_money_extra
    log_report_user_balance
    payment
    payment_minigame
    payment_gamebai
)

echo "==> Applying DLX policies to ${#QUEUES[@]} wallet queues..."

for Q in "${QUEUES[@]}"; do
    POLICY_NAME="DLX-${Q}"
    PATTERN="^queue_${Q}\$"
    DEFINITION="{\"dead-letter-exchange\":\"\",\"dead-letter-routing-key\":\"queue_${Q}_dlq\"}"

    docker exec "${CONTAINER}" rabbitmqctl set_policy \
        --vhost "${RMQ_VHOST}" \
        "${POLICY_NAME}" \
        "${PATTERN}" \
        "${DEFINITION}" \
        --apply-to queues \
        > /dev/null

    echo "  ✓ ${POLICY_NAME} → queue_${Q}_dlq"
done

echo
echo "==> Verifying policy application:"
docker exec "${CONTAINER}" rabbitmqctl list_queues name policy 2>/dev/null \
    | grep -E "queue_(log_money|log_money_extra|log_report_user_balance|payment|payment_minigame|payment_gamebai)\$" \
    | grep -v dlq

echo
echo "Done. Failed/NACKd messages on these queues will route to <queue>_dlq."

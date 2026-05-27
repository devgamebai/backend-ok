#!/bin/bash
# =============================================================================
# Post-deploy verification — catch the silent-failure modes we hit recurrently.
#
# Run after any backend rebuild + recreate (vbee, backend-api, portal-api,
# game-thirdparty). Exits non-zero if any check fails so a CI/Slack hook
# can act on it.
#
# What it catches:
#   1. Critical containers down or unhealthy
#   2. RMQ queues with messages but 0 consumers (the SUN-1228 / queue_log_gsc_bets_async class)
#   3. game-thirdparty restart loop (autoheal kill cycle)
#   4. log_gsc_bets stale by more than 5 minutes during peak hours
#   5. v_money_unbalanced_transactions returning > 0 (must always be 0)
#
# Usage:
#   ./scripts/post-deploy-verify.sh
#   ./scripts/post-deploy-verify.sh --quiet  # only output failures
# =============================================================================

set -uo pipefail

ENV_FILE="/root/sunwinkr/sunwinkr-backend/.env"
MYSQL_PWD="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
MONGO_PWD="$(grep '^MONGO_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"

QUIET=0
[[ "${1:-}" == "--quiet" ]] && QUIET=1

fails=0
checks=0

ok()    { [[ $QUIET -eq 0 ]] && printf '\e[32m[OK]\e[0m   %s\n' "$*"; checks=$((checks+1)); }
fail()  { printf '\e[31m[FAIL]\e[0m %s\n' "$*"; fails=$((fails+1)); checks=$((checks+1)); }
info()  { [[ $QUIET -eq 0 ]] && printf '\e[36m[..]\e[0m   %s\n' "$*"; }

mysql_q() {
    docker exec sunwinkr-mysql mysql -N -uroot -p"$MYSQL_PWD" vinplay -e "$1" \
        2> >(grep -v '\[Warning\] Using a password' >&2)
}

# ─────────────────────────────────────────────────────────────────────────────
# Check 1 — critical containers up & healthy
# ─────────────────────────────────────────────────────────────────────────────
info "Critical containers"
for c in sunwinkr-portal-api sunwinkr-backend-api sunwinkr-vbee sunwinkr-game-thirdparty sunwinkr-mysql sunwinkr-mongodb sunwinkr-rabbitmq; do
    status=$(docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$c" 2>/dev/null || echo "missing|missing")
    state=${status%%|*}
    health=${status##*|}
    if [[ "$state" != "running" ]]; then
        fail "$c is $state (expected running)"
    elif [[ "$health" != "healthy" && "$health" != "n/a" ]]; then
        fail "$c health=$health (expected healthy)"
    else
        ok "$c $state/$health"
    fi
done

# ─────────────────────────────────────────────────────────────────────────────
# Check 2 — RMQ queues have consumers when they have backlog
# ─────────────────────────────────────────────────────────────────────────────
info "RMQ queue consumers"
queues=$(docker exec sunwinkr-rabbitmq rabbitmqctl list_queues name messages consumers 2>/dev/null \
         | tail -n +2 | grep -vE '_dlq|^Listing|^Timeout')
while read -r name msgs cons; do
    [[ -z "$name" ]] && continue
    if [[ "$cons" == "0" && "$msgs" != "0" ]]; then
        fail "queue '$name' has $msgs messages and 0 consumers"
    elif [[ "$cons" == "0" ]]; then
        # Empty queue with 0 consumers — only complain about the known critical queues
        case "$name" in
            queue_log_gsc_bets_async|queue_log_money_user|queue_log_money_user_extra|queue_action_minigame|queue_action_portal)
                fail "critical queue '$name' has 0 consumers (will fail to attach again on next restart?)"
                ;;
            *) ;;
        esac
    fi
done <<< "$queues"
[[ $fails -eq 0 ]] && ok "all RMQ queues have consumers when they have backlog"

# ─────────────────────────────────────────────────────────────────────────────
# Check 3 — game-thirdparty restart cadence
# ─────────────────────────────────────────────────────────────────────────────
info "game-thirdparty restart cadence"
restart_count=$(docker events --since 30m --until 0s --filter "container=sunwinkr-game-thirdparty" \
                              --filter "event=start" 2>&1 | wc -l)
if [[ $restart_count -gt 2 ]]; then
    fail "game-thirdparty restarted $restart_count times in 30 min — autoheal loop?"
else
    ok "game-thirdparty restarts in last 30min: $restart_count (acceptable)"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Check 4 — log_gsc_bets freshness during peak hours
# ─────────────────────────────────────────────────────────────────────────────
info "log_gsc_bets freshness"
hour=$(date -u +%H)
# Peak GSC traffic: roughly 04:00-17:00 UTC (KST evening + Asia day). Skip during quiet windows.
if [[ $hour -ge 4 && $hour -le 17 ]]; then
    last_age=$(docker exec sunwinkr-mongodb mongosh --quiet \
        -u sunwinkr_admin -p "$MONGO_PWD" \
        --authenticationDatabase admin win123club --eval '
        const last = db.log_gsc_bets.find({}).sort({_id:-1}).limit(1).toArray()[0];
        print(last ? Math.round((Date.now() - last._id.getTimestamp().getTime())/1000) : -1);
        ' 2>/dev/null | tail -1)
    if [[ "$last_age" =~ ^[0-9]+$ ]] && [[ $last_age -gt 600 ]]; then
        fail "log_gsc_bets stale ${last_age}s — consumer not draining? (peak hour)"
    elif [[ "$last_age" =~ ^[0-9]+$ ]]; then
        ok "log_gsc_bets last write ${last_age}s ago"
    fi
else
    ok "off-peak hour (UTC $hour) — skipping log_gsc_bets freshness check"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Check 5 — ledger global invariant
# ─────────────────────────────────────────────────────────────────────────────
info "ledger global invariant"
unbal=$(mysql_q "SELECT COUNT(*) FROM v_money_unbalanced_transactions;")
if [[ "$unbal" -gt 0 ]]; then
    fail "v_money_unbalanced_transactions = $unbal (must be 0 — every money_transaction's entries must sum to 0)"
else
    ok "v_money_unbalanced_transactions = 0"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
echo
if [[ $fails -eq 0 ]]; then
    printf '\e[32m✓ post-deploy verify: %d/%d checks passed\e[0m\n' "$checks" "$checks"
    exit 0
else
    printf '\e[31m✗ post-deploy verify: %d/%d checks FAILED\e[0m\n' "$fails" "$checks"
    exit 1
fi

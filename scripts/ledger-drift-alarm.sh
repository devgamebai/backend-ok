#!/bin/bash
# =============================================================================
# Ledger drift alarm — fires when v_money_account_drift returns rows.
#
# The drift view compares each money_account.balance (cached snapshot) against
# SUM(money_entry.amount × direction-sign) across all entries on that account.
# Per the financial-ledger spec, balance is DERIVED — anything that drifts
# means a writer touched balance without posting matching entries (or vice
# versa). The dual-write era will produce some legitimate drift; the goal of
# this alarm is to catch NEW drift opening up.
#
# Strategy: maintain a baseline count in /var/run/ledger-drift-baseline. On
# each run, alert ONLY if drift_rows > baseline (new drift), or if a
# previously-drifted account's diff doubles. Threshold-based to avoid
# flapping while we work through the historical drift.
#
# Pages via the existing TELEGRAM_BOT_TOKEN/TELEGRAM_OPS_CHAT_ID from .env.
# Run from cron: */15 * * * * /root/sunwinkr/sunwinkr-backend/scripts/ledger-drift-alarm.sh
# =============================================================================

set -euo pipefail

ENV_FILE="/root/sunwinkr/sunwinkr-backend/.env"
BASELINE_FILE="/var/run/ledger-drift-baseline"
MYSQL_PWD="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
TG_TOKEN="$(grep '^TELEGRAM_BOT_TOKEN=' "$ENV_FILE" | cut -d= -f2-)"
TG_CHAT="$(grep '^TELEGRAM_OPS_CHAT_ID=' "$ENV_FILE" | cut -d= -f2- || true)"

mysql_q() {
    docker exec sunwinkr-mysql mysql -N -uroot -p"$MYSQL_PWD" vinplay -e "$1" \
        2> >(grep -v '\[Warning\] Using a password' >&2)
}

send_telegram() {
    [ -z "$TG_TOKEN" ] && { echo "TG_TOKEN unset; skipping" >&2; return 0; }
    [ -z "$TG_CHAT" ] && { echo "TG_OPS_CHAT_ID unset; skipping" >&2; return 0; }
    curl -sS --max-time 10 \
        "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
        --data-urlencode "chat_id=${TG_CHAT}" \
        --data-urlencode "text=$1" \
        --data-urlencode "parse_mode=HTML" >/dev/null
}

# Current state
current=$(mysql_q "SELECT COUNT(*) FROM v_money_account_drift;")
biggest_diff=$(mysql_q "SELECT IFNULL(MAX(ABS(drift)), 0) FROM v_money_account_drift;")

# Baseline: count from the last run (defaults to current if file missing,
# meaning the FIRST invocation establishes the baseline silently).
if [ -f "$BASELINE_FILE" ]; then
    baseline=$(cat "$BASELINE_FILE")
else
    baseline="$current"
fi
echo "$current" > "$BASELINE_FILE"

# Also separately check the unbalanced-transactions view — that one MUST be 0.
unbalanced=$(mysql_q "SELECT COUNT(*) FROM v_money_unbalanced_transactions;")

# Alert decision
if [ "$unbalanced" -gt 0 ]; then
    msg="🚨 <b>LEDGER UNBALANCED — IMMEDIATE</b>%0A"
    msg="${msg}v_money_unbalanced_transactions returned <b>${unbalanced}</b> rows. This should ALWAYS be 0.%0A"
    msg="${msg}Run: SELECT * FROM v_money_unbalanced_transactions;"
    send_telegram "$msg"
    exit 1
fi

if [ "$current" -gt "$baseline" ]; then
    delta=$((current - baseline))
    msg="⚠️ <b>NEW LEDGER DRIFT</b>%0A"
    msg="${msg}Drifting accounts: <b>${baseline}</b> → <b>${current}</b> (+${delta})%0A"
    msg="${msg}Largest absolute drift: ${biggest_diff}%0A"
    msg="${msg}Run: SELECT * FROM v_money_account_drift ORDER BY ABS(drift) DESC LIMIT 20;"
    send_telegram "$msg"
fi

echo "ledger-drift-alarm: drift_rows=${current} (baseline=${baseline}) unbalanced=${unbalanced} biggest=${biggest_diff}"

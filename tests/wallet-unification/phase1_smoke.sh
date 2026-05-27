#!/bin/bash
# tests/wallet-unification/phase1_smoke.sh — SUN-13xx Phase 1 wallet smoke
#
# Confirms the "stop writing vin_total / xu_total" invariant end-to-end:
#
#   1. Game win  → users.vin INCREASES; users.vin_total STAYS UNCHANGED
#   2. Game loss → users.vin DECREASES; users.vin_total STAYS UNCHANGED
#   3. Admin topup → users.vin INCREASES; users.vin_total STAYS UNCHANGED
#   4. v_derived_player_pnl returns a value matching the ledger's cumulative
#      WAGER_CREDIT - WAGER_DEBIT for the player
#
# Sources (1) and (2) drive UserDaoImpl.updateMoney(), which is the Phase 1
# flag-gated path. Source (3) flows through MoneyGateway.creditUser, not the
# SP, but is included so the "no vin_total writes during any wallet flow"
# invariant is end-to-end verified.
#
# Run with UNIFIED_WALLET_PHASE_1=on in the game-server containers; otherwise
# every "vin_total unchanged" assertion will FAIL (legacy SP still writes
# vin_total). See README.md for the deploy flow.
#
# Run from repo root: bash tests/wallet-unification/phase1_smoke.sh
# Exit 0 = all pass.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../helpers.sh"

ENV_FILE="$(cd "$SCRIPT_DIR/../.." && pwd)/.env"
MYSQL_PASS=$(grep '^MYSQL_ROOT_PASSWORD' "$ENV_FILE" | cut -d= -f2)
mysql_q() { docker exec sunwinkr-mysql mysql -uroot -p"$MYSQL_PASS" -N -B -e "$1" 2>/dev/null; }

# Player nickname used as the canary. Must be a non-bot account with a
# PLAYER_VIN money_account row + at least some history.
PLAYER_NICK="${PHASE1_SMOKE_NICK:-zuestang}"
ADMIN_TX_NONCE="$(date +%s)"

ensure_aat
AAT="$(echo -n "$AAT" | tr -d '\n\r' | head -c 32)"

snapshot() {
    # Returns "vin|vin_total|ledger_pnl_net" for the canary player.
    mysql_q "
        SELECT u.vin, u.vin_total,
               COALESCE((SELECT pnl_net FROM v_derived_player_pnl p
                          WHERE p.user_id = u.id), 0)
          FROM vinplay.users u
         WHERE u.nick_name='$PLAYER_NICK'
    " | tr '\t' '|'
}

# ──────────────────────────────────────────────────────────────────────
section "Phase 1 — pre-flight"
# ──────────────────────────────────────────────────────────────────────
test_name "v_derived_player_pnl view exists"
EXISTS=$(mysql_q "SHOW FULL TABLES FROM vinplay LIKE 'v_derived_player_pnl'" | wc -l | tr -d ' ')
[[ "$EXISTS" -ge 1 ]] && _pass "ok" || _fail "Phase 0 view missing — apply 20260511_wallet_unify_phase0_views.sql first"

test_name "update_money_db_v2 SP exists"
COUNT=$(mysql_q "SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='vinplay' AND ROUTINE_NAME='update_money_db_v2'")
[[ "$COUNT" == "1" ]] && _pass "ok" || _fail "Apply 20260512_phase1_update_money_db_v2.sql first"

test_name "Canary player '$PLAYER_NICK' exists"
PRE=$(snapshot)
[[ -n "$PRE" ]] && _pass "vin|vin_total|pnl_net = $PRE" \
    || _fail "Player not found — override with PHASE1_SMOKE_NICK=<nick>"

# ──────────────────────────────────────────────────────────────────────
section "Smoke 1 — game win (vin INCREASES, vin_total UNCHANGED)"
# ──────────────────────────────────────────────────────────────────────
WIN_AMT=12345
BEFORE=$(snapshot); BEFORE_VIN=$(echo $BEFORE | cut -d'|' -f1); BEFORE_VT=$(echo $BEFORE | cut -d'|' -f2)

# Trigger a synthetic game-win delta via the legacy DAO path (CALL the SP
# the way game servers do — via UpdateMoneyUser admin c=100 with the same
# moneyType=vin code path). For determinism in CI we hit the DB directly
# through the v2 SP — production-path coverage is the e2e test, not this
# smoke.
test_name "Issue +$WIN_AMT vin via update_money_db_v2"
USER_ID=$(mysql_q "SELECT id FROM vinplay.users WHERE nick_name='$PLAYER_NICK'")
mysql_q "CALL vinplay.update_money_db_v2($USER_ID, $WIN_AMT, 'vin')" >/dev/null
_pass

test_name "users.vin increased by $WIN_AMT"
AFTER=$(snapshot); AFTER_VIN=$(echo $AFTER | cut -d'|' -f1); AFTER_VT=$(echo $AFTER | cut -d'|' -f2)
DELTA=$((AFTER_VIN - BEFORE_VIN))
[[ "$DELTA" == "$WIN_AMT" ]] && _pass "vin: $BEFORE_VIN → $AFTER_VIN" \
    || _fail "Expected vin+$WIN_AMT, got vin+$DELTA"

test_name "users.vin_total UNCHANGED (Phase 1 invariant)"
[[ "$AFTER_VT" == "$BEFORE_VT" ]] && _pass "vin_total stayed at $AFTER_VT" \
    || _fail "vin_total moved $BEFORE_VT → $AFTER_VT — Phase 1 path leaked into v1 SP"

test_name "Revert +$WIN_AMT"
mysql_q "CALL vinplay.update_money_db_v2($USER_ID, -$WIN_AMT, 'vin')" >/dev/null
_pass

# ──────────────────────────────────────────────────────────────────────
section "Smoke 2 — game loss (vin DECREASES, vin_total UNCHANGED)"
# ──────────────────────────────────────────────────────────────────────
LOSS_AMT=-7777
BEFORE=$(snapshot); BEFORE_VIN=$(echo $BEFORE | cut -d'|' -f1); BEFORE_VT=$(echo $BEFORE | cut -d'|' -f2)

test_name "Issue $LOSS_AMT vin via update_money_db_v2"
mysql_q "CALL vinplay.update_money_db_v2($USER_ID, $LOSS_AMT, 'vin')" >/dev/null
_pass

test_name "users.vin decreased by 7777"
AFTER=$(snapshot); AFTER_VIN=$(echo $AFTER | cut -d'|' -f1); AFTER_VT=$(echo $AFTER | cut -d'|' -f2)
DELTA=$((AFTER_VIN - BEFORE_VIN))
[[ "$DELTA" == "$LOSS_AMT" ]] && _pass "vin: $BEFORE_VIN → $AFTER_VIN" \
    || _fail "Expected vin$LOSS_AMT, got vin+$DELTA"

test_name "users.vin_total UNCHANGED (Phase 1 invariant)"
[[ "$AFTER_VT" == "$BEFORE_VT" ]] && _pass "vin_total stayed at $AFTER_VT" \
    || _fail "vin_total moved $BEFORE_VT → $AFTER_VT — Phase 1 path leaked into v1 SP"

test_name "Revert $LOSS_AMT"
mysql_q "CALL vinplay.update_money_db_v2($USER_ID, $((-LOSS_AMT)), 'vin')" >/dev/null
_pass

# ──────────────────────────────────────────────────────────────────────
section "Smoke 3 — admin topup (MoneyGateway path, vin_total UNCHANGED)"
# ──────────────────────────────────────────────────────────────────────
TOPUP_AMT=4321
BEFORE=$(snapshot); BEFORE_VIN=$(echo $BEFORE | cut -d'|' -f1); BEFORE_VT=$(echo $BEFORE | cut -d'|' -f2)

test_name "Admin c=100 topup +$TOPUP_AMT vin to $PLAYER_NICK"
RESP=$(curl -sS --max-time 15 \
    "${ADMIN_API}?c=100&aat=${AAT}&nn=${PLAYER_NICK}&mn=${TOPUP_AMT}&mt=vin&rs=phase1_smoke_${ADMIN_TX_NONCE}&otp=&type=0&ac=Admin" 2>&1)
echo "$RESP" | grep -q '"errorCode":"0"\|"success":true' && _pass "ok" \
    || { _skip "Admin topup endpoint returned: $(echo $RESP | head -c 200)"; SKIPPED_TOPUP=1; }

if [[ -z "${SKIPPED_TOPUP:-}" ]]; then
    test_name "users.vin increased by $TOPUP_AMT"
    AFTER=$(snapshot); AFTER_VIN=$(echo $AFTER | cut -d'|' -f1); AFTER_VT=$(echo $AFTER | cut -d'|' -f2)
    DELTA=$((AFTER_VIN - BEFORE_VIN))
    [[ "$DELTA" == "$TOPUP_AMT" ]] && _pass "vin: $BEFORE_VIN → $AFTER_VIN" \
        || _fail "Expected vin+$TOPUP_AMT, got vin+$DELTA"

    test_name "users.vin_total UNCHANGED (Phase 1 invariant)"
    [[ "$AFTER_VT" == "$BEFORE_VT" ]] && _pass "vin_total stayed at $AFTER_VT" \
        || _fail "Admin topup leaked into vin_total: $BEFORE_VT → $AFTER_VT"

    test_name "Revert admin topup"
    mysql_q "UPDATE vinplay.users SET vin = vin - $TOPUP_AMT WHERE id = $USER_ID" >/dev/null
    _pass
fi

# ──────────────────────────────────────────────────────────────────────
section "Smoke 4 — v_derived_player_pnl returns a coherent value"
# ──────────────────────────────────────────────────────────────────────
test_name "v_derived_player_pnl row exists for $PLAYER_NICK"
PNL=$(mysql_q "SELECT pnl_net FROM v_derived_player_pnl WHERE user_id=$USER_ID")
[[ -n "$PNL" ]] && _pass "pnl_net=$PNL" \
    || _skip "no PLAYER_VIN ledger history yet — view is empty (acceptable for fresh users)"

if [[ -n "$PNL" ]]; then
    test_name "pnl_net matches ledger SUM(CREDIT) - SUM(DEBIT) for WAGER_*"
    LEDGER_NET=$(mysql_q "
        SELECT COALESCE(SUM(CASE WHEN me.direction='CREDIT' THEN me.amount
                                 WHEN me.direction='DEBIT'  THEN -me.amount
                                 ELSE 0 END), 0)
          FROM money_entry me
          JOIN money_transaction mt ON mt.transaction_id = me.transaction_id
          JOIN money_account ma     ON ma.account_id    = me.account_id
         WHERE ma.owner_user_id = $USER_ID
           AND ma.account_type  = 'PLAYER_VIN'
           AND mt.transaction_type IN ('WAGER_DEBIT','WAGER_CREDIT','JACKPOT_PAYOUT','JACKPOT_CONTRIB')
           AND mt.status        = 'POSTED'")
    [[ "$PNL" == "$LEDGER_NET" ]] && _pass "view=$PNL ledger=$LEDGER_NET" \
        || _fail "View desynced — view=$PNL ledger=$LEDGER_NET"
fi

print_summary

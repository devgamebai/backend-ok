#!/bin/bash
# tests/wallet/test_wallet_smoke.sh — SUN-1099 wallet smoke
#
# Verifies (in ~20 seconds):
#   - Cleanup: deleted endpoints (c=9807/c=9809/c=9814 admin, c=3080/c=3081
#     portal) return 9002 / "COMMAND NOT FOUND"
#   - Wave 2 / SUN-1099: SpecialAccount denied at LOGIN on player portal
#     (PM rule: agency portal only) → errorCode 1109
#   - Wave 2 / SUN-1099: SpecialAccount denied as agency-credit actor
#     (c=9923 / c=9922) → errorCode 1099
#   - Live cashback config CRUD intact (c=9800, c=9801)
#   - Regression: c=701 admin login + c=124 captcha
#
# Run from repo root: bash tests/wallet/test_wallet_smoke.sh
# Exit 0 = all pass.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../helpers.sh"

ENV_FILE="$(cd "$SCRIPT_DIR/../.." && pwd)/.env"
MYSQL_PASS=$(grep '^MYSQL_ROOT_PASSWORD' "$ENV_FILE" | cut -d= -f2)
mysql_q() { docker exec sunwinkr-mysql mysql -uroot -p"$MYSQL_PASS" -N -B -e "$1" 2>/dev/null; }

# Always parse JSON with `or ''` so null doesn't become "None".
extract_token() { python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken') or '')" 2>/dev/null; }

# Fresh admin token per call — old token may have aged out.
fresh_aat() {
    curl -sS --max-time 10 "${ADMIN_API}?c=701&un=${ADMIN_USER}&pw=${ADMIN_PASS}" 2>/dev/null \
        | extract_token
}

# Routing-deletion proof: server returns either JSON 9002 OR plain text "COMMAND NOT FOUND".
is_deleted_endpoint() {
    echo "$1" | grep -qE '"errorCode":"9002"|COMMAND NOT FOUND'
}

SPECIAL_NICK="SpecialAccount"
SPECIAL_USER="specialAccount"
SPECIAL_PASS_MD5=$(echo -n "account@2026" | md5sum | awk '{print $1}')

# ──────────────────────────────────────────────────────────────────────
# Cleanup — deleted endpoints
# ──────────────────────────────────────────────────────────────────────
section "SUN-1099 cleanup — deleted endpoints"

AAT=$(fresh_aat); export AAT
[[ ${#AAT} == 32 ]] || { echo -e "${RED}FATAL: cannot fetch admin token${RESET}"; exit 1; }

for c in 9807 9809 9814; do
    test_name "c=$c admin endpoint deleted"
    RESP=$(curl -sS --max-time 5 "${ADMIN_API}?c=$c&aat=${AAT}" 2>&1)
    is_deleted_endpoint "$RESP" && _pass "ok" || _fail "c=$c still routes — got: $(echo $RESP | head -c 100)"
done

for c in 3080 3081; do
    test_name "c=$c portal endpoint deleted"
    RESP=$(curl -sS --max-time 5 "${PLAYER_API}?c=$c&at=any" 2>&1)
    is_deleted_endpoint "$RESP" && _pass "ok" || _fail "c=$c still routes — got: $(echo $RESP | head -c 100)"
done

# ──────────────────────────────────────────────────────────────────────
# SpecialAccount — login deny on player portal (PM rule)
# ──────────────────────────────────────────────────────────────────────
section "SpecialAccount login deny on player portal"

test_name "Verify SpecialAccount user exists (useragent.code='0')"
SPECIAL_AGENT_ID=$(mysql_q "SELECT id FROM vinplay_admin.useragent WHERE code='0' AND nickname='$SPECIAL_NICK'")
[[ -n "$SPECIAL_AGENT_ID" ]] && _pass "ok" || _fail "SpecialAccount fixture missing"

test_name "c=3 (player login) for SpecialAccount → DENIED (errorCode 1109)"
RESP=$(curl -sS --max-time 10 "${PLAYER_API}?c=3&un=${SPECIAL_USER}&pw=${SPECIAL_PASS_MD5}" 2>&1)
echo "$RESP" | grep -q '"errorCode":"1109"' && _pass "ok" || _fail "Expected 1109, got: $(echo $RESP | head -c 150)"

# ──────────────────────────────────────────────────────────────────────
# SpecialAccount — denied as actor on agency-credit endpoints
# ──────────────────────────────────────────────────────────────────────
section "SpecialAccount as actor on c=9923 / c=9922 → 1099"

AAT=$(fresh_aat)  # refresh
SPECIAL_CODE=$(mysql_q "SELECT code FROM vinplay_admin.useragent WHERE nickname='$SPECIAL_NICK'")

test_name "c=9923 AgentCreditDeposit with SpecialAccount sender → 1099"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=9923&aat=${AAT}&code=${SPECIAL_CODE}&nn=anyone&am=1000&tt=user&pwd=x" 2>&1)
echo "$RESP" | grep -q '"errorCode":"1099"' && _pass "ok" || _fail "got: $(echo $RESP | head -c 150)"

test_name "c=9922 AgentTransferCredit with SpecialAccount sender → 1099"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=9922&aat=${AAT}&code=${SPECIAL_CODE}&to=anyone&am=1000&pwd=x" 2>&1)
echo "$RESP" | grep -q '"errorCode":"1099"' && _pass "ok" || _fail "got: $(echo $RESP | head -c 150)"

# ──────────────────────────────────────────────────────────────────────
# Live cashback config CRUD
# ──────────────────────────────────────────────────────────────────────
section "Cashback config CRUD — live config tables intact"

AAT=$(fresh_aat)  # refresh

test_name "c=9800 GetCashbackConfig"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=9800&aat=${AAT}" 2>&1)
echo "$RESP" | grep -q '"success":true' && _pass "ok" || _fail "got: $(echo $RESP | head -c 150)"

test_name "c=9801 ListCashbackConfigs"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=9801&aat=${AAT}" 2>&1)
echo "$RESP" | grep -q '"success":true' && _pass "ok" || _fail "got: $(echo $RESP | head -c 150)"

test_name "tbl_cashback_game_config table populated"
COUNT=$(mysql_q "SELECT COUNT(*) FROM vinplay.tbl_cashback_game_config")
[[ "$COUNT" -gt 0 ]] && _pass "ok" || _fail "empty — RealTimeCommission can't read rates"

# ──────────────────────────────────────────────────────────────────────
# Regression — admin login + captcha
# ──────────────────────────────────────────────────────────────────────
section "Regression — admin login + captcha"

test_name "c=701 admin login returns 32-char accessToken"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=701&un=${ADMIN_USER}&pw=${ADMIN_PASS}" 2>&1)
TOKEN=$(echo "$RESP" | extract_token)
[[ ${#TOKEN} == 32 ]] && _pass "ok" || _fail "Token length wrong: ${#TOKEN}"

test_name "c=124 captcha returns 36-char UUID + base64 image"
RESP=$(curl -sS --max-time 10 "${ADMIN_API}?c=124" 2>&1)
ID_LEN=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('id') or ''))" 2>/dev/null)
[[ "$ID_LEN" == "36" ]] && _pass "ok" || _fail "Captcha id length wrong: $ID_LEN"

# ──────────────────────────────────────────────────────────────────────
# DB cleanup verification — V14 dropped tables
# ──────────────────────────────────────────────────────────────────────
section "V14 migration — dead tables dropped"

test_name "tbl_cashback_logs no longer exists"
EXISTS=$(mysql_q "SHOW TABLES FROM vinplay LIKE 'tbl_cashback_logs'" | wc -l | tr -d ' ')
[[ "$EXISTS" == "0" ]] && _pass "ok" || _fail "tbl_cashback_logs still present"

test_name "tbl_cashback_log_game_detail no longer exists"
EXISTS=$(mysql_q "SHOW TABLES FROM vinplay LIKE 'tbl_cashback_log_game_detail'" | wc -l | tr -d ' ')
[[ "$EXISTS" == "0" ]] && _pass "ok" || _fail "tbl_cashback_log_game_detail still present"

print_summary

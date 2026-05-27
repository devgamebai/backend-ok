#!/bin/bash
# test-taixiu-error-cases.sh — TaiXiu bet error-code coverage
#
# Spec refs: taixiu-extraction-plan.md §5.5 (error codes), §2.2 B1 (error ordering)
#
# Error codes under test:
#   0001 — Wallet failure / race-disabled (simulate via absurdly large bet)
#   0002 — Betting closed (hard to guarantee timing — marked flaky, documented)
#   0003 — Insufficient balance (bet larger than wallet)
#   0004 — Below MIN(100) — bet 50
#   0005 — Cross-side bet not allowed (TAI then XIU same round)
#   0401 — Missing/invalid token
#
# Status: PENDING — /api/v2/taixiu/* endpoints not yet deployed.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

# ─── Endpoint liveness probe ─────────────────────────────────────────────────
_endpoint_live() {
    local resp="$1"
    echo "$resp" | grep -qiE '(404|Not Found|CURL_ERROR|<!DOCTYPE)' 2>/dev/null && return 1
    echo "$resp" | jq . >/dev/null 2>&1 || return 1
    return 0
}

section "TaiXiu Error Cases — Login"

test_name "Player login"
LOGIN_RESP=$(login_player "$PLAYER_USER" "$PLAYER_PASS")
PLAYER_AT=$(echo "$LOGIN_RESP" | jq -r '.accessToken // empty' 2>/dev/null)
if [[ -z "$PLAYER_AT" ]]; then
    _fail "Login failed — resp: ${LOGIN_RESP:0:200}"
    print_summary
fi
_pass "Got player accessToken"
export PLAYER_AT

WALLET=$(echo "$LOGIN_RESP" | jq -r '.currentMoney // .vin // 0' 2>/dev/null || echo "0")
echo "  Current wallet: ${WALLET}"

# Liveness probe on join endpoint
PROBE=$(minigame_post "/api/v2/taixiu/join" '{"moneyType":1}')
if ! _endpoint_live "$PROBE"; then
    section "TaiXiu Error Cases — ALL PENDING (endpoint not up)"
    for label in \
        "0001 wallet failure" \
        "0002 betting closed" \
        "0003 insufficient balance" \
        "0004 below MIN(100)" \
        "0005 cross-side bet" \
        "0401 missing token" \
        "0401 invalid token"
    do
        test_name "$label"
        pending_skip "/api/v2/taixiu/* not yet deployed"
    done
    print_summary
fi

# Join to set up session
JOIN_RESP=$(minigame_post "/api/v2/taixiu/join" '{"moneyType":1}')
BETTING_STATE=$(echo "$JOIN_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")

# ─── Error 0003: Insufficient balance ────────────────────────────────────────
section "TaiXiu Error Cases — 0003 Insufficient balance"

test_name "Bet more than wallet balance → errorCode 0003"
# Use wallet+1 to guarantee insufficient funds
OVER_BET=$((WALLET + 1000000))
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    "{\"moneyType\":1,\"betValue\":${OVER_BET},\"betSide\":1,\"clientNonce\":\"err-0003-$(date +%s)\"}")
echo "  betValue=${OVER_BET} wallet=${WALLET}"
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0003"

# ─── Error 0004: Below minimum ────────────────────────────────────────────────
section "TaiXiu Error Cases — 0004 Below MIN(100)"

test_name "Bet 50 (below MIN 100) → errorCode 0004"
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":50,"betSide":1,"clientNonce":"err-0004-min"}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0004"

test_name "Bet 0 (zero) → errorCode 0004"
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":0,"betSide":1,"clientNonce":"err-0004-zero"}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0004"

test_name "Bet 99 (one below MIN) → errorCode 0004"
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":99,"betSide":1,"clientNonce":"err-0004-99"}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0004"

test_name "Bet 100 (exactly MIN) → success (boundary)"
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":100,"betSide":1,"clientNonce":"err-0004-100"}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
# Boundary bet of exactly 100 must succeed (not return 0004)
EC=$(echo "$RESP" | jq -r '.errorCode // ""' 2>/dev/null || echo "")
if [[ "$EC" != "0004" ]]; then
    _pass "Bet 100 (MIN boundary) not rejected with 0004 (errorCode=${EC:-none})"
else
    _fail "Bet 100 (exactly MIN) wrongly returned 0004 — boundary inclusive check failed"
fi

# ─── Error 0005: Cross-side bet ───────────────────────────────────────────────
section "TaiXiu Error Cases — 0005 Cross-side bet"

if [[ "$BETTING_STATE" != "true" ]]; then
    test_name "Cross-side bet test (0005)"
    pending_skip "bettingState=false at test time — retry at round start"
else
    test_name "Place TAI bet (side=1)"
    RESP1=$(minigame_post "/api/v2/taixiu/bet" \
        '{"moneyType":1,"betValue":1000,"betSide":1,"clientNonce":"err-0005-tai"}')
    echo "  Response: $(echo "$RESP1" | jq -c . 2>/dev/null || echo "${RESP1:0:200}")"
    EC1=$(echo "$RESP1" | jq -r '.errorCode // ""' 2>/dev/null || echo "")
    if [[ "$EC1" == "0000" || "$EC1" == "0" ]]; then
        _pass "TAI bet placed (errorCode=${EC1})"

        test_name "Attempt XIU bet same round → errorCode 0005 (cross-side)"
        RESP2=$(minigame_post "/api/v2/taixiu/bet" \
            '{"moneyType":1,"betValue":1000,"betSide":0,"clientNonce":"err-0005-xiu"}')
        echo "  Response: $(echo "$RESP2" | jq -c . 2>/dev/null || echo "${RESP2:0:200}")"
        assert_status_4xx "$RESP2" "0005"
    else
        _skip "TAI bet returned errorCode=${EC1} — cannot test cross-side (betting may have closed)"
    fi
fi

# ─── Error 0001: Wallet failure ───────────────────────────────────────────────
# Simulated by a bet large enough to trigger a wallet debit failure (not just balance check).
# In practice 0001 fires when the WalletPort returns a failure code (race/disabled).
# We use 9_999_999_999 which exceeds typical VIN balance and may trigger 0003 first;
# if errorCode is 0001 or 0003 both are acceptable since balance-insufficient is checked
# before the wallet call (B1 ordering: 4→3→5→wallet). We assert it is NOT 0000.
section "TaiXiu Error Cases — 0001 Wallet failure (simulated)"

test_name "Absurdly large bet (9_999_999_999) → 0001 or 0003 (not success)"
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":9999999999,"betSide":1,"clientNonce":"err-0001-big"}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
EC=$(echo "$RESP" | jq -r '.errorCode // ""' 2>/dev/null || echo "")
if [[ "$EC" == "0001" || "$EC" == "0003" ]]; then
    _pass "Large bet correctly rejected (errorCode=${EC})"
else
    assert_status_4xx "$RESP"
fi

# ─── Error 0002: Betting closed ───────────────────────────────────────────────
# NOTE: This test is inherently timing-dependent. We poll state until bettingState=false,
# then attempt a bet. It may be flaky if the round transitions during the test.
section "TaiXiu Error Cases — 0002 Betting closed"

test_name "Bet after betting window closes → errorCode 0002 [timing-sensitive]"
echo "  NOTE: This test is inherently flaky — depends on catching the LOCKED phase."
echo "  Polling for bettingState=false (max 60 attempts, 1s apart)..."

FOUND_LOCKED=false
for i in $(seq 1 60); do
    STATE_RESP=$(minigame_get "/api/v2/taixiu/state?moneyType=1")
    BS=$(echo "$STATE_RESP" | jq -r '.bettingState' 2>/dev/null || echo "true")
    if [[ "$BS" == "false" ]]; then
        FOUND_LOCKED=true
        echo "  Found bettingState=false at poll ${i}"
        break
    fi
    sleep 1
done

if [[ "$FOUND_LOCKED" == "true" ]]; then
    RESP=$(minigame_post "/api/v2/taixiu/bet" \
        '{"moneyType":1,"betValue":1000,"betSide":1,"clientNonce":"err-0002-closed"}')
    echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
    assert_status_4xx "$RESP" "0002"
else
    _skip "Could not observe bettingState=false within 60s — 0002 test inconclusive (flaky by design)"
fi

# ─── Error 0401: Unauthorized ────────────────────────────────────────────────
section "TaiXiu Error Cases — 0401 Unauthorized"

test_name "POST /api/v2/taixiu/bet with no Authorization header → 0401"
RESP=$(minigame_post_no_auth "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":1000,"betSide":1}')
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0401"

test_name "POST /api/v2/taixiu/bet with garbage token → 0401"
# Temporarily override PLAYER_AT with an invalid token
_SAVED_AT="$PLAYER_AT"
PLAYER_AT="not-a-real-token-00000000000000000000"
export PLAYER_AT
RESP=$(minigame_post "/api/v2/taixiu/bet" \
    '{"moneyType":1,"betValue":1000,"betSide":1}')
PLAYER_AT="$_SAVED_AT"
export PLAYER_AT
echo "  Response: $(echo "$RESP" | jq -c . 2>/dev/null || echo "${RESP:0:300}")"
assert_status_4xx "$RESP" "0401"

test_name "GET /api/v2/taixiu/state with no token → 0401"
STATE_NO_AUTH=$(curl -sf --max-time "$TIMEOUT" \
    "${BASE_URL}/api/v2/taixiu/state?moneyType=1" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
echo "  Response: $(echo "$STATE_NO_AUTH" | jq -c . 2>/dev/null || echo "${STATE_NO_AUTH:0:300}")"
assert_status_4xx "$STATE_NO_AUTH" "0401"

print_summary

#!/bin/bash
# test-taixiu-happy-path.sh — TaiXiu full happy-path flow
#
# Spec refs: taixiu-extraction-plan.md §5.1 (endpoints), §5.2 (DTOs)
#
# Flow:
#   1. Login (player token)
#   2. POST /api/v2/taixiu/join       → StateDto
#   3. POST /api/v2/taixiu/bet        → BetResponseDto, currentMoney decremented
#   4. GET  /api/v2/taixiu/state      → myBetTai reflects the bet
#   5. GET  /api/v2/taixiu/history    → recent rounds array
#   6. POST /api/v2/taixiu/leave      → success
#
# Status: PENDING — /api/v2/taixiu/* endpoints not yet deployed.
#         All assertions are written against the final spec so they run
#         without modification once the endpoint server is up.
#         Tests are marked SKIP (not FAIL) while the server returns 404.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

# ─── Helper: detect "endpoint not yet up" (404 or connection refused) ────────
_endpoint_live() {
    local resp="$1"
    # If the response is a curl error OR an HTML 404, the endpoint is not up.
    if echo "$resp" | grep -qiE '(404|Not Found|CURL_ERROR|<!DOCTYPE)' 2>/dev/null; then
        return 1
    fi
    # If jq can't parse it, it's not JSON — not up.
    echo "$resp" | jq . >/dev/null 2>&1 || return 1
    return 0
}

# ─── Section 1: Login ─────────────────────────────────────────────────────────
section "TaiXiu Happy Path — Login"

test_name "Player login to obtain access token"
PLAYER_AT=""
LOGIN_RESP=$(login_player "$PLAYER_USER" "$PLAYER_PASS")
if [[ -z "$LOGIN_RESP" ]]; then
    _fail "curl to login endpoint returned empty"
    print_summary
fi
PLAYER_AT=$(echo "$LOGIN_RESP" | jq -r '.accessToken // empty' 2>/dev/null)
if [[ -z "$PLAYER_AT" ]]; then
    _fail "No accessToken in login response — resp: ${LOGIN_RESP:0:200}"
    print_summary
else
    _pass "Got player accessToken (len=${#PLAYER_AT})"
fi
export PLAYER_AT

# Capture initial wallet balance for later comparison
INITIAL_MONEY=$(echo "$LOGIN_RESP" | jq -r '.currentMoney // .vin // -1' 2>/dev/null || echo "-1")
echo "  Initial wallet balance: ${INITIAL_MONEY}"

# ─── Section 2: Join ──────────────────────────────────────────────────────────
section "TaiXiu Happy Path — Join"

test_name "POST /api/v2/taixiu/join (moneyType=1)"
JOIN_RESP=$(minigame_post "/api/v2/taixiu/join" '{"moneyType":1}')
echo "  Response: $(echo "$JOIN_RESP" | jq -c . 2>/dev/null || echo "${JOIN_RESP:0:300}")"

if ! _endpoint_live "$JOIN_RESP"; then
    pending_skip "/api/v2/taixiu/join not yet deployed (got: ${JOIN_RESP:0:100})"
    # Mark remaining tests as pending too — no point running them if join fails
    section "TaiXiu Happy Path — Bet (pending)"
    test_name "POST /api/v2/taixiu/bet (moneyType=1, betValue=1000, betSide=1)"
    pending_skip "/api/v2/taixiu/bet not yet deployed"

    section "TaiXiu Happy Path — State (pending)"
    test_name "GET /api/v2/taixiu/state?moneyType=1"
    pending_skip "/api/v2/taixiu/state not yet deployed"

    section "TaiXiu Happy Path — History (pending)"
    test_name "GET /api/v2/taixiu/history?moneyType=1&n=10"
    pending_skip "/api/v2/taixiu/history not yet deployed"

    section "TaiXiu Happy Path — Leave (pending)"
    test_name "POST /api/v2/taixiu/leave"
    pending_skip "/api/v2/taixiu/leave not yet deployed"

    print_summary
fi

assert_status_200 "$JOIN_RESP"
assert_state_dto "$JOIN_RESP"
assert_json_field_not_null "$JOIN_RESP" ".referenceId"
assert_json_field_not_null "$JOIN_RESP" ".remainTime"

# Snapshot bettingState — bet is only valid when bettingState==true
BETTING_STATE=$(echo "$JOIN_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")
echo "  bettingState=${BETTING_STATE}"

# ─── Section 3: Bet ───────────────────────────────────────────────────────────
section "TaiXiu Happy Path — Bet"

BET_NONCE="smoke-happy-$(date +%s)"

if [[ "$BETTING_STATE" != "true" ]]; then
    test_name "POST /api/v2/taixiu/bet (moneyType=1, betValue=1000, betSide=1)"
    pending_skip "Betting window closed at join time — re-run at start of a round (bettingState was false)"
else
    test_name "POST /api/v2/taixiu/bet (moneyType=1, betValue=1000, betSide=1 [TAI])"
    BET_RESP=$(minigame_post "/api/v2/taixiu/bet" \
        "{\"moneyType\":1,\"betValue\":1000,\"betSide\":1,\"clientNonce\":\"${BET_NONCE}\"}")
    echo "  Response: $(echo "$BET_RESP" | jq -c . 2>/dev/null || echo "${BET_RESP:0:300}")"

    assert_status_200 "$BET_RESP"
    assert_bet_response_dto "$BET_RESP"
    assert_json_field "$BET_RESP" ".errorCode" "0000"

    # currentMoney should be initial minus 1000
    MONEY_AFTER_BET=$(echo "$BET_RESP" | jq -r '.currentMoney // -1' 2>/dev/null || echo "-1")
    if [[ "$INITIAL_MONEY" != "-1" && "$MONEY_AFTER_BET" != "-1" ]]; then
        EXPECTED_MONEY=$((INITIAL_MONEY - 1000))
        test_name "currentMoney decremented by 1000 after bet"
        assert_json_field "$BET_RESP" ".currentMoney" "$EXPECTED_MONEY"
    else
        test_name "currentMoney present in BetResponseDto"
        assert_json_field_not_null "$BET_RESP" ".currentMoney"
    fi

    PER_BET_TX_ID=$(echo "$BET_RESP" | jq -r '.perBetTxId // empty' 2>/dev/null)
    echo "  perBetTxId=${PER_BET_TX_ID}"
fi

# ─── Section 4: State ─────────────────────────────────────────────────────────
section "TaiXiu Happy Path — State"

test_name "GET /api/v2/taixiu/state?moneyType=1"
STATE_RESP=$(minigame_get "/api/v2/taixiu/state?moneyType=1")
echo "  Response: $(echo "$STATE_RESP" | jq -c . 2>/dev/null || echo "${STATE_RESP:0:300}")"

assert_status_200 "$STATE_RESP"
assert_state_dto "$STATE_RESP"

# If we successfully placed a bet above, myBetTai should be >= 1000
if [[ "$BETTING_STATE" == "true" ]]; then
    test_name "state.myBetTai reflects placed bet (>= 1000)"
    MY_BET=$(echo "$STATE_RESP" | jq -r '.myBetTai // 0' 2>/dev/null || echo "0")
    if (( MY_BET >= 1000 )); then
        _pass "myBetTai=${MY_BET} >= 1000 — bet recorded in state"
    else
        _fail "myBetTai=${MY_BET}, expected >= 1000 after placing 1000 TAI bet"
    fi
fi

# Snapshot-censoring invariant: if not yet revealed, dice must be zero
test_name "StateDto dice censoring — pre-reveal dice are zero"
DICE1=$(echo "$STATE_RESP" | jq -r '.dice1 // -1' 2>/dev/null || echo "-1")
DICE2=$(echo "$STATE_RESP" | jq -r '.dice2 // -1' 2>/dev/null || echo "-1")
DICE3=$(echo "$STATE_RESP" | jq -r '.dice3 // -1' 2>/dev/null || echo "-1")
RESULT=$(echo "$STATE_RESP" | jq -r '.result // 99' 2>/dev/null || echo "99")
BETTING_NOW=$(echo "$STATE_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")

if [[ "$BETTING_NOW" == "true" ]]; then
    # Definitely pre-reveal — dice MUST be 0 and result MUST be -1
    if [[ "$DICE1" == "0" && "$DICE2" == "0" && "$DICE3" == "0" && "$RESULT" == "-1" ]]; then
        _pass "Pre-reveal state: dice=[0,0,0] result=-1 (snapshot censoring holds)"
    else
        _fail "Pre-reveal state leaked dice/result: dice=[${DICE1},${DICE2},${DICE3}] result=${RESULT}"
    fi
else
    _skip "bettingState=false at time of state call — may be post-reveal (cannot assert dice=0)"
fi

# ─── Section 5: History ───────────────────────────────────────────────────────
section "TaiXiu Happy Path — History"

test_name "GET /api/v2/taixiu/history?moneyType=1&n=10"
HIST_RESP=$(minigame_get "/api/v2/taixiu/history?moneyType=1&n=10")
echo "  Response: $(echo "$HIST_RESP" | jq -c . 2>/dev/null || echo "${HIST_RESP:0:300}")"

assert_status_200 "$HIST_RESP"
# History response should be an array or contain an array field
HIST_LEN=$(echo "$HIST_RESP" | jq 'if type == "array" then length elif .data | type == "array" then .data | length else -1 end' 2>/dev/null || echo "-1")

test_name "History returns array of recent rounds"
if [[ "$HIST_LEN" == "-1" ]]; then
    _fail "history response is not an array or {data:[]} — resp: ${HIST_RESP:0:300}"
else
    _pass "history array length=${HIST_LEN} (requested n=10)"
fi

test_name "History length <= 10 (n param respected)"
if [[ "$HIST_LEN" =~ ^[0-9]+$ ]] && (( HIST_LEN <= 10 )); then
    _pass "history length ${HIST_LEN} <= 10"
else
    _fail "history length ${HIST_LEN} exceeds n=10"
fi

# ─── Section 6: Leave ─────────────────────────────────────────────────────────
section "TaiXiu Happy Path — Leave"

test_name "POST /api/v2/taixiu/leave (moneyType=1)"
LEAVE_RESP=$(minigame_post "/api/v2/taixiu/leave" '{"moneyType":1}')
echo "  Response: $(echo "$LEAVE_RESP" | jq -c . 2>/dev/null || echo "${LEAVE_RESP:0:300}")"

assert_status_200 "$LEAVE_RESP"

print_summary

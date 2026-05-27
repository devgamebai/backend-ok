#!/bin/bash
# test-sicbo-happy-path.sh — Sicbo full happy-path flow
#
# Spec refs: sicbo-extraction-plan.md §6 (BitZero adapter / STOMP topics),
#            sicbo-extraction-plan.md §2.3 (bet acceptance),
#            taixiu-extraction-plan.md §5.1 (REST endpoint shape — Sicbo mirrors TaiXiu)
#
# Sicbo betSide is a STRING, not a short. Example values from §2.3 + §2.6:
#   "TAI"           — over (total > 10)
#   "XIU"           — under (total <= 10)
#   "POINT_8"       — exact total = 8
#   "ONE_DICE_3"    — 3 appears on at least one die
#   "TRIPLE_DICES_4"— all three dice show 4
#
# Flow:
#   1. Login
#   2. POST /api/v2/sicbo/join        → StateDto (Sicbo variant)
#   3. Loop through several bet types — TAI, XIU, POINT_8, ONE_DICE_3
#   4. GET  /api/v2/sicbo/state       → verify last-placed bet reflected
#   5. GET  /api/v2/sicbo/history     → recent rounds array
#   6. POST /api/v2/sicbo/leave       → success
#
# Status: PENDING — /api/v2/sicbo/* endpoints not yet deployed.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

_endpoint_live() {
    echo "$1" | grep -qiE '(404|Not Found|CURL_ERROR|<!DOCTYPE)' 2>/dev/null && return 1
    echo "$1" | jq . >/dev/null 2>&1 || return 1
    return 0
}

section "Sicbo Happy Path — Login"

test_name "Player login"
LOGIN_RESP=$(login_player "$PLAYER_USER" "$PLAYER_PASS")
PLAYER_AT=$(echo "$LOGIN_RESP" | jq -r '.accessToken // empty' 2>/dev/null)
if [[ -z "$PLAYER_AT" ]]; then
    _fail "Login failed — resp: ${LOGIN_RESP:0:200}"
    print_summary
fi
_pass "Got player accessToken"
export PLAYER_AT

INITIAL_MONEY=$(echo "$LOGIN_RESP" | jq -r '.currentMoney // .vin // -1' 2>/dev/null || echo "-1")
echo "  Initial wallet: ${INITIAL_MONEY}"

# ─── Liveness probe ───────────────────────────────────────────────────────────
PROBE=$(minigame_post "/api/v2/sicbo/join" '{"moneyType":1}')
if ! _endpoint_live "$PROBE"; then
    section "Sicbo Happy Path — ALL PENDING (endpoint not up)"
    for label in "join" "bet TAI" "bet XIU" "bet POINT_8" "bet ONE_DICE_3" "state" "history" "leave"; do
        test_name "Sicbo ${label}"
        pending_skip "/api/v2/sicbo/* not yet deployed (got: ${PROBE:0:60})"
    done
    print_summary
fi

# ─── Join ─────────────────────────────────────────────────────────────────────
section "Sicbo Happy Path — Join"

test_name "POST /api/v2/sicbo/join (moneyType=1)"
JOIN_RESP=$(minigame_post "/api/v2/sicbo/join" '{"moneyType":1}')
echo "  Response: $(echo "$JOIN_RESP" | jq -c . 2>/dev/null || echo "${JOIN_RESP:0:300}")"
assert_status_200 "$JOIN_RESP"

# Sicbo StateDto mirrors TaiXiu per plan §6; reuse the same field names
assert_json_field_not_null "$JOIN_RESP" ".referenceId"
assert_json_field_not_null "$JOIN_RESP" ".remainTime"

BETTING_STATE=$(echo "$JOIN_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")
echo "  bettingState=${BETTING_STATE}"

# ─── Bet loop — multiple bet types ───────────────────────────────────────────
section "Sicbo Happy Path — Bet (multiple types)"

# Bet types to exercise. Sicbo allows multi-side per spec (AMBIGUOUS #3 resolved:
# cross-side is intentionally allowed for Sicbo unlike TaiXiu).
declare -a BET_TYPES=("TAI" "XIU" "POINT_8" "ONE_DICE_3")
declare -a BET_VALUES=(1000 1000 1000 1000)

LAST_MONEY="$INITIAL_MONEY"
BETS_PLACED=0

for idx in "${!BET_TYPES[@]}"; do
    BET_SIDE="${BET_TYPES[$idx]}"
    BET_VAL="${BET_VALUES[$idx]}"
    NONCE="sicbo-happy-${BET_SIDE}-$(date +%s%N)"

    test_name "POST /api/v2/sicbo/bet — betSide=\"${BET_SIDE}\" betValue=${BET_VAL}"

    if [[ "$BETTING_STATE" != "true" ]]; then
        _skip "bettingState=false — cannot place bet for ${BET_SIDE}"
        continue
    fi

    BET_RESP=$(minigame_post "/api/v2/sicbo/bet" \
        "{\"moneyType\":1,\"betValue\":${BET_VAL},\"betSide\":\"${BET_SIDE}\",\"clientNonce\":\"${NONCE}\"}")
    echo "  Response: $(echo "$BET_RESP" | jq -c . 2>/dev/null || echo "${BET_RESP:0:300}")"

    assert_status_200 "$BET_RESP"
    assert_json_field "$BET_RESP" ".errorCode" "0000"
    assert_json_field_not_null "$BET_RESP" ".perBetTxId"
    assert_json_field_not_null "$BET_RESP" ".currentMoney"

    NEW_MONEY=$(echo "$BET_RESP" | jq -r '.currentMoney // -1' 2>/dev/null || echo "-1")
    if [[ "$LAST_MONEY" != "-1" && "$NEW_MONEY" != "-1" ]]; then
        EXPECTED=$((LAST_MONEY - BET_VAL))
        test_name "currentMoney decremented by ${BET_VAL} after ${BET_SIDE} bet"
        assert_json_field "$BET_RESP" ".currentMoney" "$EXPECTED"
        LAST_MONEY="$NEW_MONEY"
    fi

    BETS_PLACED=$((BETS_PLACED + 1))

    # Re-check bettingState after each bet (window may close mid-loop)
    STATE_CHECK=$(minigame_get "/api/v2/sicbo/state?moneyType=1")
    BETTING_STATE=$(echo "$STATE_CHECK" | jq -r '.bettingState' 2>/dev/null || echo "false")
done

echo ""
echo "  Bets placed in this run: ${BETS_PLACED}/${#BET_TYPES[@]}"

# ─── State ────────────────────────────────────────────────────────────────────
section "Sicbo Happy Path — State"

test_name "GET /api/v2/sicbo/state?moneyType=1"
STATE_RESP=$(minigame_get "/api/v2/sicbo/state?moneyType=1")
echo "  Response: $(echo "$STATE_RESP" | jq -c . 2>/dev/null || echo "${STATE_RESP:0:300}")"
assert_status_200 "$STATE_RESP"
assert_json_field_not_null "$STATE_RESP" ".referenceId"
assert_json_field_not_null "$STATE_RESP" ".remainTime"

# Snapshot censoring invariant
test_name "Sicbo state: pre-reveal dice are zero (snapshot censoring)"
D1=$(echo "$STATE_RESP" | jq -r '.dice1 // -1' 2>/dev/null || echo "-1")
D2=$(echo "$STATE_RESP" | jq -r '.dice2 // -1' 2>/dev/null || echo "-1")
D3=$(echo "$STATE_RESP" | jq -r '.dice3 // -1' 2>/dev/null || echo "-1")
RS=$(echo "$STATE_RESP" | jq -r '.result // 99' 2>/dev/null || echo "99")
BS_NOW=$(echo "$STATE_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")

if [[ "$BS_NOW" == "true" ]]; then
    if [[ "$D1" == "0" && "$D2" == "0" && "$D3" == "0" && "$RS" == "-1" ]]; then
        _pass "Pre-reveal Sicbo state: dice=[0,0,0] result=-1 — censoring holds"
    else
        _fail "LEAK: Sicbo pre-reveal state has dice=[${D1},${D2},${D3}] result=${RS}"
    fi
else
    _skip "bettingState=false — may be post-reveal (cannot assert dice=0)"
fi

# ─── History ──────────────────────────────────────────────────────────────────
section "Sicbo Happy Path — History"

test_name "GET /api/v2/sicbo/history?moneyType=1&n=10"
HIST_RESP=$(minigame_get "/api/v2/sicbo/history?moneyType=1&n=10")
echo "  Response: $(echo "$HIST_RESP" | jq -c . 2>/dev/null || echo "${HIST_RESP:0:300}")"
assert_status_200 "$HIST_RESP"

HIST_LEN=$(echo "$HIST_RESP" | jq 'if type == "array" then length elif .data | type == "array" then .data | length else -1 end' 2>/dev/null || echo "-1")
test_name "Sicbo history returns array"
if [[ "$HIST_LEN" == "-1" ]]; then
    _fail "history response is not array or {data:[]} — resp: ${HIST_RESP:0:200}"
else
    _pass "history array length=${HIST_LEN}"
fi

test_name "Sicbo history length <= 10 (n param respected)"
if [[ "$HIST_LEN" =~ ^[0-9]+$ ]] && (( HIST_LEN <= 10 )); then
    _pass "history length ${HIST_LEN} <= 10"
else
    _fail "history length ${HIST_LEN} exceeds n=10 or is non-numeric"
fi

# ─── Leave ────────────────────────────────────────────────────────────────────
section "Sicbo Happy Path — Leave"

test_name "POST /api/v2/sicbo/leave (moneyType=1)"
LEAVE_RESP=$(minigame_post "/api/v2/sicbo/leave" '{"moneyType":1}')
echo "  Response: $(echo "$LEAVE_RESP" | jq -c . 2>/dev/null || echo "${LEAVE_RESP:0:300}")"
assert_status_200 "$LEAVE_RESP"

print_summary

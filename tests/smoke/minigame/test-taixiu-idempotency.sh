#!/bin/bash
# test-taixiu-idempotency.sh — clientNonce idempotency guarantee
#
# Spec refs: taixiu-extraction-plan.md §5.6
#   "clientNonce → Hazelcast taixiu:bet:nonce:<user> 5min TTL.
#    Repeat → return cached BetResponseDto."
#
# Test:
#   1. Place a bet with a unique clientNonce
#   2. Repeat the identical request with the same clientNonce
#   3. Assert second response has SAME perBetTxId as first
#   4. Assert second response has SAME currentMoney as first (wallet not debited twice)
#
# Status: PENDING — /api/v2/taixiu/* endpoints not yet deployed.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

_endpoint_live() {
    echo "$1" | grep -qiE '(404|Not Found|CURL_ERROR|<!DOCTYPE)' 2>/dev/null && return 1
    echo "$1" | jq . >/dev/null 2>&1 || return 1
    return 0
}

section "TaiXiu Idempotency — Login"

test_name "Player login"
LOGIN_RESP=$(login_player "$PLAYER_USER" "$PLAYER_PASS")
PLAYER_AT=$(echo "$LOGIN_RESP" | jq -r '.accessToken // empty' 2>/dev/null)
if [[ -z "$PLAYER_AT" ]]; then
    _fail "Login failed — resp: ${LOGIN_RESP:0:200}"
    print_summary
fi
_pass "Got player accessToken"
export PLAYER_AT

# Liveness probe
PROBE=$(minigame_post "/api/v2/taixiu/join" '{"moneyType":1}')
if ! _endpoint_live "$PROBE"; then
    section "TaiXiu Idempotency — ALL PENDING"
    test_name "clientNonce repeat returns same perBetTxId + currentMoney"
    pending_skip "/api/v2/taixiu/* not yet deployed"
    print_summary
fi

# Join and check betting is open
JOIN_RESP=$(minigame_post "/api/v2/taixiu/join" '{"moneyType":1}')
BETTING_STATE=$(echo "$JOIN_RESP" | jq -r '.bettingState' 2>/dev/null || echo "false")

if [[ "$BETTING_STATE" != "true" ]]; then
    section "TaiXiu Idempotency — ALL PENDING (betting window closed)"
    test_name "clientNonce idempotency"
    pending_skip "bettingState=false at test time — retry at round start"
    print_summary
fi

# ─── Idempotency test ─────────────────────────────────────────────────────────
section "TaiXiu Idempotency — First bet"

# Use a timestamp-based nonce that is unique per test run but stable within this run
NONCE="idempotency-test-$(date +%s%N)"
BET_BODY="{\"moneyType\":1,\"betValue\":1000,\"betSide\":1,\"clientNonce\":\"${NONCE}\"}"

test_name "First bet with nonce=${NONCE}"
RESP1=$(minigame_post "/api/v2/taixiu/bet" "$BET_BODY")
echo "  Response 1: $(echo "$RESP1" | jq -c . 2>/dev/null || echo "${RESP1:0:300}")"

assert_status_200 "$RESP1"
assert_bet_response_dto "$RESP1"

TX_ID_1=$(echo "$RESP1" | jq -r '.perBetTxId // empty' 2>/dev/null)
MONEY_1=$(echo "$RESP1" | jq -r '.currentMoney // -1' 2>/dev/null || echo "-1")
echo "  perBetTxId=${TX_ID_1}  currentMoney=${MONEY_1}"

if [[ -z "$TX_ID_1" ]]; then
    _fail "First bet returned no perBetTxId — cannot test idempotency"
    print_summary
fi

# ─── Repeat identical request ─────────────────────────────────────────────────
section "TaiXiu Idempotency — Repeated bet (same nonce)"

test_name "Second bet with identical clientNonce → cached response"
RESP2=$(minigame_post "/api/v2/taixiu/bet" "$BET_BODY")
echo "  Response 2: $(echo "$RESP2" | jq -c . 2>/dev/null || echo "${RESP2:0:300}")"

TX_ID_2=$(echo "$RESP2" | jq -r '.perBetTxId // empty' 2>/dev/null)
MONEY_2=$(echo "$RESP2" | jq -r '.currentMoney // -1' 2>/dev/null || echo "-1")
echo "  perBetTxId=${TX_ID_2}  currentMoney=${MONEY_2}"

test_name "Same perBetTxId on repeated nonce (idempotency key)"
if [[ "$TX_ID_1" == "$TX_ID_2" && -n "$TX_ID_1" ]]; then
    _pass "perBetTxId is identical on repeat: ${TX_ID_1}"
else
    _fail "perBetTxId changed on repeat: first=${TX_ID_1} second=${TX_ID_2} — wallet may have been double-debited"
fi

test_name "Same currentMoney on repeated nonce (wallet not double-debited)"
if [[ "$MONEY_1" == "$MONEY_2" && "$MONEY_1" != "-1" ]]; then
    _pass "currentMoney is identical on repeat: ${MONEY_1}"
else
    _fail "currentMoney differs: first=${MONEY_1} second=${MONEY_2} — double-debit detected or field missing"
fi

# ─── Third repeat (still cached) ─────────────────────────────────────────────
section "TaiXiu Idempotency — Third repeat (cache still valid)"

test_name "Third bet with same nonce → still returns cached perBetTxId"
RESP3=$(minigame_post "/api/v2/taixiu/bet" "$BET_BODY")
TX_ID_3=$(echo "$RESP3" | jq -r '.perBetTxId // empty' 2>/dev/null)
MONEY_3=$(echo "$RESP3" | jq -r '.currentMoney // -1' 2>/dev/null || echo "-1")
echo "  perBetTxId=${TX_ID_3}  currentMoney=${MONEY_3}"

if [[ "$TX_ID_1" == "$TX_ID_3" && "$MONEY_1" == "$MONEY_3" ]]; then
    _pass "Third repeat: perBetTxId and currentMoney still cached"
else
    _fail "Third repeat inconsistent: txId first=${TX_ID_1} third=${TX_ID_3}; money first=${MONEY_1} third=${MONEY_3}"
fi

# ─── Different nonce produces a different txId ───────────────────────────────
section "TaiXiu Idempotency — Different nonce produces new txId"

test_name "New clientNonce → new perBetTxId (cache is per-nonce, not global)"
NEW_NONCE="idempotency-test-new-$(date +%s%N)"
RESP_NEW=$(minigame_post "/api/v2/taixiu/bet" \
    "{\"moneyType\":1,\"betValue\":1000,\"betSide\":1,\"clientNonce\":\"${NEW_NONCE}\"}")
echo "  Response new: $(echo "$RESP_NEW" | jq -c . 2>/dev/null || echo "${RESP_NEW:0:300}")"
TX_ID_NEW=$(echo "$RESP_NEW" | jq -r '.perBetTxId // empty' 2>/dev/null)

if [[ -n "$TX_ID_NEW" && "$TX_ID_NEW" != "$TX_ID_1" ]]; then
    _pass "New nonce produced new perBetTxId: ${TX_ID_NEW} (vs ${TX_ID_1})"
else
    # May also fail because betting just closed — acceptable
    EC=$(echo "$RESP_NEW" | jq -r '.errorCode // ""' 2>/dev/null || echo "")
    if [[ "$EC" == "0002" ]]; then
        _skip "Betting window closed before second nonce could be placed (errorCode=0002)"
    else
        _fail "New nonce returned same txId or no txId: new=${TX_ID_NEW} original=${TX_ID_1} ec=${EC}"
    fi
fi

print_summary

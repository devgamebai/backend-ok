#!/bin/bash
# test_awc_callback.sh — AWC seamless wallet regression suite
#
# Covers the AWC API test scenarios (SXB-1..SXB-13) that the AWC
# certification platform runs. Each scenario is reproduced via curl
# against /awc/callback and the balance ledger is verified after every
# step.
#
# Why this test exists:
#   AWC's external test platform takes ~minutes to run and gives no
#   precise repro for failures. This suite gives us a 30-second local
#   regression so we catch breakages before AWC sees them.
#
# Run:
#   bash tests/test_awc_callback.sh
#
# Prereqs:
#   - staging-play.sunkr.bet reachable
#   - AWC_CALLBACK_KEY env var set (defaults to current staging cert)
#   - test player nguoinaodo (user_name=nguoinaodo, nick_name=laviai) exists

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh" 2>/dev/null || true

# ────────────────────────────────────────────────
# Config
# ────────────────────────────────────────────────
AWC_CALLBACK_URL="${AWC_CALLBACK_URL:-https://staging-play.sunkr.bet/awc/callback}"
AWC_CALLBACK_KEY="${AWC_CALLBACK_KEY:-PigVq2D07hNL}"
AWC_USER="${AWC_USER:-nguoinaodo}"

# Colors (in case helpers.sh wasn't sourced)
GREEN="${GREEN:-\033[0;32m}"
RED="${RED:-\033[0;31m}"
YELLOW="${YELLOW:-\033[1;33m}"
CYAN="${CYAN:-\033[0;36m}"
BOLD="${BOLD:-\033[1m}"
RESET="${RESET:-\033[0m}"

PASS=0
FAIL=0
TS="$(date +%s%N)"

# ────────────────────────────────────────────────
# Wire helpers
# ────────────────────────────────────────────────

awc_call() {
    curl -s -X POST "$AWC_CALLBACK_URL" \
        --data-urlencode "key=$AWC_CALLBACK_KEY" \
        --data-urlencode "message=$1"
}

bal() {
    awc_call "{\"action\":\"getBalance\",\"userId\":\"$AWC_USER\"}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("balance", -1))'
}

bet() {
    local TXID=$1 AMT=$2
    awc_call "{\"action\":\"bet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"betType\":\"Banker\",\"currency\":\"VND\",\"betTime\":\"2026-05-03T22:00:00.000+08:00\",\"betAmount\":$AMT,\"jackpotBetAmount\":0,\"gameInfo\":{}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("balance", d.get("status", "ERR")))'
}

settle() {
    local TXID=$1 BET=$2 WIN=$3
    awc_call "{\"action\":\"settle\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"settleType\":\"platformTxId\",\"updateTime\":\"2026-05-03T22:00:01.000+08:00\",\"betType\":\"Banker\",\"betTime\":\"2026-05-03T22:00:00.000+08:00\",\"txTime\":\"2026-05-03T22:00:00.000+08:00\",\"turnover\":$BET,\"betAmount\":$BET,\"winAmount\":$WIN,\"jackpotWinAmount\":0,\"gameInfo\":{\"status\":\"WIN\"}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status", "ERR"))'
}

bet_n_settle() {
    local TXID=$1 BET=$2 WIN=$3
    awc_call "{\"action\":\"betNSettle\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"betType\":\"Banker\",\"currency\":\"VND\",\"betTime\":\"2026-05-03T22:00:00.000+08:00\",\"betAmount\":$BET,\"winAmount\":$WIN,\"turnover\":$BET,\"jackpotBetAmount\":0,\"jackpotWinAmount\":0,\"gameInfo\":{\"status\":\"WIN\"}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("balance", d.get("status", "ERR")))'
}

void_bet() {
    local TXID=$1 BET=$2
    awc_call "{\"action\":\"voidBet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"updateTime\":\"2026-05-03T22:00:02.000+08:00\",\"voidType\":2,\"betAmount\":$BET,\"gameInfo\":{}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status", "ERR"))'
}

void_settle() {
    local TXID=$1
    awc_call "{\"action\":\"voidSettle\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"updateTime\":\"2026-05-03T22:00:03.000+08:00\",\"voidType\":2,\"betAmount\":10,\"gameInfo\":{}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status", "ERR"))'
}

cancel_bet() {
    local TXID=$1
    awc_call "{\"action\":\"cancelBet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"gameInfo\":{}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("balance", d.get("status", "ERR")))'
}

cancel_bet_n_settle() {
    local TXID=$1
    awc_call "{\"action\":\"cancelBetNSettle\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TXID\",\"roundId\":\"R-$TXID\",\"gameInfo\":{}}]}" \
        | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("balance", d.get("status", "ERR")))'
}

eq() {
    local NAME="$1" GOT="$2" WANT="$3"
    if [ "$(echo "$GOT == $WANT" | bc -l 2>/dev/null)" = "1" ]; then
        echo -e "${GREEN}✅ $NAME${RESET}: $GOT"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}❌ $NAME${RESET}: got=$GOT want=$WANT"
        FAIL=$((FAIL + 1))
    fi
}

# ────────────────────────────────────────────────
# Suite
# ────────────────────────────────────────────────

echo -e "${BOLD}${CYAN}AWC Callback Regression Suite${RESET}"
echo "URL: $AWC_CALLBACK_URL"
echo "User: $AWC_USER"

B0=$(bal)
echo -e "${YELLOW}Initial balance: $B0${RESET}"
echo

# SXB-1 — bet → settle WIN
T="S1-$TS"
B=$(bet "$T" 10);                eq "SXB-1 bet -10"             "$B" "$(echo "$B0-10" | bc -l)"
settle "$T" 10 19.5 > /dev/null; B=$(bal); eq "SXB-1 settle WIN +19.5"     "$B" "$(echo "$B0+9.5" | bc -l)"
B1=$B

# SXB-2 — bet → settle LOSE
T="S2-$TS"
Bx=$(bet "$T" 10);                eq "SXB-2 bet -10"             "$Bx" "$(echo "$B1-10" | bc -l)"
settle "$T" 10 0 > /dev/null;     B=$(bal); eq "SXB-2 settle LOSE (no win)" "$B"  "$Bx"
B2=$B

# SXB-5 — 5x settle dedup (reuse SXB-1 platformTxId — already settled)
T="S1-$TS"
for i in 1 2 3 4 5; do settle "$T" 10 19.5 > /dev/null; done
B=$(bal);                        eq "SXB-5 5x settle dedup (no extra credit)" "$B" "$B2"
B5=$B

# SXB-6 — bet → voidBet
T="S6-$TS"
Bx=$(bet "$T" 10);                eq "SXB-6 bet -10"             "$Bx" "$(echo "$B5-10" | bc -l)"
void_bet "$T" 10 > /dev/null;     B=$(bal); eq "SXB-6 voidBet refund +10"   "$B"  "$B5"
B6=$B

# SXB-7 — bet → 5x voidBet dedup
T="S7-$TS"
Bx=$(bet "$T" 10);                eq "SXB-7 bet -10"             "$Bx" "$(echo "$B6-10" | bc -l)"
for i in 1 2 3 4 5; do void_bet "$T" 10 > /dev/null; done
B=$(bal);                        eq "SXB-7 5x voidBet dedup"   "$B"  "$B6"
B7=$B

# SXB-8 — bet → settle → voidSettle (clawback winLoss)
T="S8-$TS"
bet "$T" 10 > /dev/null
settle "$T" 10 19.5 > /dev/null; Bps=$(bal)
void_settle "$T" > /dev/null;    B=$(bal); eq "SXB-8 voidSettle clawback -9.5" "$B" "$(echo "$Bps-9.5" | bc -l)"
B8=$B

# SXB-12 — bet → cancelBet
T="S12-$TS"
Bx=$(bet "$T" 10)
cancel_bet "$T" > /dev/null;     B=$(bal); eq "SXB-12 cancelBet refund +10" "$B" "$(echo "$Bx+10" | bc -l)"
# SXB-12-4 retry: 2nd cancelBet must be no-op (SUN-AWC-CANCEL-DEDUP)
B12_post=$B
cancel_bet "$T" > /dev/null;     B=$(bal); eq "SXB-12 cancelBet RETRY dedup (no extra refund)" "$B" "$B12_post"
B12=$B

# SXB-13 — betNSettle → cancelBetNSettle
T="S13-$TS"
Bx=$(bet_n_settle "$T" 10 19.5); eq "SXB-13 betNSettle (+9.5 net)" "$Bx" "$(echo "$B12+9.5" | bc -l)"
cancel_bet_n_settle "$T" > /dev/null; B=$(bal); eq "SXB-13 cancelBetNSettle reverse" "$B" "$B12"
# SXB-13-4 retry: 2nd cancelBetNSettle must be no-op (SUN-AWC-CANCEL-DEDUP)
B13_post=$B
cancel_bet_n_settle "$T" > /dev/null; B=$(bal); eq "SXB-13 cancelBetNSettle RETRY dedup (no extra reverse)" "$B" "$B13_post"

# SXB-14 — orphan cancelBet (no prior bet) → must return status 0000 with current balance
T="S14-$TS"
ORPHAN_RESP=$(awc_call "{\"action\":\"cancelBet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"roundId\":\"R-$T\",\"gameInfo\":{}}]}")
ORPHAN_STATUS=$(echo "$ORPHAN_RESP" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status","ERR"))')
if [ "$ORPHAN_STATUS" = "0000" ]; then
    echo -e "${GREEN}✅ SXB-14 orphan cancelBet returns 0000${RESET}: $ORPHAN_STATUS"
    PASS=$((PASS + 1))
else
    echo -e "${RED}❌ SXB-14 orphan cancelBet returns 0000${RESET}: got=$ORPHAN_STATUS resp=$ORPHAN_RESP"
    FAIL=$((FAIL + 1))
fi
B=$(bal); B_PRE_S14=27191719
# Orphan cancel must NOT mutate balance (no prior bet to refund)
B14_post=$B
ORPHAN_RESP2=$(awc_call "{\"action\":\"cancelBet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"roundId\":\"R-$T\",\"gameInfo\":{}}]}")
ORPHAN_STATUS2=$(echo "$ORPHAN_RESP2" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status","ERR"))')
B=$(bal); eq "SXB-14 orphan cancelBet RETRY dedup (no extra mutation)" "$B" "$B14_post"
if [ "$ORPHAN_STATUS2" = "0000" ]; then
    echo -e "${GREEN}✅ SXB-14 orphan cancelBet retry returns 0000${RESET}: $ORPHAN_STATUS2"
    PASS=$((PASS + 1))
else
    echo -e "${RED}❌ SXB-14 orphan cancelBet retry returns 0000${RESET}: got=$ORPHAN_STATUS2"
    FAIL=$((FAIL + 1))
fi

# SXB-15 — CancelBet (orphan, with explicit betAmount in body) → Bet (same txId)
# AWC SXB-15 expects: cancelBet without prior bet must be no-op REGARDLESS of betAmount in body
T="S15-$TS"
B_PRE=$(bal)
# Step 1: orphan cancelBet WITH betAmount=10 in body — must NOT credit
ORPH_R=$(awc_call "{\"action\":\"cancelBet\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"roundId\":\"R-$T\",\"betAmount\":10,\"gameInfo\":{}}]}")
ORPH_STATUS=$(echo "$ORPH_R" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status","ERR"))')
B=$(bal); eq "SXB-15 orphan cancelBet w/ betAmount=10 (no mutation)" "$B" "$B_PRE"
[ "$ORPH_STATUS" = "0000" ] && { echo -e "${GREEN}✅ SXB-15 cancelBet status 0000${RESET}"; PASS=$((PASS+1)); } || { echo -e "${RED}❌ SXB-15 cancelBet status${RESET}: $ORPH_STATUS"; FAIL=$((FAIL+1)); }
# Step 2: bet with same txId — AWC SXB-15 expects bet to be REJECTED
# (txId already claimed by cancelBet → bet is dedup no-op, balance unchanged)
Bx=$(bet "$T" 10); eq "SXB-15 bet on cancelled txId is no-op (balance unchanged)" "$Bx" "$B_PRE"

# SXB-16 — bulk bet (5 txns in one msg) → bulk cancelBet (5 txns)
TS16="${TS}-16"
build_bulk() {
    local ACTION=$1; shift
    local TXNS=""
    local IDX=1
    for TID in "$@"; do
        [ -n "$TXNS" ] && TXNS="$TXNS,"
        if [ "$ACTION" = "bet" ]; then
            TXNS="$TXNS{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TID\",\"roundId\":\"R-$TID\",\"betType\":\"Banker\",\"currency\":\"VND\",\"betTime\":\"2026-05-04T22:00:00.000+08:00\",\"betAmount\":10,\"jackpotBetAmount\":0,\"gameInfo\":{}}"
        else
            TXNS="$TXNS{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$TID\",\"roundId\":\"R-$TID\",\"gameInfo\":{}}"
        fi
        IDX=$((IDX+1))
    done
    echo "{\"action\":\"$ACTION\",\"txns\":[$TXNS]}"
}

B_PRE16=$(bal)
TIDS=("S16A-$TS16" "S16B-$TS16" "S16C-$TS16" "S16D-$TS16" "S16E-$TS16")
BULK_BET=$(build_bulk "bet" "${TIDS[@]}")
R=$(awc_call "$BULK_BET")
B_AFTER_BULK_BET=$(bal); eq "SXB-16 bulk bet 5 × -10 (-50)" "$B_AFTER_BULK_BET" "$(echo "$B_PRE16-50" | bc -l)"
BULK_CANCEL=$(build_bulk "cancelBet" "${TIDS[@]}")
R=$(awc_call "$BULK_CANCEL")
B_AFTER_BULK_CANCEL=$(bal); eq "SXB-16 bulk cancelBet 5 × +10 (+50)" "$B_AFTER_BULK_CANCEL" "$B_PRE16"

# SXB-21 — Tip after CancelTip (orphan): cancelTip claims txId, subsequent tip is no-op
T="S21-$TS"
B_PRE21=$(bal)
# Step 1: orphan cancelTip (no prior tip)
awc_call "{\"action\":\"cancelTip\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"gameInfo\":{}}]}" > /dev/null
B=$(bal); eq "SXB-21 orphan cancelTip (no mutation)" "$B" "$B_PRE21"
# Step 2: tip on same txId — must be no-op (cancelTip claimed)
awc_call "{\"action\":\"tip\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"tip\":10,\"currency\":\"VND\",\"gameInfo\":{}}]}" > /dev/null
B=$(bal); eq "SXB-21 tip on cancelled txId is no-op" "$B" "$B_PRE21"

# SXB-17 — tip event (AWC sends field "tip", not "tipAmount")
T="S17-$TS"
B_PRE17=$(bal)
TIP_R=$(awc_call "{\"action\":\"tip\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"gameName\":\"BaccaratClassic\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"tip\":10,\"currency\":\"VND\",\"txTime\":\"2026-05-04T12:00:00.000+08:00\",\"tipInfo\":{\"unitPrice\":10,\"quantity\":1,\"receiverId\":\"Alice/Bulgaria/1501544\",\"giftName\":\"Balloon\",\"tableId\":\"1\",\"dealerDomain\":\"Mexico\"},\"type\":\"DEALER_TIPPING\"}]}")
TIP_STATUS=$(echo "$TIP_R" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status","ERR"))')
B=$(bal); eq "SXB-17 tip 10 debit (-10)" "$B" "$(echo "$B_PRE17-10" | bc -l)"
[ "$TIP_STATUS" = "0000" ] && { echo -e "${GREEN}✅ SXB-17 tip status 0000${RESET}"; PASS=$((PASS+1)); } || { echo -e "${RED}❌ SXB-17 tip status${RESET}: $TIP_STATUS"; FAIL=$((FAIL+1)); }
# cancelTip refund
CTIP_R=$(awc_call "{\"action\":\"cancelTip\",\"txns\":[{\"gameType\":\"LIVE\",\"gameCode\":\"MX-LIVE-001\",\"platform\":\"SEXYBCRT\",\"userId\":\"$AWC_USER\",\"platformTxId\":\"$T\",\"gameInfo\":{}}]}")
B=$(bal); eq "SXB-17 cancelTip refund (+10)" "$B" "$B_PRE17"

echo
TOTAL=$((PASS + FAIL))
if [ "$FAIL" -eq 0 ]; then
    echo -e "${BOLD}${GREEN}=== AWC SUITE: $PASS/$TOTAL PASS ===${RESET}"
    exit 0
else
    echo -e "${BOLD}${RED}=== AWC SUITE: $PASS pass / $FAIL FAIL (of $TOTAL) ===${RESET}"
    exit 1
fi

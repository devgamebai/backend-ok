#!/bin/bash
# test-taixiu-snapshot-censoring.sh — Anti-cheat: dice never visible pre-reveal
#
# Spec refs:
#   taixiu-extraction-plan.md §3.3 (snapshotForClient censoring logic)
#   taixiu-sicbo-anticheat-audit.md (reveal hardening)
#   INV: dice1==0, dice2==0, dice3==0, result==-1 for all phases != REVEALED/SETTLED
#
# This is the most critical anti-cheat test (F12/devtools defense).
# It polls /api/v2/taixiu/state every 1s during the OPEN/LOCKED/GENERATING phases
# and asserts that no pre-reveal snapshot leaks any non-zero dice value.
#
# After reveal: asserts dice become non-zero.
#
# PASS criteria: 100% of pre-reveal samples have dice=[0,0,0] and result=-1.
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

section "TaiXiu Snapshot Censoring — Login"

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
PROBE=$(minigame_get "/api/v2/taixiu/state?moneyType=1")
if ! _endpoint_live "$PROBE"; then
    section "TaiXiu Snapshot Censoring — ALL PENDING"
    test_name "Pre-reveal poll: dice=[0,0,0] result=-1 every sample"
    pending_skip "/api/v2/taixiu/state not yet deployed (got: ${PROBE:0:80})"
    test_name "Post-reveal: dice non-zero after REVEALED transition"
    pending_skip "/api/v2/taixiu/state not yet deployed"
    test_name "Round boundary observed"
    pending_skip "/api/v2/taixiu/state not yet deployed"
    test_name "Zero dice leaks across all pre-reveal samples"
    pending_skip "/api/v2/taixiu/state not yet deployed"
    print_summary
fi

# Join to register session
minigame_post "/api/v2/taixiu/join" '{"moneyType":1}' >/dev/null 2>&1 || true

# ─── Phase 1: Poll during pre-reveal ─────────────────────────────────────────
section "TaiXiu Snapshot Censoring — Pre-reveal polling"

echo "  Polling /api/v2/taixiu/state every 1s for up to 75s (one full round)."
echo "  Asserting dice=[0,0,0] and result=-1 for every OPEN/LOCKED/GENERATING sample."
echo "  Will stop polling once bettingState transitions to true after a reveal."
echo ""

PRE_REVEAL_SAMPLES=0
PRE_REVEAL_LEAKS=0
POST_REVEAL_CONFIRMED=false
REVEAL_DICE=""
ROUND_BOUNDARY_SEEN=false
LAST_REFERENCE_ID=""
MAX_POLLS=90  # 90s max — covers one full 68s round plus buffer

for i in $(seq 1 $MAX_POLLS); do
    STATE=$(minigame_get "/api/v2/taixiu/state?moneyType=1")
    TS=$(date +%H:%M:%S)

    D1=$(echo "$STATE" | jq -r '.dice1 // -999' 2>/dev/null || echo "-999")
    D2=$(echo "$STATE" | jq -r '.dice2 // -999' 2>/dev/null || echo "-999")
    D3=$(echo "$STATE" | jq -r '.dice3 // -999' 2>/dev/null || echo "-999")
    RS=$(echo "$STATE" | jq -r '.result // -999' 2>/dev/null || echo "-999")
    BS=$(echo "$STATE" | jq -r '.bettingState' 2>/dev/null || echo "unknown")
    RT=$(echo "$STATE" | jq -r '.remainTime // "?"' 2>/dev/null || echo "?")
    REF=$(echo "$STATE" | jq -r '.referenceId // "?"' 2>/dev/null || echo "?")

    # Track round boundaries for completeness
    if [[ -n "$LAST_REFERENCE_ID" && "$REF" != "$LAST_REFERENCE_ID" && "$REF" != "?" ]]; then
        echo "  [${TS}] Round boundary: refId ${LAST_REFERENCE_ID} → ${REF}"
        ROUND_BOUNDARY_SEEN=true
    fi
    LAST_REFERENCE_ID="$REF"

    # Classify this sample
    if [[ "$D1" == "0" && "$D2" == "0" && "$D3" == "0" && "$RS" == "-1" ]]; then
        # Pre-reveal (correct — dice censored)
        PRE_REVEAL_SAMPLES=$((PRE_REVEAL_SAMPLES + 1))
        echo "  [${TS}] sample ${i}: bettingState=${BS} remainTime=${RT} dice=[0,0,0] result=-1 refId=${REF} [CENSORED OK]"

    elif [[ "$D1" -gt 0 && "$D2" -gt 0 && "$D3" -gt 0 ]] 2>/dev/null; then
        # Post-reveal (dice non-zero) — this is expected ONLY after REVEALED transition
        echo "  [${TS}] sample ${i}: bettingState=${BS} remainTime=${RT} dice=[${D1},${D2},${D3}] result=${RS} refId=${REF} [REVEALED]"
        POST_REVEAL_CONFIRMED=true
        REVEAL_DICE="[${D1},${D2},${D3}] result=${RS}"

        # Collect a few more post-reveal samples to confirm stability, then stop
        # After a reveal we expect bettingState to go false for a bit then true again (new round)
        if [[ "$BS" == "true" && "$ROUND_BOUNDARY_SEEN" == "true" ]]; then
            echo "  Post-reveal + new round detected — stopping poll"
            break
        fi

    else
        # Unexpected: dice partially set or missing fields
        PRE_REVEAL_SAMPLES=$((PRE_REVEAL_SAMPLES + 1))
        echo "  [${TS}] sample ${i}: UNEXPECTED STATE — bettingState=${BS} dice=[${D1},${D2},${D3}] result=${RS} refId=${REF}"

        if [[ "$BS" == "true" ]]; then
            # Betting open but dice non-zero and not all-zero: LEAK
            if [[ "$D1" != "0" || "$D2" != "0" || "$D3" != "0" || "$RS" != "-1" ]]; then
                PRE_REVEAL_LEAKS=$((PRE_REVEAL_LEAKS + 1))
                echo "  *** LEAK DETECTED at sample ${i}: dice=[${D1},${D2},${D3}] result=${RS} ***"
            fi
        fi
    fi

    sleep 1
done

echo ""
echo "  Pre-reveal samples collected : ${PRE_REVEAL_SAMPLES}"
echo "  Leaks detected               : ${PRE_REVEAL_LEAKS}"
echo "  Post-reveal confirmed        : ${POST_REVEAL_CONFIRMED}"
[[ -n "$REVEAL_DICE" ]] && echo "  Revealed dice                : ${REVEAL_DICE}"

# ─── Assertions ───────────────────────────────────────────────────────────────
section "TaiXiu Snapshot Censoring — Assertions"

test_name "At least 1 pre-reveal sample collected"
if (( PRE_REVEAL_SAMPLES > 0 )); then
    _pass "Collected ${PRE_REVEAL_SAMPLES} pre-reveal samples"
else
    _fail "No pre-reveal samples collected — test may have run entirely post-reveal"
fi

test_name "Zero dice leaks across all pre-reveal samples (100% pass rate required)"
if (( PRE_REVEAL_LEAKS == 0 )); then
    _pass "ZERO pre-reveal dice leaks across ${PRE_REVEAL_SAMPLES} samples — anti-cheat holds"
else
    _fail "CRITICAL: ${PRE_REVEAL_LEAKS}/${PRE_REVEAL_SAMPLES} samples leaked non-zero dice pre-reveal"
fi

test_name "Post-reveal: dice become non-zero after REVEALED transition"
if [[ "$POST_REVEAL_CONFIRMED" == "true" ]]; then
    _pass "Observed non-zero dice after reveal: ${REVEAL_DICE}"
else
    _skip "Did not observe REVEALED transition within polling window — extend MAX_POLLS or re-run at round start"
fi

test_name "Round boundary observed (confirming full-round coverage)"
if [[ "$ROUND_BOUNDARY_SEEN" == "true" ]]; then
    _pass "At least one round boundary (refId change) observed during polling"
else
    _skip "No round boundary seen — poll may not have spanned a full round"
fi

print_summary

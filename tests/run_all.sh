#!/bin/bash
# run_all.sh — Execute all API test suites and report summary
# Usage: bash tests/run_all.sh [--fast] [--suite test_bank]

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# ────────────────────────────────────────────────
# Parse args
# ────────────────────────────────────────────────
FAST_MODE=false
ONLY_SUITE=""
for arg in "$@"; do
    case "$arg" in
        --fast) FAST_MODE=true ;;
        --suite) shift; ONLY_SUITE="$1" ;;
        --suite=*) ONLY_SUITE="${arg#--suite=}" ;;
    esac
done

# ────────────────────────────────────────────────
# All test suites in execution order
# ────────────────────────────────────────────────
ALL_SUITES=(
    "test_auth"
    "test_bank"
    "test_deposit"
    "test_withdraw"
    "test_promotion"
    "test_banner_event"
    "test_dashboard"
    "test_user"
    "test_role"
    "test_cashback"
    "test_games"
    "test_agent"
    "test_admin"
    "test_awc_callback"
    "test_login_log_duplicate"
    "test_provider_contract"
)

# ────────────────────────────────────────────────
# Connectivity pre-check
# ────────────────────────────────────────────────
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}  SUNWINKR API TEST RUNNER${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  Date     : $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "  Player   : https://staging-play.sunkr.bet/api"
echo -e "  Admin    : https://staging-admin.sunkr.bet/api_backend"
echo ""

echo -e "${BOLD}Pre-flight connectivity check...${RESET}"
PLAYER_STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 10 \
    "https://staging-play.sunkr.bet/api?c=3&un=superadmin&pw=0192023a7bbd73250516f069df18b500" 2>/dev/null || echo "000")
ADMIN_STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 10 \
    "https://staging-admin.sunkr.bet/api_backend?c=701&un=superadmin&pw=0192023a7bbd73250516f069df18b500" 2>/dev/null || echo "000")

if [[ "$PLAYER_STATUS" == "200" ]]; then
    echo -e "  ${GREEN}Player API: HTTP $PLAYER_STATUS${RESET}"
else
    echo -e "  ${RED}Player API: HTTP $PLAYER_STATUS — UNREACHABLE${RESET}"
    echo -e "  ${RED}Aborting: cannot reach player API.${RESET}"
    exit 1
fi

if [[ "$ADMIN_STATUS" == "200" ]]; then
    echo -e "  ${ADMIN_STATUS:+}${GREEN}Admin API:  HTTP $ADMIN_STATUS${RESET}"
else
    echo -e "  ${RED}Admin API:  HTTP $ADMIN_STATUS — UNREACHABLE${RESET}"
    echo -e "  ${RED}Aborting: cannot reach admin API.${RESET}"
    exit 1
fi
echo ""

# ────────────────────────────────────────────────
# Determine suites to run
# ────────────────────────────────────────────────
if [[ -n "$ONLY_SUITE" ]]; then
    SUITES=("$ONLY_SUITE")
else
    SUITES=("${ALL_SUITES[@]}")
fi

# ────────────────────────────────────────────────
# Run each suite, capture exit code and output
# ────────────────────────────────────────────────
SUITE_RESULTS=()
SUITE_PASS=()
SUITE_FAIL=()
SUITE_SKIP=()
TOTAL_PASS=0
TOTAL_FAIL=0
TOTAL_SKIP=0
FAILED_SUITES=()

LOG_DIR="${SCRIPT_DIR}/../logs/test-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$LOG_DIR"

for suite in "${SUITES[@]}"; do
    SUITE_FILE="${SCRIPT_DIR}/${suite}.sh"
    if [[ ! -f "$SUITE_FILE" ]]; then
        echo -e "${YELLOW}SKIP${RESET} ${suite} — file not found: $SUITE_FILE"
        continue
    fi

    echo -e "${BOLD}Running: ${CYAN}${suite}${RESET}"
    LOG_FILE="${LOG_DIR}/${suite}.log"

    # Run suite, capture output and exit code
    START_TS=$(date +%s)
    set +e
    bash "$SUITE_FILE" 2>&1 | tee "$LOG_FILE"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e
    END_TS=$(date +%s)
    ELAPSED=$((END_TS - START_TS))

    # Parse pass/fail/skip counts from SUMMARY lines in log (strip ANSI codes first)
    CLEAN_LOG=$(sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" 2>/dev/null)
    P=$(echo "$CLEAN_LOG" | grep -E "^  Pass  :" | grep -oE '[0-9]+$' | tail -1)
    F=$(echo "$CLEAN_LOG" | grep -E "^  Fail  :" | grep -oE '[0-9]+$' | tail -1)
    S=$(echo "$CLEAN_LOG" | grep -E "^  Skip  :" | grep -oE '[0-9]+$' | tail -1)
    P=${P:-0}; F=${F:-0}; S=${S:-0}

    TOTAL_PASS=$((TOTAL_PASS + P))
    TOTAL_FAIL=$((TOTAL_FAIL + F))
    TOTAL_SKIP=$((TOTAL_SKIP + S))

    if [[ $EXIT_CODE -eq 0 ]]; then
        echo -e "  ${GREEN}Suite PASSED${RESET} in ${ELAPSED}s (pass=${P} fail=${F} skip=${S})"
    else
        echo -e "  ${RED}Suite FAILED${RESET} in ${ELAPSED}s (pass=${P} fail=${F} skip=${S})"
        FAILED_SUITES+=("$suite")
    fi
    echo ""
done

# ────────────────────────────────────────────────
# Final summary
# ────────────────────────────────────────────────
TOTAL=$((TOTAL_PASS + TOTAL_FAIL + TOTAL_SKIP))
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}FINAL SUMMARY${RESET}"
echo -e "  Total assertions : $TOTAL"
echo -e "  ${GREEN}Pass  : $TOTAL_PASS${RESET}"
echo -e "  ${RED}Fail  : $TOTAL_FAIL${RESET}"
echo -e "  ${YELLOW}Skip  : $TOTAL_SKIP${RESET}"
echo ""
if [[ ${#FAILED_SUITES[@]} -gt 0 ]]; then
    echo -e "  ${RED}Failed suites:${RESET}"
    for s in "${FAILED_SUITES[@]}"; do
        echo -e "    ${RED}- $s${RESET}"
    done
fi
echo -e "  Logs saved to: ${LOG_DIR}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

if [[ ${#FAILED_SUITES[@]} -gt 0 ]]; then
    exit 1
fi

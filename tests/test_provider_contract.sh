#!/bin/bash
# test_provider_contract.sh — SUN-1339 C1
#
# Entry-point for the provider-contract test suite.
# Discovered by run_all.sh as suite "test_provider_contract".
#
# Delegates to individual scripts in tests/test_provider_contract/:
#   - test_lottery_provider_contract.sh
#   - test_taixiu_provider_contract.sh
#   - test_sicbo_provider_contract.sh
#
# Each sub-script maintains its own PASS/FAIL/SKIP counters and calls
# print_summary internally. This wrapper aggregates exit codes: non-zero
# from any sub-script causes this suite to exit non-zero.
#
# Usage: bash tests/test_provider_contract.sh
#        bash tests/run_all.sh --suite=test_provider_contract

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SUB_DIR="${SCRIPT_DIR}/test_provider_contract"

# Colors (inline — helpers.sh may not be sourced here)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

SUB_SCRIPTS=(
    "test_lottery_provider_contract.sh"
    "test_taixiu_provider_contract.sh"
    "test_sicbo_provider_contract.sh"
)

OVERALL_EXIT=0

echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}  PROVIDER CONTRACT — SUN-1339 C1${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

for script in "${SUB_SCRIPTS[@]}"; do
    SCRIPT_PATH="${SUB_DIR}/${script}"
    if [[ ! -f "$SCRIPT_PATH" ]]; then
        echo -e "${YELLOW}SKIP${RESET} ${script} — file not found"
        continue
    fi

    echo -e "${BOLD}Running: ${CYAN}${script}${RESET}"
    set +e
    OUTPUT=$(bash "$SCRIPT_PATH" 2>&1)
    EXIT_CODE=$?
    set -e

    echo "$OUTPUT"

    # Parse counts from the sub-script's SUMMARY block (strip ANSI codes)
    CLEAN=$(echo "$OUTPUT" | sed 's/\x1b\[[0-9;]*m//g')
    P=$(echo "$CLEAN" | grep -E "^  Pass  :" | grep -oE '[0-9]+$' | tail -1)
    F=$(echo "$CLEAN" | grep -E "^  Fail  :" | grep -oE '[0-9]+$' | tail -1)
    S=$(echo "$CLEAN" | grep -E "^  Skip  :" | grep -oE '[0-9]+$' | tail -1)
    P=${P:-0}; F=${F:-0}; S=${S:-0}

    PASS_COUNT=$((PASS_COUNT + P))
    FAIL_COUNT=$((FAIL_COUNT + F))
    SKIP_COUNT=$((SKIP_COUNT + S))

    if [[ $EXIT_CODE -ne 0 ]]; then
        OVERALL_EXIT=1
        echo -e "  ${RED}Sub-suite FAILED${RESET}: ${script} (pass=${P} fail=${F} skip=${S})"
    else
        echo -e "  ${GREEN}Sub-suite PASSED${RESET}: ${script} (pass=${P} fail=${F} skip=${S})"
    fi
    echo ""
done

# Emit summary in the exact format run_all.sh parses (see run_all.sh:128-131)
TOTAL=$((PASS_COUNT + FAIL_COUNT + SKIP_COUNT))
echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}SUMMARY${RESET}"
echo -e "  Total : $TOTAL"
echo -e "  ${GREEN}Pass  : $PASS_COUNT${RESET}"
echo -e "  ${RED}Fail  : $FAIL_COUNT${RESET}"
echo -e "  ${YELLOW}Skip  : $SKIP_COUNT${RESET}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

exit $OVERALL_EXIT

#!/bin/bash
# run-all.sh — Minigame smoke test runner
#
# Runs all tests/smoke/minigame/test-*.sh scripts in order.
# Each script's stdout+stderr is tee'd to output/<timestamp>/<test>.log.
# Exits non-zero if any test script exits non-zero.
#
# Usage:
#   bash tests/smoke/minigame/run-all.sh
#   bash tests/smoke/minigame/run-all.sh --fast        # happy-path subset only (< 5 min)
#   bash tests/smoke/minigame/run-all.sh --suite test-taixiu-happy-path
#
# Env overrides (all optional):
#   BASE_URL        default: https://staging-play.sunkr.bet
#   ADMIN_BASE_URL  default: https://staging-admin.sunkr.bet
#   PLAYER_USER     default: zuestang
#   PLAYER_PASS     default: e3486545c690ee99b976888431dda037
#   ADMIN_USER      default: superadmin
#   ADMIN_PASS      default: 0192023a7bbd73250516f069df18b500
#   TIMEOUT         default: 10  (curl --max-time seconds)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ─── Colors ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# ─── Defaults ────────────────────────────────────────────────────────────────
export BASE_URL="${BASE_URL:-https://staging-play.sunkr.bet}"
export ADMIN_BASE_URL="${ADMIN_BASE_URL:-https://staging-admin.sunkr.bet}"
export PLAYER_USER="${PLAYER_USER:-zuestang2}"
export PLAYER_PASS="${PLAYER_PASS:-4581777b67685c53166793900e05f575}"
export ADMIN_USER="${ADMIN_USER:-superadmin}"
export ADMIN_PASS="${ADMIN_PASS:-0192023a7bbd73250516f069df18b500}"
export TIMEOUT="${TIMEOUT:-10}"

# ─── Parse args ───────────────────────────────────────────────────────────────
FAST_MODE=false
ONLY_SUITE=""
for arg in "$@"; do
    case "$arg" in
        --fast) FAST_MODE=true ;;
        --suite=*) ONLY_SUITE="${arg#--suite=}" ;;
        --suite) ;;  # handled below with shift — not available in this loop style
    esac
done

# ─── Test suite definitions ───────────────────────────────────────────────────
# Full ordered suite — total expected runtime: ~8-12 min (dominated by polling tests)
ALL_SUITES=(
    "test-taixiu-happy-path"
    "test-taixiu-error-cases"
    "test-taixiu-idempotency"
    "test-taixiu-snapshot-censoring"
    "test-sicbo-happy-path"
    "test-sicbo-one-dice-special"
    "test-admin-force-result"
    "test-stomp-tick-no-dice"
)

# Fast subset: happy paths + error cases + snapshot censoring.
# Target: < 5 minutes. Omits the long-polling tests (snapshot censoring polls
# up to 90s — still included as it is the most critical anti-cheat test).
FAST_SUITES=(
    "test-taixiu-happy-path"
    "test-taixiu-error-cases"
    "test-taixiu-snapshot-censoring"
    "test-sicbo-happy-path"
)

# ─── Output directory ─────────────────────────────────────────────────────────
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
export OUTPUT_DIR="${SCRIPT_DIR}/output/${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

# ─── Header ───────────────────────────────────────────────────────────────────
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}  MINIGAME SMOKE TEST RUNNER${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  Date     : $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "  Base URL : ${BASE_URL}"
echo -e "  Admin URL: ${ADMIN_BASE_URL}"
echo -e "  Output   : ${OUTPUT_DIR}"
if [[ "$FAST_MODE" == "true" ]]; then
    echo -e "  Mode     : ${YELLOW}FAST (happy-path subset)${RESET}"
else
    echo -e "  Mode     : FULL"
fi
echo ""

# ─── Dependency checks ────────────────────────────────────────────────────────
echo -e "${BOLD}Dependency check...${RESET}"
if ! command -v curl >/dev/null 2>&1; then
    echo -e "  ${RED}FATAL: curl not found${RESET}"; exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
    echo -e "  ${RED}FATAL: jq not found${RESET}"; exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo -e "  ${YELLOW}WARN: python3 not found — some frame-parse logic uses python3${RESET}"
fi
echo -e "  ${GREEN}curl: $(curl --version | head -1)${RESET}"
echo -e "  ${GREEN}jq: $(jq --version)${RESET}"
if command -v websocat >/dev/null 2>&1; then
    echo -e "  ${GREEN}websocat: $(websocat --version 2>&1 | head -1)${RESET}"
else
    echo -e "  ${YELLOW}websocat: not found — test-stomp-tick-no-dice.sh will SKIP${RESET}"
fi
echo ""

# ─── Connectivity pre-check ───────────────────────────────────────────────────
echo -e "${BOLD}Connectivity pre-check...${RESET}"
PLAYER_HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 10 \
    "${BASE_URL}/api?c=3&un=superadmin&pw=0192023a7bbd73250516f069df18b500" 2>/dev/null || echo "000")
ADMIN_HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 10 \
    "${ADMIN_BASE_URL}/api_backend?c=701&un=superadmin&pw=0192023a7bbd73250516f069df18b500" 2>/dev/null || echo "000")

if [[ "$PLAYER_HTTP" == "200" ]]; then
    echo -e "  ${GREEN}Player API : HTTP ${PLAYER_HTTP}${RESET}"
else
    echo -e "  ${RED}Player API : HTTP ${PLAYER_HTTP} — unreachable${RESET}"
    echo -e "  ${YELLOW}Continuing anyway — tests will mark pending where appropriate${RESET}"
fi
if [[ "$ADMIN_HTTP" == "200" ]]; then
    echo -e "  ${GREEN}Admin API  : HTTP ${ADMIN_HTTP}${RESET}"
else
    echo -e "  ${RED}Admin API  : HTTP ${ADMIN_HTTP} — unreachable${RESET}"
    echo -e "  ${YELLOW}Continuing anyway${RESET}"
fi
echo ""

# ─── Determine suites to run ──────────────────────────────────────────────────
if [[ -n "$ONLY_SUITE" ]]; then
    SUITES=("$ONLY_SUITE")
elif [[ "$FAST_MODE" == "true" ]]; then
    SUITES=("${FAST_SUITES[@]}")
else
    SUITES=("${ALL_SUITES[@]}")
fi

# ─── Run each suite ───────────────────────────────────────────────────────────
TOTAL_PASS=0
TOTAL_FAIL=0
TOTAL_SKIP=0
FAILED_SUITES=()
SUITE_START=$(date +%s)

for suite in "${SUITES[@]}"; do
    SUITE_FILE="${SCRIPT_DIR}/${suite}.sh"
    if [[ ! -f "$SUITE_FILE" ]]; then
        echo -e "${YELLOW}SKIP${RESET} ${suite} — file not found: ${SUITE_FILE}"
        continue
    fi

    echo -e "${BOLD}Running: ${CYAN}${suite}${RESET}"
    LOG_FILE="${OUTPUT_DIR}/${suite}.log"

    START_TS=$(date +%s)
    set +e
    bash "$SUITE_FILE" 2>&1 | tee "$LOG_FILE"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e
    END_TS=$(date +%s)
    ELAPSED=$((END_TS - START_TS))

    # Parse PASS/FAIL/SKIP counts from SUMMARY block (strip ANSI first)
    CLEAN=$(sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" 2>/dev/null || cat "$LOG_FILE")
    P=$(echo "$CLEAN" | grep -E "^  Pass  :" | grep -oE '[0-9]+$' | tail -1); P=${P:-0}
    F=$(echo "$CLEAN" | grep -E "^  Fail  :" | grep -oE '[0-9]+$' | tail -1); F=${F:-0}
    S=$(echo "$CLEAN" | grep -E "^  Skip  :" | grep -oE '[0-9]+$' | tail -1); S=${S:-0}

    TOTAL_PASS=$((TOTAL_PASS + P))
    TOTAL_FAIL=$((TOTAL_FAIL + F))
    TOTAL_SKIP=$((TOTAL_SKIP + S))

    if [[ $EXIT_CODE -eq 0 ]]; then
        echo -e "  ${GREEN}PASS${RESET} ${suite} in ${ELAPSED}s (pass=${P} fail=${F} skip=${S})"
    else
        echo -e "  ${RED}FAIL${RESET} ${suite} in ${ELAPSED}s (pass=${P} fail=${F} skip=${S})"
        FAILED_SUITES+=("$suite")
    fi
    echo ""
done

SUITE_END=$(date +%s)
TOTAL_ELAPSED=$((SUITE_END - SUITE_START))

# ─── Final summary ────────────────────────────────────────────────────────────
TOTAL=$((TOTAL_PASS + TOTAL_FAIL + TOTAL_SKIP))
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}FINAL SUMMARY${RESET}"
echo -e "  Total assertions : ${TOTAL}"
echo -e "  ${GREEN}Pass  : ${TOTAL_PASS}${RESET}"
echo -e "  ${RED}Fail  : ${TOTAL_FAIL}${RESET}"
echo -e "  ${YELLOW}Skip  : ${TOTAL_SKIP}${RESET}"
echo -e "  Duration : ${TOTAL_ELAPSED}s"
echo ""
if [[ ${#FAILED_SUITES[@]} -gt 0 ]]; then
    echo -e "  ${RED}Failed suites:${RESET}"
    for s in "${FAILED_SUITES[@]}"; do
        echo -e "    ${RED}- ${s}${RESET}"
    done
    echo ""
fi
echo -e "  Logs: ${OUTPUT_DIR}/"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

if [[ ${#FAILED_SUITES[@]} -gt 0 ]]; then
    exit 1
fi

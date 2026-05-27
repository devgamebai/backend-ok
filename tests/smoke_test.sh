#!/bin/bash
# smoke_test.sh — Fast smoke test for backend-api after deploy.
#
# Bypasses CAPTCHA via SMOKE_TEST_BYPASS_KEY (.env) + X-Smoke-Test-Key
# header. Runs from inside the backend-api container so client IP is
# loopback (127.0.0.1) and passes the SmokeTestBypass internal-IP check.
#
# Use:
#   bash tests/smoke_test.sh
#
# What it covers:
#   - c=701  admin login
#   - c=1992 health ping
#   - c=9930 admin betting history (hideBot=true/false delta)
#   - c=9843 agent betting history (hide_bot smoke)
#   - c=9520 dashboard summary (hot path)
#   - c=9610 user list
#
# Exit code: 0 = all pass, 1 = any fail.

set -uo pipefail

CONTAINER="${SMOKE_CONTAINER:-sunwinkr-backend-api}"
PORT="${SMOKE_PORT:-19082}"
SMOKE_KEY="${SMOKE_TEST_BYPASS_KEY:-$(grep '^SMOKE_TEST_BYPASS_KEY=' .env 2>/dev/null | cut -d= -f2)}"
ADMIN_USER="${ADMIN_USER:-superadmin}"
ADMIN_PASS="${ADMIN_PASS:-0192023a7bbd73250516f069df18b500}"

GREEN='\033[0;32m' RED='\033[0;31m' YELLOW='\033[1;33m' CYAN='\033[0;36m' BOLD='\033[1m' RESET='\033[0m'
PASS=0 FAIL=0

if [[ -z "$SMOKE_KEY" ]]; then
    echo -e "${RED}FATAL: SMOKE_TEST_BYPASS_KEY not set in .env or env${RESET}" >&2
    exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    echo -e "${RED}FATAL: container '$CONTAINER' not running${RESET}" >&2
    exit 1
fi

# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────
ecall() {
    docker exec "$CONTAINER" curl -s --max-time 10 \
        -H "X-Smoke-Test-Key: $SMOKE_KEY" \
        "http://localhost:$PORT/api_backend?$1"
}

assert_success() {
    local NAME="$1" RESP="$2"
    if echo "$RESP" | python3 -c 'import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get("success") is True or d.get("errorCode") in ("0",0,None) else 1)' 2>/dev/null; then
        echo -e "  ${GREEN}PASS${RESET} $NAME"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}FAIL${RESET} $NAME"
        echo "    response: $(echo "$RESP" | head -c 200)"
        FAIL=$((FAIL + 1))
    fi
}

assert_field() {
    local NAME="$1" RESP="$2" KEY="$3"
    local got
    got=$(echo "$RESP" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('$KEY', ''))" 2>/dev/null)
    if [[ -n "$got" && "$got" != "None" && "$got" != "0" ]]; then
        echo -e "  ${GREEN}PASS${RESET} $NAME → $KEY=$got"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}FAIL${RESET} $NAME → $KEY missing/empty"
        echo "    response: $(echo "$RESP" | head -c 200)"
        FAIL=$((FAIL + 1))
    fi
}

# ──────────────────────────────────────────────
# Header
# ──────────────────────────────────────────────
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}  SUNWINKR BACKEND-API SMOKE TEST${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo "  Container : $CONTAINER:$PORT"
echo "  Date      : $(date '+%Y-%m-%d %H:%M:%S')"
echo

# ──────────────────────────────────────────────
# 1. Health ping
# ──────────────────────────────────────────────
echo -e "${BOLD}1. Health ping (c=1992)${RESET}"
RESP=$(ecall "c=1992")
[[ -n "$RESP" ]] && echo -e "  ${GREEN}PASS${RESET} health" && PASS=$((PASS + 1)) || { echo -e "  ${RED}FAIL${RESET} no response"; FAIL=$((FAIL + 1)); }

# ──────────────────────────────────────────────
# 2. Admin login (smoke bypass)
# ──────────────────────────────────────────────
echo -e "\n${BOLD}2. Admin login (c=701, smoke bypass)${RESET}"
LOGIN=$(ecall "c=701&un=$ADMIN_USER&pw=$ADMIN_PASS")
AAT=$(echo "$LOGIN" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('accessToken',''))" 2>/dev/null)
if [[ -n "$AAT" ]]; then
    echo -e "  ${GREEN}PASS${RESET} login → AAT=${AAT:0:8}…"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}FAIL${RESET} login"
    echo "    response: $LOGIN"
    FAIL=$((FAIL + 1))
    echo -e "\n${RED}Cannot continue without admin token${RESET}"
    exit 1
fi

FROM=$(date -d '7 days ago' +%Y-%m-%d 2>/dev/null || date -v -7d +%Y-%m-%d)
TO=$(date +%Y-%m-%d)
FROM_TS="${FROM}T00:00:00"
TO_TS="${TO}T23:59:59"

# ──────────────────────────────────────────────
# 3. Admin betting history c=9930
# ──────────────────────────────────────────────
echo -e "\n${BOLD}3. Admin LS Cuoc (c=9930) — MR !303 hideBot${RESET}"
RESP1=$(ecall "c=9930&aat=$AAT&fromDate=$FROM&toDate=$TO&hideBot=true&page=1&pageSize=10")
TR1=$(echo "$RESP1" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('totalRecords',0))" 2>/dev/null)
RESP2=$(ecall "c=9930&aat=$AAT&fromDate=$FROM&toDate=$TO&hideBot=false&page=1&pageSize=10")
TR2=$(echo "$RESP2" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('totalRecords',0))" 2>/dev/null)
echo "  hideBot=true  totalRecords=$TR1"
echo "  hideBot=false totalRecords=$TR2"
if [[ "$TR2" -ge "$TR1" ]] 2>/dev/null; then
    echo -e "  ${GREEN}PASS${RESET} hideBot reduces (or equals) row count"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}FAIL${RESET} hideBot=true returned MORE than hideBot=false"
    FAIL=$((FAIL + 1))
fi

# ──────────────────────────────────────────────
# 4. Agent betting history c=9843
# ──────────────────────────────────────────────
echo -e "\n${BOLD}4. Agent LS Cuoc (c=9843) — MR !303 loadBotNicknameSet${RESET}"
RESP=$(ecall "c=9843&aat=$AAT&rc=$ADMIN_USER&ft=$FROM_TS&et=$TO_TS&hide_bot=true&p=1&l=5")
assert_success "c=9843 hide_bot=true" "$RESP"
RESP=$(ecall "c=9843&aat=$AAT&rc=$ADMIN_USER&ft=$FROM_TS&et=$TO_TS&hide_bot=false&p=1&l=5")
assert_success "c=9843 hide_bot=false" "$RESP"

# ──────────────────────────────────────────────
# 5. Dashboard summary (hot path)
# ──────────────────────────────────────────────
echo -e "\n${BOLD}5. Dashboard summary (c=9520)${RESET}"
RESP=$(ecall "c=9520&aat=$AAT&fromDate=$FROM&toDate=$TO")
assert_success "c=9520 dashboard" "$RESP"

# ──────────────────────────────────────────────
# 6. User list (basic CRUD path)
# ──────────────────────────────────────────────
echo -e "\n${BOLD}6. User list (c=9610)${RESET}"
RESP=$(ecall "c=9610&aat=$AAT&page=1&pageSize=5")
assert_success "c=9610 user list" "$RESP"

# ──────────────────────────────────────────────
# 7. Backend log scan for stack traces in last 60s
# ──────────────────────────────────────────────
echo -e "\n${BOLD}7. Backend log scan (last 60s)${RESET}"
ERRS=$(docker logs "$CONTAINER" --since 60s 2>&1 | grep -E "Exception|ERROR" 2>/dev/null | wc -l | tr -d ' ')
ERRS=${ERRS:-0}
if [[ "$ERRS" -le 2 ]]; then
    echo -e "  ${GREEN}PASS${RESET} ≤2 errors in last 60s (got $ERRS)"
    PASS=$((PASS + 1))
else
    echo -e "  ${YELLOW}WARN${RESET} $ERRS error lines in last 60s — investigate"
    docker logs "$CONTAINER" --since 60s 2>&1 | grep -E "Exception|ERROR" | head -5
    FAIL=$((FAIL + 1))
fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
TOTAL=$((PASS + FAIL))
echo
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
if [[ $FAIL -eq 0 ]]; then
    echo -e "${BOLD}${GREEN}SMOKE: $PASS/$TOTAL PASS${RESET}"
    exit 0
else
    echo -e "${BOLD}${RED}SMOKE: $PASS pass / $FAIL FAIL (of $TOTAL)${RESET}"
    exit 1
fi

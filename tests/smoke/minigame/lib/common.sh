#!/bin/bash
# lib/common.sh — Shared helpers for minigame smoke tests
#
# All tests in tests/smoke/minigame/ source this file.
#
# Env vars (set before sourcing, or use defaults):
#   BASE_URL          Player-facing Spring API base  (default: https://staging-play.sunkr.bet)
#   ADMIN_BASE_URL    Admin Spring API base           (default: https://staging-admin.sunkr.bet)
#   PLAYER_USER       Player account username         (default: zuestang)
#   PLAYER_PASS       Player account MD5 password     (default: e3486545c690ee99b976888431dda037)
#   ADMIN_USER        Admin account username          (default: superadmin)
#   ADMIN_PASS        Admin account MD5 password      (default: 0192023a7bbd73250516f069df18b500)
#   TIMEOUT           curl --max-time seconds         (default: 10)
#   OUTPUT_DIR        Directory for captured logs     (set by run-all.sh; falls back to /tmp)
#
# Depends on: curl, jq (both required on PATH)
# Optional:   websocat (for STOMP tests — test-stomp-tick-no-dice.sh documents separately)

# ─── Runtime checks ──────────────────────────────────────────────────────────
if ! command -v curl >/dev/null 2>&1; then
    echo "FATAL: curl not found on PATH" >&2; exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
    echo "FATAL: jq not found on PATH" >&2; exit 1
fi

# ─── Defaults ────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-https://staging-play.sunkr.bet}"
ADMIN_BASE_URL="${ADMIN_BASE_URL:-https://staging-admin.sunkr.bet}"
PLAYER_USER="${PLAYER_USER:-zuestang2}"
PLAYER_PASS="${PLAYER_PASS:-4581777b67685c53166793900e05f575}"
ADMIN_USER="${ADMIN_USER:-superadmin}"
ADMIN_PASS="${ADMIN_PASS:-0192023a7bbd73250516f069df18b500}"
TIMEOUT="${TIMEOUT:-10}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/minigame-smoke-output}"

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# ─── Test counters ────────────────────────────────────────────────────────────
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
CURRENT_TEST=""

# ─── Token storage ────────────────────────────────────────────────────────────
# Player access token (set via login_player or ensure_player_token)
PLAYER_AT=""
# Admin access token (set via login_admin or ensure_admin_token)
ADMIN_AT=""

# ─────────────────────────────────────────────────────────────────────────────
# login_player <username> <password_md5>
#   Calls the existing portal login endpoint (c=3) and returns the raw JSON.
#   Exits 1 on curl failure.
# ─────────────────────────────────────────────────────────────────────────────
login_player() {
    local user="$1"
    local pass="$2"
    curl -sf --max-time "$TIMEOUT" \
        "${BASE_URL}/api?c=3&un=${user}&pw=${pass}" 2>&1 || {
        echo "" ; return 1
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# login_admin <username> <password_md5>
#   Calls the admin login endpoint (c=701) and returns the raw JSON.
# ─────────────────────────────────────────────────────────────────────────────
login_admin() {
    local user="$1"
    local pass="$2"
    curl -sf --max-time "$TIMEOUT" \
        "${ADMIN_BASE_URL}/api_backend?c=701&un=${user}&pw=${pass}" 2>&1 || {
        echo "" ; return 1
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# ensure_player_token
#   Lazily acquires a player access token into $PLAYER_AT.
#   Exits the script on failure.
# ─────────────────────────────────────────────────────────────────────────────
ensure_player_token() {
    [[ -n "$PLAYER_AT" ]] && return 0
    local resp
    resp=$(login_player "$PLAYER_USER" "$PLAYER_PASS")
    if [[ -z "$resp" ]]; then
        echo -e "${RED}FATAL: player login curl failed${RESET}" >&2; exit 1
    fi
    PLAYER_AT=$(echo "$resp" | jq -r '.accessToken // empty' 2>/dev/null)
    if [[ -z "$PLAYER_AT" ]]; then
        echo -e "${RED}FATAL: player login returned no accessToken — resp: ${resp:0:200}${RESET}" >&2
        exit 1
    fi
    export PLAYER_AT
}

# ─────────────────────────────────────────────────────────────────────────────
# ensure_admin_token
#   Lazily acquires an admin access token into $ADMIN_AT.
#   Exits the script on failure.
# ─────────────────────────────────────────────────────────────────────────────
ensure_admin_token() {
    [[ -n "$ADMIN_AT" ]] && return 0
    local resp
    resp=$(login_admin "$ADMIN_USER" "$ADMIN_PASS")
    if [[ -z "$resp" ]]; then
        echo -e "${RED}FATAL: admin login curl failed${RESET}" >&2; exit 1
    fi
    ADMIN_AT=$(echo "$resp" | jq -r '.accessToken // empty' 2>/dev/null)
    if [[ -z "$ADMIN_AT" ]]; then
        echo -e "${RED}FATAL: admin login returned no accessToken — resp: ${resp:0:200}${RESET}" >&2
        exit 1
    fi
    export ADMIN_AT
}

# ─────────────────────────────────────────────────────────────────────────────
# minigame_get <path> [extra_curl_args...]
#   GET ${BASE_URL}<path> with bearer token. Returns raw response body.
#   Falls back to {"success":false,"errorCode":"CURL_ERROR"} on curl failure.
# ─────────────────────────────────────────────────────────────────────────────
minigame_get() {
    ensure_player_token
    local path="$1"; shift
    curl -sf --max-time "$TIMEOUT" \
        -H "Authorization: Bearer ${PLAYER_AT}" \
        "${BASE_URL}${path}" "$@" 2>&1 \
        || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ─────────────────────────────────────────────────────────────────────────────
# minigame_post <path> <json_body> [extra_curl_args...]
#   POST ${BASE_URL}<path> with bearer token + JSON body.
# ─────────────────────────────────────────────────────────────────────────────
minigame_post() {
    ensure_player_token
    local path="$1"
    local body="$2"; shift 2
    curl -sf --max-time "$TIMEOUT" \
        -X POST \
        -H "Authorization: Bearer ${PLAYER_AT}" \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${BASE_URL}${path}" "$@" 2>&1 \
        || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ─────────────────────────────────────────────────────────────────────────────
# admin_minigame_post <path> <json_body> [extra_curl_args...]
#   POST ${ADMIN_BASE_URL}<path> with admin bearer token + JSON body.
# ─────────────────────────────────────────────────────────────────────────────
admin_minigame_post() {
    ensure_admin_token
    local path="$1"
    local body="$2"; shift 2
    curl -sf --max-time "$TIMEOUT" \
        -X POST \
        -H "Authorization: Bearer ${ADMIN_AT}" \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${ADMIN_BASE_URL}${path}" "$@" 2>&1 \
        || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ─────────────────────────────────────────────────────────────────────────────
# minigame_post_no_auth <path> <json_body>
#   POST without any Authorization header — used for 0401 tests.
# ─────────────────────────────────────────────────────────────────────────────
minigame_post_no_auth() {
    local path="$1"
    local body="$2"
    curl -sf --max-time "$TIMEOUT" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${BASE_URL}${path}" 2>&1 \
        || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ─────────────────────────────────────────────────────────────────────────────
# player_minigame_post_as_admin_path <path> <json_body>
#   POST to the admin path but with a PLAYER token — used for 0403 tests.
# ─────────────────────────────────────────────────────────────────────────────
player_post_to_admin_path() {
    ensure_player_token
    local path="$1"
    local body="$2"
    curl -sf --max-time "$TIMEOUT" \
        -X POST \
        -H "Authorization: Bearer ${PLAYER_AT}" \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${ADMIN_BASE_URL}${path}" 2>&1 \
        || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ─────────────────────────────────────────────────────────────────────────────
# Assertion helpers
# ─────────────────────────────────────────────────────────────────────────────

test_name() {
    CURRENT_TEST="$1"
    echo -e "\n${CYAN}TEST:${RESET} ${BOLD}${CURRENT_TEST}${RESET}"
}

section() {
    echo ""
    echo -e "${BOLD}${YELLOW}=== $1 ===${RESET}"
}

_pass() {
    echo -e "  ${GREEN}PASS${RESET} — $1"
    PASS_COUNT=$((PASS_COUNT + 1))
}

_fail() {
    echo -e "  ${RED}FAIL${RESET} — $1"
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

_skip() {
    echo -e "  ${YELLOW}SKIP${RESET} [PENDING] — $1"
    SKIP_COUNT=$((SKIP_COUNT + 1))
}

# pretty_print <response>
#   Pipes the response through jq for readable output (debug aid).
pretty_print() {
    echo "$1" | jq . 2>/dev/null || echo "$1"
}

# assert_status_200 <response>
#   Passes if the response JSON has success:true OR errorCode "0000"/"0".
assert_status_200() {
    local resp="$1"
    local ok
    ok=$(echo "$resp" | jq -r '
        if .success == true then "true"
        elif .errorCode == "0000" then "true"
        elif .errorCode == "0" then "true"
        else "false"
        end' 2>/dev/null || echo "false")
    if [[ "$ok" == "true" ]]; then
        _pass "HTTP 200 / success — ${CURRENT_TEST}"
    else
        local ec
        ec=$(echo "$resp" | jq -r '.errorCode // "?"' 2>/dev/null || echo "?")
        _fail "expected success, got errorCode=${ec} — resp: ${resp:0:300}"
    fi
}

# assert_status_4xx <response> [expected_error_code]
#   Passes if success==false (any error). If expected_error_code given,
#   also checks the errorCode field matches exactly.
assert_status_4xx() {
    local resp="$1"
    local expected_code="${2:-}"
    local success
    success=$(echo "$resp" | jq -r '.success // "null"' 2>/dev/null || echo "null")
    local actual_code
    actual_code=$(echo "$resp" | jq -r '.errorCode // ""' 2>/dev/null || echo "")

    if [[ "$success" == "false" || "$success" == "null" ]]; then
        if [[ -n "$expected_code" && "$actual_code" != "$expected_code" ]]; then
            _fail "expected errorCode=${expected_code}, got errorCode=${actual_code} — resp: ${resp:0:300}"
        else
            local code_info=""
            [[ -n "$actual_code" ]] && code_info=" (errorCode=${actual_code})"
            _pass "rejected as expected${code_info} — ${CURRENT_TEST}"
        fi
    else
        _fail "expected rejection but got success — resp: ${resp:0:300}"
    fi
}

# assert_json_field <response> <jq_path> <expected_value>
#   Evaluates the jq_path against the response and compares to expected_value.
#   jq_path example: ".errorCode"   ".data.referenceId"
assert_json_field() {
    local resp="$1"
    local jq_path="$2"
    local expected="$3"
    local actual
    actual=$(echo "$resp" | jq -r "${jq_path} // \"__missing__\"" 2>/dev/null || echo "__jq_error__")
    if [[ "$actual" == "$expected" ]]; then
        _pass "field ${jq_path}=${expected} — ${CURRENT_TEST}"
    else
        _fail "field ${jq_path}: expected '${expected}', got '${actual}' — resp: ${resp:0:300}"
    fi
}

# assert_json_field_not_null <response> <jq_path>
#   Passes if the field is present and not null/empty.
assert_json_field_not_null() {
    local resp="$1"
    local jq_path="$2"
    local actual
    actual=$(echo "$resp" | jq -r "${jq_path} // \"__null__\"" 2>/dev/null || echo "__jq_error__")
    if [[ "$actual" != "__null__" && "$actual" != "__jq_error__" && -n "$actual" ]]; then
        _pass "field ${jq_path} is present (value=${actual:0:50}) — ${CURRENT_TEST}"
    else
        _fail "field ${jq_path} is null/missing — resp: ${resp:0:300}"
    fi
}

# assert_json_field_lt <response> <jq_path> <threshold>
#   Passes if the numeric field value < threshold.
assert_json_field_lt() {
    local resp="$1"
    local jq_path="$2"
    local threshold="$3"
    local actual
    actual=$(echo "$resp" | jq -r "${jq_path} // \"__null__\"" 2>/dev/null || echo "__null__")
    if [[ "$actual" == "__null__" ]]; then
        _fail "field ${jq_path} missing — ${CURRENT_TEST}"
        return
    fi
    if (( $(echo "$actual < $threshold" | bc -l 2>/dev/null || echo 0) )); then
        _pass "field ${jq_path}=${actual} < ${threshold} — ${CURRENT_TEST}"
    else
        _fail "field ${jq_path}=${actual} not < ${threshold} — ${CURRENT_TEST}"
    fi
}

# assert_json_array_not_empty <response> <jq_path>
#   Passes if the field at jq_path is a non-empty JSON array.
assert_json_array_not_empty() {
    local resp="$1"
    local jq_path="$2"
    local len
    len=$(echo "$resp" | jq "${jq_path} | length" 2>/dev/null || echo "0")
    if [[ "$len" =~ ^[0-9]+$ ]] && (( len > 0 )); then
        _pass "field ${jq_path} is array with ${len} items — ${CURRENT_TEST}"
    else
        _fail "field ${jq_path} is empty or not an array (len=${len}) — resp: ${resp:0:300}"
    fi
}

# assert_state_dto <response>
#   Asserts all required StateDto fields are present per §5.2 of the TaiXiu extraction plan.
assert_state_dto() {
    local resp="$1"
    local missing=""
    for field in referenceId remainTime bettingState potTai potXiu jpTai jpXiu dice1 dice2 dice3 result; do
        local val
        val=$(echo "$resp" | jq -r ".${field} // \"__missing__\"" 2>/dev/null || echo "__missing__")
        [[ "$val" == "__missing__" ]] && missing="${missing} ${field}"
    done
    if [[ -z "$missing" ]]; then
        _pass "StateDto has all required fields — ${CURRENT_TEST}"
    else
        _fail "StateDto missing fields:${missing} — resp: ${resp:0:400}"
    fi
}

# assert_bet_response_dto <response>
#   Asserts all required BetResponseDto fields are present per §5.2.
assert_bet_response_dto() {
    local resp="$1"
    local missing=""
    for field in success errorCode currentMoney perBetTxId; do
        local val
        val=$(echo "$resp" | jq -r ".${field} // \"__missing__\"" 2>/dev/null || echo "__missing__")
        [[ "$val" == "__missing__" ]] && missing="${missing} ${field}"
    done
    if [[ -z "$missing" ]]; then
        _pass "BetResponseDto has all required fields — ${CURRENT_TEST}"
    else
        _fail "BetResponseDto missing fields:${missing} — resp: ${resp:0:400}"
    fi
}

# pending_skip <reason>
#   Marks a test as skipped with PENDING marker — endpoints not yet deployed.
pending_skip() {
    local reason="$1"
    _skip "PENDING — endpoint not yet deployed: ${reason}"
}

# ─────────────────────────────────────────────────────────────────────────────
# print_summary
#   Prints final PASS/FAIL/SKIP counts and exits non-zero if any failures.
# ─────────────────────────────────────────────────────────────────────────────
print_summary() {
    local total=$((PASS_COUNT + FAIL_COUNT + SKIP_COUNT))
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    echo -e "${BOLD}SUMMARY${RESET}"
    echo -e "  Total : $total"
    echo -e "  ${GREEN}Pass  : $PASS_COUNT${RESET}"
    echo -e "  ${RED}Fail  : $FAIL_COUNT${RESET}"
    echo -e "  ${YELLOW}Skip  : $SKIP_COUNT${RESET}"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    if [[ $FAIL_COUNT -gt 0 ]]; then
        exit 1
    fi
    exit 0
}

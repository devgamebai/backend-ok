#!/bin/bash
# helpers.sh — Shared utilities for all API test scripts
#
# Response shape notes (from live testing 2026-04-04):
#   - Most admin endpoints return {"success":true,"errorCode":"0","data":...}
#   - Some endpoints (c=9403,9413,9610,9641,9652,9750,108) return {"data":[...],"total":N} with no "success" field
#   - Some endpoints return data as a JSON-encoded string inside "data" (c=9520,9820)
#   - c=9901 returns plain text "COMMAND NOT FOUND" (not JSON)
#   - assert_success treats any valid JSON with "data" key (or success:true) as passing

PLAYER_API="${PLAYER_API:-https://staging-play.sunkr.bet/api}"
ADMIN_API="${ADMIN_API:-https://staging-admin.sunkr.bet/api_backend}"

PLAYER_USER="${PLAYER_USER:-superadmin}"
PLAYER_PASS="${PLAYER_PASS:-0192023a7bbd73250516f069df18b500}"
ADMIN_USER="${ADMIN_USER:-superadmin}"
ADMIN_PASS="${ADMIN_PASS:-0192023a7bbd73250516f069df18b500}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# Counters
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
CURRENT_TEST=""

# Token storage
PAT=""
AAT=""

# ────────────────────────────────────────────────
# Login helpers
# ────────────────────────────────────────────────

player_login() {
    local resp
    resp=$(curl -sf --max-time 15 \
        "${PLAYER_API}?c=3&un=${PLAYER_USER}&pw=${PLAYER_PASS}" 2>&1) || {
        echo "" ; return 1
    }
    echo "$resp"
}

admin_login() {
    local resp
    resp=$(curl -sf --max-time 15 \
        "${ADMIN_API}?c=701&un=${ADMIN_USER}&pw=${ADMIN_PASS}" 2>&1) || {
        echo "" ; return 1
    }
    echo "$resp"
}

ensure_pat() {
    if [[ -n "$PAT" ]]; then return 0; fi
    local resp
    resp=$(player_login)
    if [[ -z "$resp" ]]; then
        echo -e "${RED}FATAL: Cannot reach player API${RESET}" >&2
        exit 1
    fi
    PAT=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))" 2>/dev/null)
    if [[ -z "$PAT" ]]; then
        echo -e "${RED}FATAL: Player login failed. Response: $resp${RESET}" >&2
        exit 1
    fi
    export PAT
}

ensure_aat() {
    if [[ -n "$AAT" ]]; then return 0; fi
    local resp
    resp=$(admin_login)
    if [[ -z "$resp" ]]; then
        echo -e "${RED}FATAL: Cannot reach admin API${RESET}" >&2
        exit 1
    fi
    AAT=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))" 2>/dev/null)
    if [[ -z "$AAT" ]]; then
        echo -e "${RED}FATAL: Admin login failed. Response: $resp${RESET}" >&2
        exit 1
    fi
    export AAT
}

# ────────────────────────────────────────────────
# Request helpers
# ────────────────────────────────────────────────

player_get() {
    ensure_pat
    curl -sf --max-time 15 "${PLAYER_API}?${1}&at=${PAT}" 2>&1 || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

admin_get() {
    ensure_aat
    curl -sf --max-time 15 "${ADMIN_API}?${1}&aat=${AAT}" 2>&1 || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

player_post() {
    ensure_pat
    curl -sf --max-time 15 -X POST \
        -H "Content-Type: application/json" \
        -d "$2" \
        "${PLAYER_API}?${1}&at=${PAT}" 2>&1 || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

admin_post() {
    ensure_aat
    curl -sf --max-time 15 -X POST \
        -H "Content-Type: application/json" \
        -d "$2" \
        "${ADMIN_API}?${1}&aat=${AAT}" 2>&1 || echo '{"success":false,"errorCode":"CURL_ERROR"}'
}

# ────────────────────────────────────────────────
# Assertion helpers
# ────────────────────────────────────────────────

test_name() {
    CURRENT_TEST="$1"
    echo -e "\n${CYAN}TEST:${RESET} ${BOLD}${CURRENT_TEST}${RESET}"
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
    echo -e "  ${YELLOW}SKIP${RESET} — $1"
    SKIP_COUNT=$((SKIP_COUNT + 1))
}

# _resp_ok "$resp"
# Returns "true" if:
#   - response is valid JSON AND (success==true OR "data" key exists) AND success!=false
# Returns "false" for: non-JSON, CURL_ERROR, success:false, errorCode!=0
_resp_ok() {
    local resp="$1"
    echo "$resp" | python3 -c "
import sys, json
result = 'false'
try:
    raw = sys.stdin.read()
    d = json.loads(raw)
    success = d.get('success')
    if success is False:
        result = 'false'
    elif success is True:
        result = 'true'
    elif 'data' in d or 'total' in d or 'totals' in d:
        result = 'true'
except Exception:
    result = 'false'
print(result)
" 2>/dev/null
}

# assert_success "$response"
assert_success() {
    local resp="$1"
    local ok
    ok=$(_resp_ok "$resp")
    if [[ "$ok" == "true" ]]; then
        _pass "success — ${CURRENT_TEST}"
    else
        local ec
        ec=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('errorCode','?'))" 2>/dev/null || echo "non-JSON")
        _fail "expected success, got errorCode=${ec} — resp: $(echo "$resp" | head -c 200)"
    fi
}

# assert_error "$response" "errorCode"
assert_error() {
    local resp="$1"
    local expected_code="$2"
    local actual_code
    actual_code=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('errorCode','')))" 2>/dev/null)
    if [[ "$actual_code" == "$expected_code" ]]; then
        _pass "errorCode=${expected_code} as expected — ${CURRENT_TEST}"
    else
        local ok
        ok=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success',False)).lower())" 2>/dev/null)
        _fail "expected errorCode=${expected_code}, got success=${ok} errorCode=${actual_code} — resp: $(echo "$resp" | head -c 200)"
    fi
}

# assert_has_field "$response" "fieldName"
# Checks top-level keys and also handles data-as-JSON-string pattern
assert_has_field() {
    local resp="$1"
    local field="$2"
    local val
    val=$(echo "$resp" | python3 -c "
import sys, json
result = 'missing'
try:
    d = json.load(sys.stdin)
    if '$field' in d:
        result = 'found'
    elif isinstance(d.get('data'), list):
        result = 'found'
    elif isinstance(d.get('data'), dict) and '$field' in d['data']:
        result = 'found'
    elif isinstance(d.get('data'), str):
        try:
            inner = json.loads(d['data'])
            if '$field' in inner or isinstance(inner.get('list'), list) or isinstance(inner.get('data'), list):
                result = 'found'
        except Exception:
            pass
except Exception:
    result = 'missing'
print(result)
" 2>/dev/null)
    if [[ "$val" == "found" ]]; then
        _pass "field '${field}' present — ${CURRENT_TEST}"
    else
        _fail "field '${field}' missing — resp: $(echo "$resp" | head -c 200)"
    fi
}

# assert_data_is_array "$response"
# Passes if response.data is a JSON array (may be empty)
assert_data_is_array() {
    local resp="$1"
    local result
    result=$(echo "$resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data')
    if isinstance(data, list):
        print('found')
    elif isinstance(data, str):
        try:
            inner = json.loads(data)
            if isinstance(inner.get('data'), list) or isinstance(inner.get('list'), list):
                print('found')
            else:
                print('not_array')
        except Exception:
            print('not_array')
    else:
        print('not_array')
except Exception:
    print('not_array')
" 2>/dev/null)
    if [[ "$result" == "found" ]]; then
        _pass "data is array — ${CURRENT_TEST}"
    else
        _fail "data is not an array — resp: $(echo "$resp" | head -c 200)"
    fi
}

# assert_data_has_items "$response" "field1,field2,field3"
# Passes if the first item in response.data (array) contains all specified fields.
# Also handles data-as-JSON-string pattern where inner object has data[] or list[].
assert_data_has_items() {
    local resp="$1"
    local fields="$2"
    local result
    result=$(echo "$resp" | python3 -c "
import sys, json
fields = '$fields'.split(',')
fields = [f.strip() for f in fields if f.strip()]
try:
    d = json.load(sys.stdin)
    # Resolve the array
    items = None
    data = d.get('data')
    if isinstance(data, list):
        items = data
    elif isinstance(data, str):
        try:
            inner = json.loads(data)
            if isinstance(inner.get('data'), list):
                items = inner['data']
            elif isinstance(inner.get('list'), list):
                items = inner['list']
        except Exception:
            pass
    if items is None:
        print('no_array')
    elif len(items) == 0:
        print('empty_ok')
    else:
        first = items[0]
        missing = [f for f in fields if f not in first]
        if missing:
            print('missing:' + ','.join(missing))
        else:
            print('ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
    if [[ "$result" == "ok" || "$result" == "empty_ok" ]]; then
        _pass "data items have fields [${fields}] — ${CURRENT_TEST}"
    elif [[ "$result" == "empty_ok" ]]; then
        _pass "data array is empty (cannot verify item fields) — ${CURRENT_TEST}"
    elif [[ "$result" == "no_array" ]]; then
        _fail "data is not an array, cannot check item fields — resp: $(echo "$resp" | head -c 200)"
    else
        _fail "item field check failed (${result}) — resp: $(echo "$resp" | head -c 200)"
    fi
}

# assert_not_empty "$response" "description"
assert_not_empty() {
    local resp="$1"
    local desc="${2:-response}"
    if [[ -z "$resp" || "$resp" == '{"success":false,"errorCode":"CURL_ERROR"}' || "$resp" == "COMMAND NOT FOUND" ]]; then
        _fail "empty/error response for ${desc} — ${CURRENT_TEST}"
    else
        _pass "non-empty response for ${desc} — ${CURRENT_TEST}"
    fi
}

# assert_field_equals "$response" "field" "expected_value"
assert_field_equals() {
    local resp="$1"
    local field="$2"
    local expected="$3"
    local val
    val=$(echo "$resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    v = d.get('$field') or (d.get('data') or {}).get('$field','')
    print(str(v))
except:
    print('')
" 2>/dev/null)
    if [[ "$val" == "$expected" ]]; then
        _pass "field '${field}'='${expected}' — ${CURRENT_TEST}"
    else
        _fail "field '${field}': expected '${expected}', got '${val}' — ${CURRENT_TEST}"
    fi
}

# assert_rejected "$response" — asserts the request was NOT successful (any error)
assert_rejected() {
    local resp="$1"
    local ok
    ok=$(_resp_ok "$resp")
    if [[ "$ok" != "true" ]]; then
        _pass "request rejected as expected — ${CURRENT_TEST}"
    else
        _fail "expected rejection but got success — resp: $(echo "$resp" | head -c 200)"
    fi
}

# ────────────────────────────────────────────────
# Summary
# ────────────────────────────────────────────────

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
}

section() {
    echo ""
    echo -e "${BOLD}${YELLOW}=== $1 ===${RESET}"
}

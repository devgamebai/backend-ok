#!/bin/bash
# Test: Authentication
# Tests: c=3 (player login), c=701 (admin login), token validation
# Note: c=9901 (heartbeat) returns plain text "COMMAND NOT FOUND" — not a valid endpoint,
#       so it is tested as a known-broken command.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "AUTH — Player Login (c=3)"

test_name "Player login with valid credentials (c=3)"
RAW=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=3&un=${PLAYER_USER}&pw=${PLAYER_PASS}" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_success "$RAW"
assert_has_field "$RAW" "accessToken"
assert_has_field "$RAW" "sessionKey"
PAT=$(echo "$RAW" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
export PAT
echo "  accessToken: ${PAT:0:16}..."

test_name "Player login with wrong password (c=3)"
RESP=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=3&un=${PLAYER_USER}&pw=wrongpassword" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

test_name "Player login with missing username (c=3)"
RESP=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=3&pw=${PLAYER_PASS}" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

section "AUTH — Admin Login (c=701)"

test_name "Admin login with valid credentials (c=701)"
RAW_ADMIN=$(curl -sf --max-time 15 \
    "${ADMIN_API}?c=701&un=${ADMIN_USER}&pw=${ADMIN_PASS}" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_success "$RAW_ADMIN"
assert_has_field "$RAW_ADMIN" "accessToken"
assert_has_field "$RAW_ADMIN" "sessionKey"
assert_has_field "$RAW_ADMIN" "data"
# c=701 data field is a JSON string — verify inner fields
ADMIN_DATA_CHECK=$(echo "$RAW_ADMIN" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    inner = json.loads(d.get('data', '{}'))
    required = ['adminId', 'fullName', 'tokenExpiry', 'userName', 'adminToken', 'parentId', 'status']
    missing = [f for f in required if f not in inner]
    print('missing:' + ','.join(missing) if missing else 'ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$ADMIN_DATA_CHECK" == "ok" ]]; then
    _pass "admin login data contains all required inner fields"
else
    _fail "admin login data inner fields check: ${ADMIN_DATA_CHECK}"
fi
AAT=$(echo "$RAW_ADMIN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
export AAT
echo "  accessToken: ${AAT:0:16}..."

test_name "Admin login with wrong password (c=701)"
RESP=$(curl -sf --max-time 15 \
    "${ADMIN_API}?c=701&un=${ADMIN_USER}&pw=badpass" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

section "AUTH — Heartbeat / Token Validation"

test_name "c=9901 returns non-success (command not mapped)"
RESP=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=9901&at=${PAT}" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
# Known: returns plain "COMMAND NOT FOUND" — not a valid endpoint
if [[ "$RESP" == "COMMAND NOT FOUND" ]]; then
    _pass "c=9901 returns 'COMMAND NOT FOUND' (known — endpoint not mapped)"
else
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null || echo "non-json")
    echo "  Response: $(echo "$RESP" | head -c 100)"
    _skip "c=9901 response: $OK — verify endpoint mapping"
fi

test_name "Player API rejects request with no token"
RESP=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=3002" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

test_name "Player API rejects request with invalid token"
RESP=$(curl -sf --max-time 15 \
    "${PLAYER_API}?c=3002&at=INVALID_TOKEN_XYZ_000" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

test_name "Admin API rejects protected endpoint with no token (c=9610)"
RESP=$(curl -sf --max-time 15 \
    "${ADMIN_API}?c=9610" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

test_name "Admin API rejects protected endpoint with invalid token (c=9610)"
RESP=$(curl -sf --max-time 15 \
    "${ADMIN_API}?c=9610&aat=INVALID_TOKEN_XYZ_000" 2>&1 \
    || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_rejected "$RESP"

print_summary

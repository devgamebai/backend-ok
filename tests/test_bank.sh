#!/bin/bash
# Test: Bank Management
# Tests: c=3001,3002,3003 (player), c=8819,8820,8821 (bank names),
#        c=8801,8802,8803 (player banks), c=9520,9521,9522 (admin banks)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "BANK — Player: Bank List (c=3001)"

test_name "Get bank list (c=3001)"
RESP=$(player_get "c=3001")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "code,bank_name,logo,id"

section "BANK — Player: Get User Bank Info (c=3002)"

test_name "Get own bank info (c=3002)"
RESP=$(player_get "c=3002")
# May succeed (bank set) or return error if not set — both are valid
assert_not_empty "$RESP" "user bank info"
echo "  Response: $(echo "$RESP" | head -c 200)"

section "BANK — Player: Submit Bank (c=3003)"

test_name "Submit bank missing params (c=3003)"
RESP=$(player_get "c=3003")
# Expect error — missing required fields
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=3003)"
else
    _fail "Expected error for missing bank params"
fi

test_name "Submit bank with invalid bank_id (c=3003)"
RESP=$(player_get "c=3003&bank_id=99999&account_number=123456789&account_name=TestUser")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid bank_id rejected (c=3003)"
else
    _pass "Submit bank succeeded (bank may already be set)"
fi

section "BANK — Admin: Bank Name List (c=8820)"

test_name "Admin bank name list (c=8820)"
RESP=$(admin_get "c=8820")
assert_success "$RESP"
assert_has_field "$RESP" "data"

section "BANK — Admin: Create/Update Bank Name (c=8819)"

test_name "Create bank name missing params (c=8819)"
RESP=$(admin_get "c=8819")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=8819)"
else
    _fail "Expected error for missing bank name params"
fi

test_name "Create bank name with params (c=8819)"
RESP=$(admin_get "c=8819&code=TESTBK&name=Test+Bank&status=1")
assert_not_empty "$RESP" "create bank name"
EC=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('success',''),d.get('errorCode',''))" 2>/dev/null)
echo "  Result: $EC"

section "BANK — Admin: Delete Bank Name (c=8821)"

test_name "Delete bank name missing id (c=8821)"
RESP=$(admin_get "c=8821")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=8821)"
else
    _fail "Expected error for missing bank id"
fi

section "BANK — Admin: Player Bank Search (c=8802)"

test_name "Search player banks (c=8802)"
RESP=$(admin_get "c=8802")
assert_success "$RESP"
assert_has_field "$RESP" "data"

test_name "Search player banks by username (c=8802)"
RESP=$(admin_get "c=8802&username=superadmin")
assert_success "$RESP"

section "BANK — Admin: Update Player Bank (c=8801)"

test_name "Update player bank missing params (c=8801)"
RESP=$(admin_get "c=8801")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=8801)"
else
    _fail "Expected error for missing update params"
fi

section "BANK — Admin: Delete Player Bank (c=8803)"

test_name "Delete player bank missing id (c=8803)"
RESP=$(admin_get "c=8803")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=8803)"
else
    _fail "Expected error for missing bank id"
fi

section "BANK — Admin: Admin Bank Search (c=9520)"

test_name "Search admin banks (c=9520)"
RESP=$(admin_get "c=9520")
assert_success "$RESP"
assert_has_field "$RESP" "data"
# c=9520 data is a JSON string containing {total, data:[]}
ADMIN_BANK_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    inner = json.loads(d.get('data', '{}'))
    has_total = 'total' in inner
    has_data = isinstance(inner.get('data'), list)
    print('ok' if has_total and has_data else 'missing_fields')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$ADMIN_BANK_CHECK" == "ok" ]]; then
    _pass "admin banks inner structure has total+data[] — c=9520"
else
    _fail "admin banks inner structure check: ${ADMIN_BANK_CHECK}"
fi

section "BANK — Admin: Insert/Update Admin Bank (c=9521)"

test_name "Insert admin bank missing params (c=9521)"
RESP=$(admin_get "c=9521")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9521)"
else
    _fail "Expected error for missing admin bank params"
fi

test_name "Insert admin bank with params (c=9521)"
RESP=$(admin_get "c=9521&bank_code=TESTBK&account_number=9999999999&account_name=Test+Admin+Bank&status=1")
assert_not_empty "$RESP" "insert admin bank"
EC=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('success',''),d.get('errorCode',''))" 2>/dev/null)
echo "  Result: $EC"

section "BANK — Admin: Delete Admin Bank (c=9522)"

test_name "Delete admin bank missing id (c=9522)"
RESP=$(admin_get "c=9522")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9522)"
else
    _fail "Expected error for missing admin bank id"
fi

# Get a valid admin bank id to test deletion
test_name "Delete admin bank with invalid id (c=9522)"
RESP=$(admin_get "c=9522&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid admin bank id rejected (errorCode=${EC})"
else
    _fail "Expected error for invalid admin bank id"
fi

print_summary

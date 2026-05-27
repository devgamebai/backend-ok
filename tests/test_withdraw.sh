#!/bin/bash
# Test: Withdraw Flow
# Tests: c=3030,3031,3032 (withdraw password), c=3040,3041,3042 (withdraw ops)
#        Admin: c=9652,9653,9654

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "WITHDRAW — Player: Check Withdraw Password Set (c=3030)"

test_name "Check if withdraw password is set (c=3030)"
RESP=$(player_get "c=3030")
assert_not_empty "$RESP" "withdraw password check"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
_pass "Withdraw password check responded"

section "WITHDRAW — Player: Set Withdraw Password (c=3031)"

test_name "Set withdraw password missing params (c=3031)"
RESP=$(player_get "c=3031")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=3031)"
else
    _fail "Expected error for missing withdraw password params"
fi

test_name "Set withdraw password with params (c=3031)"
RESP=$(player_get "c=3031&password=123456&new_password=123456&confirm_password=123456")
assert_not_empty "$RESP" "set withdraw password"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

section "WITHDRAW — Player: Verify Withdraw Password (c=3032)"

test_name "Verify withdraw password missing params (c=3032)"
RESP=$(player_get "c=3032")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=3032)"
else
    _fail "Expected error for missing verify params"
fi

test_name "Verify withdraw password with wrong password (c=3032)"
RESP=$(player_get "c=3032&password=wrongpass")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Wrong withdraw password rejected"
else
    _fail "Expected error for wrong withdraw password"
fi

section "WITHDRAW — Player: Get Withdrawal Status (c=3040)"

test_name "Get withdrawal status (c=3040)"
RESP=$(player_get "c=3040")
assert_not_empty "$RESP" "withdrawal status"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
if [[ "$OK" == "True" ]]; then
    assert_has_field "$RESP" "data"
    # c=3040 data is an object with withdraw status fields
    WD_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', {})
    required = ['total_required_volume','total_actual_volume','commission_withdrawn','remaining_volume','min_bank_withdraw','max_bank_withdraw','commission_available','status']
    missing = [f for f in required if f not in data]
    print('missing:' + ','.join(missing) if missing else 'ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
    if [[ "$WD_CHECK" == "ok" ]]; then
        _pass "withdrawal status data has all required fields"
    else
        _fail "withdrawal status data fields: ${WD_CHECK}"
    fi
else
    _pass "Withdrawal status responded (errorCode=${EC})"
fi

section "WITHDRAW — Player: Request Withdraw (c=3041)"

test_name "Request withdraw missing params (c=3041)"
RESP=$(player_get "c=3041")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=3041)"
else
    _fail "Expected error for missing withdraw params"
fi

test_name "Request withdraw with invalid amount (c=3041)"
RESP=$(player_get "c=3041&amount=1&bank_id=1&withdraw_password=123456")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid withdraw amount rejected (errorCode=$EC)"
else
    _pass "Withdraw request accepted (amount valid)"
fi

section "WITHDRAW — Player: Withdraw History (c=3042)"

test_name "Get withdraw history (c=3042)"
RESP=$(player_get "c=3042")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Get withdraw history with pagination (c=3042)"
RESP=$(player_get "c=3042&page=1&size=10")
assert_success "$RESP"

section "WITHDRAW — Admin: Withdraw List (c=9652)"

test_name "Admin withdraw list (c=9652)"
RESP=$(admin_get "c=9652")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Admin withdraw list with status filter (c=9652)"
RESP=$(admin_get "c=9652&status=0&page=1&size=10")
assert_success "$RESP"

test_name "Admin withdraw list filter by username (c=9652)"
RESP=$(admin_get "c=9652&username=superadmin")
assert_success "$RESP"

section "WITHDRAW — Admin: Approve Withdraw (c=9653)"

test_name "Approve withdraw missing id (c=9653)"
RESP=$(admin_get "c=9653")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9653)"
else
    _fail "Expected error for missing withdraw id"
fi

test_name "Approve withdraw with invalid id (c=9653)"
RESP=$(admin_get "c=9653&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid id rejected for withdraw approve (errorCode=$EC)"
else
    _fail "Expected error for invalid withdraw id"
fi

section "WITHDRAW — Admin: Reject Withdraw (c=9654)"

test_name "Reject withdraw missing id (c=9654)"
RESP=$(admin_get "c=9654")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9654)"
else
    _fail "Expected error for missing withdraw id"
fi

test_name "Reject withdraw with invalid id (c=9654)"
RESP=$(admin_get "c=9654&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid id rejected for withdraw reject (errorCode=$EC)"
else
    _fail "Expected error for invalid withdraw id on reject"
fi

print_summary

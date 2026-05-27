#!/bin/bash
# Test: Deposit Flow
# Tests: c=3011 (create deposit), c=2011 (deposit history)
#        Admin: c=9610 (list), c=9611 (lock), c=9612 (approve), c=9613 (reject), c=9614 (release lock)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "DEPOSIT — Player: Create Deposit (c=3011)"

test_name "Create deposit missing params (c=3011)"
RESP=$(player_get "c=3011")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=3011)"
else
    _fail "Expected error for missing deposit params"
fi

test_name "Create deposit with valid params (c=3011)"
RESP=$(player_get "c=3011&amount=100000&bank_id=1&type=1")
assert_not_empty "$RESP" "create deposit"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
# Store deposit id if created
DEPOSIT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(d.get('data',{}).get('id','') if isinstance(d.get('data'),dict) else '')
" 2>/dev/null)
echo "  Deposit ID: ${DEPOSIT_ID:-none}"

section "DEPOSIT — Player: Deposit History (c=2011)"

test_name "Get deposit history (c=2011)"
RESP=$(player_get "c=2011")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Get deposit history with pagination (c=2011)"
RESP=$(player_get "c=2011&page=1&size=10")
assert_success "$RESP"

section "DEPOSIT — Admin: List Deposits (c=9610)"

test_name "Admin list deposits (c=9610)"
RESP=$(admin_get "c=9610")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Admin list deposits with pagination (c=9610)"
RESP=$(admin_get "c=9610&page=1&size=10")
assert_success "$RESP"

test_name "Admin list deposits filter by username (c=9610)"
RESP=$(admin_get "c=9610&username=superadmin")
assert_success "$RESP"

section "DEPOSIT — Admin: Lock Deposit (c=9611)"

test_name "Lock deposit missing id (c=9611)"
RESP=$(admin_get "c=9611")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9611)"
else
    _fail "Expected error for missing deposit id"
fi

test_name "Lock deposit with invalid id (c=9611)"
RESP=$(admin_get "c=9611&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid deposit id rejected (errorCode=${EC})"
else
    _fail "Expected error for invalid deposit id"
fi

section "DEPOSIT — Admin: Approve Deposit (c=9612)"

test_name "Approve deposit missing id (c=9612)"
RESP=$(admin_get "c=9612")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9612)"
else
    _fail "Expected error for missing deposit id"
fi

test_name "Approve deposit with invalid id (c=9612)"
RESP=$(admin_get "c=9612&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid deposit id rejected for approve (errorCode=${EC})"
else
    _fail "Expected error for invalid deposit id on approve"
fi

section "DEPOSIT — Admin: Reject Deposit (c=9613)"

test_name "Reject deposit missing id (c=9613)"
RESP=$(admin_get "c=9613")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9613)"
else
    _fail "Expected error for missing deposit id"
fi

test_name "Reject deposit with invalid id (c=9613)"
RESP=$(admin_get "c=9613&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid deposit id rejected for reject (errorCode=${EC})"
else
    _fail "Expected error for invalid deposit id on reject"
fi

section "DEPOSIT — Admin: Release Lock (c=9614)"

test_name "Release lock missing id (c=9614)"
RESP=$(admin_get "c=9614")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9614)"
else
    _fail "Expected error for missing deposit id"
fi

test_name "Release lock with invalid id (c=9614)"
RESP=$(admin_get "c=9614&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid deposit id rejected for release lock (errorCode=${EC})"
else
    _fail "Expected error for invalid deposit id on release lock"
fi

print_summary

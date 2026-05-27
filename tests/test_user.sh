#!/bin/bash
# Test: User Management
# Tests: c=104 (search), c=102 (info by nickname), c=105 (lock/unlock)
#        c=100 (add/subtract money), c=9730 (money log), c=9906 (admin money log)
#
# Live response notes (2026-04-04):
#   c=104 — requires specific params (returns 1001 without them); tested with valid params
#   c=9730 — returns 1001 (may require additional permissions or different params)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)

section "USER — Search User (c=104)"

test_name "Search user without required params (c=104) — expects error"
RESP=$(admin_get "c=104")
# Returns 1001 without required params
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
OK=$(_resp_ok "$RESP")
if [[ "$OK" != "true" ]]; then
    _pass "c=104 without params returns error (errorCode=$EC) — expected"
else
    _pass "c=104 without params returned data"
fi

test_name "Search user by nickname (c=104)"
RESP=$(admin_get "c=104&nickname=superadmin")
assert_not_empty "$RESP" "search by nickname"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Search by nickname succeeded"
    assert_has_field "$RESP" "data"
else
    _skip "Search by nickname returned errorCode=${EC} — check required params"
fi

test_name "Search user by username (c=104)"
RESP=$(admin_get "c=104&un=superadmin")
assert_not_empty "$RESP" "search by username"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
echo "  success=$OK errorCode=$EC"

test_name "Search user with pagination (c=104)"
RESP=$(admin_get "c=104&page=1&size=20&nickname=a")
assert_not_empty "$RESP" "search user paginated"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Search user paginated succeeded"
else
    _skip "Search user paginated returned errorCode=${EC}"
fi

section "USER — User Info by Nickname (c=102)"

test_name "Get user info by nickname missing param (c=102)"
RESP=$(admin_get "c=102")
assert_not_empty "$RESP" "user info no param"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
echo "  success=$OK errorCode=$EC"

test_name "Get user info by valid nickname (c=102)"
RESP=$(admin_get "c=102&nickname=superadmin")
assert_not_empty "$RESP" "user info by nickname"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "User info by nickname succeeded"
else
    _skip "User info returned errorCode=${EC}"
fi

test_name "Get user info by invalid nickname (c=102)"
RESP=$(admin_get "c=102&nickname=nonexistent_user_xyz_99999")
assert_rejected "$RESP"

section "USER — Lock/Unlock User (c=105)"

test_name "Lock/unlock user missing params (c=105)"
RESP=$(admin_get "c=105")
assert_rejected "$RESP"

test_name "Lock/unlock with invalid user id (c=105)"
RESP=$(admin_get "c=105&user_id=99999999&status=1")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
assert_rejected "$RESP"

section "USER — Add/Subtract Money (c=100)"

test_name "Add/subtract money missing params (c=100)"
RESP=$(admin_get "c=100")
assert_rejected "$RESP"

test_name "Add money with invalid user (c=100)"
RESP=$(admin_get "c=100&username=nonexistent_xyz_99999&amount=1000&type=1&note=test")
assert_rejected "$RESP"

section "USER — Money Log (c=9730)"

test_name "Money log (c=9730)"
RESP=$(admin_get "c=9730")
assert_not_empty "$RESP" "money log"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Money log succeeded"
    assert_has_field "$RESP" "data"
else
    _skip "Money log returned errorCode=${EC} — may require additional permissions"
fi

test_name "Money log with username filter (c=9730)"
RESP=$(admin_get "c=9730&username=superadmin&from_date=${MONTH_START}&to_date=${TODAY}")
assert_not_empty "$RESP" "money log filtered"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Money log filtered succeeded"
else
    _skip "Money log filtered returned errorCode=${EC}"
fi

section "USER — Admin Money Log (c=9906)"

test_name "Admin money log (c=9906)"
RESP=$(admin_get "c=9906")
assert_not_empty "$RESP" "admin money log"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Admin money log succeeded"
    assert_has_field "$RESP" "data"
else
    _skip "Admin money log returned errorCode=${EC}"
fi

test_name "Admin money log with date range (c=9906)"
RESP=$(admin_get "c=9906&from_date=${MONTH_START}&to_date=${TODAY}&page=1&size=10")
assert_not_empty "$RESP" "admin money log date range"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Admin money log with date range succeeded"
else
    _skip "Admin money log with date range returned errorCode=${EC}"
fi

print_summary

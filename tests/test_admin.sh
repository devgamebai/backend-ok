#!/bin/bash
# Test: Admin Account Management
# Tests: c=9820,9821,9822,9713 (admin CRUD), c=9720,9721 (groups)
#        c=9722,9723,9724 (menus), c=9731,9732 (admin logs)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "ADMIN — List Admins (c=9820)"

test_name "List admins (c=9820)"
RESP=$(admin_get "c=9820")
assert_success "$RESP"
assert_has_field "$RESP" "data"
# c=9820 data is a JSON string containing {total, list:[]}
ADMINS_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    inner = json.loads(d.get('data', '{}'))
    has_total = 'total' in inner
    has_list = isinstance(inner.get('list'), list)
    print('ok' if has_total and has_list else 'missing_fields')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$ADMINS_CHECK" == "ok" ]]; then
    _pass "admins inner structure has total+list[] — c=9820"
else
    _fail "admins inner structure check: ${ADMINS_CHECK}"
fi

test_name "List admins with pagination (c=9820)"
RESP=$(admin_get "c=9820&page=1&size=20")
assert_success "$RESP"

FIRST_ADMIN_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('admin_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('admins',[])))
    if items:
        print(items[0].get('id',items[0].get('admin_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First admin ID: ${FIRST_ADMIN_ID:-none}"

section "ADMIN — Admin Detail (c=9713)"

test_name "Admin detail missing id (c=9713)"
RESP=$(admin_get "c=9713")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9713)"
else
    _pass "c=9713 without id returned own detail"
fi

if [[ -n "${FIRST_ADMIN_ID:-}" ]]; then
    test_name "Admin detail with valid id (c=9713)"
    RESP=$(admin_get "c=9713&id=${FIRST_ADMIN_ID}")
    assert_success "$RESP"
    assert_has_field "$RESP" "data"
fi

section "ADMIN — Create Admin (c=9821)"

test_name "Create admin missing params (c=9821)"
RESP=$(admin_get "c=9821")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9821)"
else
    _fail "Expected error for missing admin params"
fi

test_name "Create admin with params (c=9821)"
TS=$(date +%s)
RESP=$(admin_get "c=9821&username=testadmin${TS}&password=Test%40123&name=TestAdmin${TS}&status=1")
assert_not_empty "$RESP" "create admin"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
NEW_ADMIN_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
v=d.get('data',{})
if isinstance(v,dict):
    print(v.get('id',v.get('admin_id','')))
else:
    print('')
" 2>/dev/null)
echo "  New admin ID: ${NEW_ADMIN_ID:-none}"

section "ADMIN — Update Admin (c=9822)"

test_name "Update admin missing id (c=9822)"
RESP=$(admin_get "c=9822")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9822)"
else
    _fail "Expected error for missing admin id"
fi

TARGET_ID="${NEW_ADMIN_ID:-${FIRST_ADMIN_ID:-}}"
if [[ -n "${TARGET_ID:-}" ]]; then
    test_name "Update admin with valid id (c=9822)"
    RESP=$(admin_get "c=9822&id=${TARGET_ID}&status=1")
    assert_not_empty "$RESP" "update admin"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "ADMIN — Groups (c=9720, c=9721)"

test_name "List admin groups (c=9720)"
RESP=$(admin_get "c=9720")
assert_success "$RESP"
assert_has_field "$RESP" "data"

GROUP_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('group_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('groups',[])))
    if items:
        print(items[0].get('id',items[0].get('group_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First group ID: ${GROUP_ID:-none}"

test_name "Create/update group missing params (c=9721)"
RESP=$(admin_get "c=9721")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9721)"
else
    _fail "Expected error for missing group params"
fi

test_name "Create admin group with params (c=9721)"
TS=$(date +%s)
RESP=$(admin_get "c=9721&name=TestGroup${TS}&status=1")
assert_not_empty "$RESP" "create group"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

section "ADMIN — Menus (c=9722, c=9723, c=9724)"

test_name "List menus (c=9722)"
RESP=$(admin_get "c=9722")
assert_success "$RESP"
assert_has_field "$RESP" "data"
# c=9722 data is a JSON string containing {list:[]}
MENUS_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    inner = json.loads(d.get('data', '{}'))
    has_list = isinstance(inner.get('list'), list)
    print('ok' if has_list else 'missing_list')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$MENUS_CHECK" == "ok" ]]; then
    _pass "menus inner structure has list[] — c=9722"
else
    _fail "menus inner structure check: ${MENUS_CHECK}"
fi

MENU_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('menu_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('menus',[])))
    if items:
        print(items[0].get('id',items[0].get('menu_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First menu ID: ${MENU_ID:-none}"

test_name "Create/update menu missing params (c=9723)"
RESP=$(admin_get "c=9723")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9723)"
else
    _fail "Expected error for missing menu params"
fi

test_name "Delete menu missing id (c=9724)"
RESP=$(admin_get "c=9724")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9724)"
else
    _fail "Expected error for missing menu id"
fi

test_name "Delete menu with invalid id (c=9724)"
RESP=$(admin_get "c=9724&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid menu id rejected (errorCode=$EC)"
else
    _fail "Expected error for invalid menu id"
fi

section "ADMIN — Admin Logs (c=9731, c=9732)"

test_name "Admin action logs (c=9731)"
RESP=$(admin_get "c=9731")
assert_success "$RESP"
assert_has_field "$RESP" "data"
# c=9731 data is a JSON string containing {total, list:[]}
LOGS_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    inner = json.loads(d.get('data', '{}'))
    has_total = 'total' in inner
    has_list = isinstance(inner.get('list'), list)
    print('ok' if has_total and has_list else 'missing_fields')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$LOGS_CHECK" == "ok" ]]; then
    _pass "admin logs inner structure has total+list[] — c=9731"
else
    _fail "admin logs inner structure check: ${LOGS_CHECK}"
fi

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)
test_name "Admin action logs with date filter (c=9731)"
RESP=$(admin_get "c=9731&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

test_name "Admin login logs (c=9732)"
RESP=$(admin_get "c=9732")
assert_success "$RESP"
assert_has_field "$RESP" "data"

test_name "Admin login logs with date filter (c=9732)"
RESP=$(admin_get "c=9732&from_date=${MONTH_START}&to_date=${TODAY}&page=1&size=20")
assert_success "$RESP"

print_summary

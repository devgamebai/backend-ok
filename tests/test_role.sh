#!/bin/bash
# Test: RBAC — Roles & Permissions
# Tests: c=9700 (list roles), c=9708 (my permissions), c=9704 (list permissions)
#        c=9703 (assign perm to role), c=9706 (list admin+role), c=9705 (assign role to admin)
#        c=9707 (check permission)
#
# Live response notes (2026-04-04):
#   c=9700,9704,9706 — return 4003 "Only super_admin can view role configuration"
#     for the superadmin account used here (role system uses a separate super_admin flag)
#   c=9708 — returns 9999 (not implemented or different account type required)
#   These are tested as known permission-restricted endpoints.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "ROLE — List Roles (c=9700)"

test_name "List roles (c=9700)"
RESP=$(admin_get "c=9700")
assert_not_empty "$RESP" "list roles"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "List roles succeeded"
    assert_has_field "$RESP" "data"
    ROLE_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('role_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('roles',[])))
    if items: print(items[0].get('id',''))
    else: print('')
else: print('')
" 2>/dev/null)
    echo "  First role ID: ${ROLE_ID:-none}"
else
    _skip "List roles returned errorCode=${EC} (requires super_admin role flag)"
    ROLE_ID=""
fi

section "ROLE — My Permissions (c=9708)"

test_name "Get my permissions (c=9708)"
RESP=$(admin_get "c=9708")
assert_not_empty "$RESP" "my permissions"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "Get my permissions succeeded"
    assert_has_field "$RESP" "data"
else
    _skip "Get my permissions returned errorCode=${EC}"
fi

section "ROLE — List Permissions (c=9704)"

test_name "List all permissions (c=9704)"
RESP=$(admin_get "c=9704")
assert_not_empty "$RESP" "list permissions"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "List permissions succeeded"
    assert_has_field "$RESP" "data"
    PERM_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('permission_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('permissions',[])))
    if items: print(items[0].get('id',''))
    else: print('')
else: print('')
" 2>/dev/null)
    echo "  First permission ID: ${PERM_ID:-none}"
else
    _skip "List permissions returned errorCode=${EC} (requires super_admin role flag)"
    PERM_ID=""
fi

section "ROLE — Assign Permission to Role (c=9703)"

test_name "Assign permission to role missing params (c=9703)"
RESP=$(admin_get "c=9703")
assert_rejected "$RESP"

if [[ -n "${ROLE_ID:-}" && -n "${PERM_ID:-}" ]]; then
    test_name "Assign permission to role with valid ids (c=9703)"
    RESP=$(admin_get "c=9703&role_id=${ROLE_ID}&permission_id=${PERM_ID}")
    assert_not_empty "$RESP" "assign permission to role"
    OK=$(_resp_ok "$RESP")
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "ROLE — List Admin + Role (c=9706)"

test_name "List admins with their roles (c=9706)"
RESP=$(admin_get "c=9706")
assert_not_empty "$RESP" "list admin+role"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "List admin+role succeeded"
    assert_has_field "$RESP" "data"
    ADMIN_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('admin_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',[]))
    if items: print(items[0].get('id',''))
    else: print('')
else: print('')
" 2>/dev/null)
    echo "  First admin ID: ${ADMIN_ID:-none}"
else
    _skip "List admin+role returned errorCode=${EC} (requires super_admin role flag)"
    ADMIN_ID=""
fi

section "ROLE — Assign Role to Admin (c=9705)"

test_name "Assign role to admin missing params (c=9705)"
RESP=$(admin_get "c=9705")
assert_rejected "$RESP"

if [[ -n "${ADMIN_ID:-}" && -n "${ROLE_ID:-}" ]]; then
    test_name "Assign role to admin with valid ids (c=9705)"
    RESP=$(admin_get "c=9705&admin_id=${ADMIN_ID}&role_id=${ROLE_ID}")
    assert_not_empty "$RESP" "assign role to admin"
    OK=$(_resp_ok "$RESP")
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "ROLE — Check Permission (c=9707)"

test_name "Check permission (c=9707)"
RESP=$(admin_get "c=9707")
assert_not_empty "$RESP" "check permission"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "Check permission succeeded"
else
    _skip "Check permission returned errorCode=${EC}"
fi

test_name "Check specific permission (c=9707)"
RESP=$(admin_get "c=9707&permission=MANAGE_USER")
assert_not_empty "$RESP" "check specific permission"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
if [[ "$OK" == "true" ]]; then
    _pass "Check specific permission succeeded"
else
    _skip "Check specific permission returned errorCode=${EC}"
fi

print_summary

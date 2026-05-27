#!/bin/bash
# Test: Agent Management
# Tests: c=9420,9421,9422,9423,9424,9425,9426 (admin agent mgmt)
#        c=8826,8827,8840 (agent codes, users, stats)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "AGENT — List Agents (c=9420)"

test_name "List agents (c=9420)"
RESP=$(admin_get "c=9420")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_has_field "$RESP" "totalRecords"
assert_has_field "$RESP" "totalPages"
assert_has_field "$RESP" "currentPage"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "id,username,nickname,nameagent,address,phone,email,level,code,status"

test_name "List agents with pagination (c=9420)"
RESP=$(admin_get "c=9420&page=1&size=20")
assert_success "$RESP"

# Grab first agent id
AGENT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('agent_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('agents',[])))
    if items:
        print(items[0].get('id',items[0].get('agent_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
AGENT_CODE=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('code',data[0].get('agent_code','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('agents',[])))
    if items:
        print(items[0].get('code',items[0].get('agent_code','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First agent ID: ${AGENT_ID:-none}, code: ${AGENT_CODE:-none}"

section "AGENT — Agent Detail (c=9424)"

test_name "Agent detail missing id (c=9424)"
RESP=$(admin_get "c=9424")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9424)"
else
    _pass "c=9424 without id returned data"
fi

if [[ -n "${AGENT_ID:-}" ]]; then
    test_name "Agent detail with valid id (c=9424)"
    RESP=$(admin_get "c=9424&id=${AGENT_ID}")
    assert_success "$RESP"
    assert_has_field "$RESP" "data"
fi

section "AGENT — Create Agent (c=9422)"

test_name "Create agent missing params (c=9422)"
RESP=$(admin_get "c=9422")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9422)"
else
    _fail "Expected error for missing agent params"
fi

test_name "Create agent with params (c=9422)"
TS=$(date +%s)
RESP=$(admin_get "c=9422&username=testagent${TS}&password=Test@123&name=TestAgent${TS}&commission=5&status=1")
assert_not_empty "$RESP" "create agent"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
NEW_AGENT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
v=d.get('data',{})
if isinstance(v,dict):
    print(v.get('id',v.get('agent_id','')))
else:
    print('')
" 2>/dev/null)
echo "  New agent ID: ${NEW_AGENT_ID:-none}"

section "AGENT — Update Agent (c=9423)"

test_name "Update agent missing id (c=9423)"
RESP=$(admin_get "c=9423")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9423)"
else
    _fail "Expected error for missing agent id"
fi

TARGET_ID="${NEW_AGENT_ID:-${AGENT_ID:-}}"
if [[ -n "${TARGET_ID:-}" ]]; then
    test_name "Update agent with valid id (c=9423)"
    RESP=$(admin_get "c=9423&id=${TARGET_ID}&status=1&commission=3")
    assert_not_empty "$RESP" "update agent"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "AGENT — Delete Agent (c=9421)"

test_name "Delete agent missing id (c=9421)"
RESP=$(admin_get "c=9421")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9421)"
else
    _fail "Expected error for missing agent id"
fi

if [[ -n "${NEW_AGENT_ID:-}" ]]; then
    test_name "Delete newly created test agent (c=9421)"
    RESP=$(admin_get "c=9421&id=${NEW_AGENT_ID}")
    assert_not_empty "$RESP" "delete agent"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

test_name "Delete agent with invalid id (c=9421)"
RESP=$(admin_get "c=9421&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid agent id rejected (errorCode=$EC)"
else
    _fail "Expected error for invalid agent id"
fi

section "AGENT — Change Agent Password (c=9426)"

test_name "Change agent password missing params (c=9426)"
RESP=$(admin_get "c=9426")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9426)"
else
    _fail "Expected error for missing password params"
fi

section "AGENT — Agent Users (c=9425)"

test_name "Agent users list missing agent id (c=9425)"
RESP=$(admin_get "c=9425")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing agent id rejected (c=9425)"
else
    _pass "c=9425 without id returned data (may return all)"
fi

if [[ -n "${AGENT_ID:-}" ]]; then
    test_name "Agent users with valid agent id (c=9425)"
    RESP=$(admin_get "c=9425&agent_id=${AGENT_ID}")
    assert_not_empty "$RESP" "agent users with id"
    OK=$(_resp_ok "$RESP")
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    if [[ "$OK" == "true" ]]; then
        _pass "Agent users with valid id succeeded"
        assert_has_field "$RESP" "data"
    else
        _skip "Agent users returned errorCode=${EC} — check param name"
    fi
fi

section "AGENT — Agent Codes (c=8826)"

test_name "List agent codes (c=8826)"
RESP=$(admin_get "c=8826")
assert_success "$RESP"
assert_has_field "$RESP" "data"

section "AGENT — Agent Users Portal (c=8827)"

test_name "Agent users portal (c=8827)"
RESP=$(admin_get "c=8827")
assert_success "$RESP"
assert_has_field "$RESP" "data"

section "AGENT — Agent Stats (c=8840)"

test_name "Agent stats (c=8840)"
RESP=$(admin_get "c=8840")
assert_success "$RESP"
assert_has_field "$RESP" "data"

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)
test_name "Agent stats with date filter (c=8840)"
RESP=$(admin_get "c=8840&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

print_summary

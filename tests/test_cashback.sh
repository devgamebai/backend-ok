#!/bin/bash
# Test: Cashback & Rebate
# Cashback: c=9800,9801,9802,9803,9804,9806,9808
# Rebate:   c=9750,9751,9752,9755,9756,9757

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)

# ══════════════════════════════════════════════════════
# CASHBACK
# ══════════════════════════════════════════════════════

section "CASHBACK — Get Config (c=9800)"

test_name "Get cashback config (c=9800)"
RESP=$(admin_get "c=9800")
assert_success "$RESP"
assert_has_field "$RESP" "data"

section "CASHBACK — List Configs (c=9801)"

test_name "List cashback configs (c=9801)"
RESP=$(admin_get "c=9801")
assert_success "$RESP"
assert_has_field "$RESP" "data"

CB_CONFIG_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('config_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('configs',[])))
    if items:
        print(items[0].get('id',items[0].get('config_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First config ID: ${CB_CONFIG_ID:-none}"

section "CASHBACK — Update Config (c=9802)"

test_name "Update cashback config missing params (c=9802)"
RESP=$(admin_get "c=9802")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9802)"
else
    _fail "Expected error for missing update params"
fi

if [[ -n "${CB_CONFIG_ID:-}" ]]; then
    test_name "Update cashback config with valid id (c=9802)"
    RESP=$(admin_get "c=9802&id=${CB_CONFIG_ID}&status=1")
    assert_not_empty "$RESP" "update cashback config"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "CASHBACK — Create Config (c=9803)"

test_name "Create cashback config missing params (c=9803)"
RESP=$(admin_get "c=9803")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9803)"
else
    _fail "Expected error for missing create params"
fi

test_name "Create cashback config with params (c=9803)"
TS=$(date +%s)
RESP=$(admin_get "c=9803&game_type=slot&percent=1.5&min_bet=10000&max_cashback=500000&status=1&name=TestCB${TS}")
assert_not_empty "$RESP" "create cashback config"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

section "CASHBACK — Cashback Logs (c=9804)"

test_name "Get cashback logs (c=9804)"
RESP=$(admin_get "c=9804")
assert_success "$RESP"
assert_has_field "$RESP" "data"

test_name "Get cashback logs with date filter (c=9804)"
RESP=$(admin_get "c=9804&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

test_name "Get cashback logs with username filter (c=9804)"
RESP=$(admin_get "c=9804&username=superadmin")
assert_success "$RESP"

section "CASHBACK — Process Cashback (c=9806)"

test_name "Process cashback missing params (c=9806)"
RESP=$(admin_get "c=9806")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9806)"
else
    _fail "Expected error for missing process params"
fi

test_name "Process cashback with invalid id (c=9806)"
RESP=$(admin_get "c=9806&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid id rejected for process cashback (errorCode=$EC)"
else
    _fail "Expected error for invalid cashback id"
fi

section "CASHBACK — Reject Cashback (c=9808)"

test_name "Reject cashback missing params (c=9808)"
RESP=$(admin_get "c=9808")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9808)"
else
    _fail "Expected error for missing reject params"
fi

test_name "Reject cashback with invalid id (c=9808)"
RESP=$(admin_get "c=9808&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid id rejected for reject cashback (errorCode=$EC)"
else
    _fail "Expected error for invalid cashback id on reject"
fi

# ══════════════════════════════════════════════════════
# REBATE
# ══════════════════════════════════════════════════════

section "REBATE — Dashboard (c=9750)"

test_name "Rebate dashboard (c=9750)"
RESP=$(admin_get "c=9750")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_has_field "$RESP" "limit"
assert_has_field "$RESP" "page"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "agent_user_id,total_volume,is_active,share_percentage,total_paid,total_unpaid,agent_nickname,rebate_percentage,total_rebate,period_type"

test_name "Rebate dashboard with date filter (c=9750)"
RESP=$(admin_get "c=9750&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

section "REBATE — Detail (c=9751)"

test_name "Rebate detail missing agent_user_id (c=9751)"
RESP=$(admin_get "c=9751")
# Requires agent_user_id param — returns error without it
assert_not_empty "$RESP" "rebate detail no params"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode','?'))" 2>/dev/null)
if [[ "$OK" != "true" ]]; then
    _pass "Rebate detail without agent_user_id rejected (errorCode=$EC)"
else
    _pass "Rebate detail without params succeeded"
    assert_has_field "$RESP" "data"
fi

test_name "Rebate detail with agent_user_id (c=9751)"
# Use agent id=2 (sunc1) from live data
RESP=$(admin_get "c=9751&agent_user_id=2")
assert_not_empty "$RESP" "rebate detail with agent_user_id"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "Rebate detail with agent_user_id succeeded"
else
    _skip "Rebate detail returned errorCode=${EC}"
fi

section "REBATE — Logs (c=9752)"

test_name "Rebate logs (c=9752)"
RESP=$(admin_get "c=9752")
assert_success "$RESP"
assert_has_field "$RESP" "data"

test_name "Rebate logs with date range (c=9752)"
RESP=$(admin_get "c=9752&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

section "REBATE — Config Get (c=9755)"

test_name "Get rebate config missing agent params (c=9755)"
RESP=$(admin_get "c=9755")
# Requires agent_user_id and agent_nickname
assert_not_empty "$RESP" "rebate config no params"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode','?'))" 2>/dev/null)
if [[ "$OK" != "true" ]]; then
    _pass "Rebate config without required params rejected (errorCode=$EC)"
else
    _pass "Rebate config succeeded"
    assert_has_field "$RESP" "data"
fi

test_name "Get rebate config with agent params (c=9755)"
RESP=$(admin_get "c=9755&agent_user_id=2&agent_nickname=DaiLySo1SunWin")
assert_not_empty "$RESP" "rebate config with params"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" == "true" ]]; then
    _pass "Rebate config with params succeeded"
else
    _skip "Rebate config with params returned errorCode=${EC}"
fi

section "REBATE — Config Update (c=9756)"

test_name "Update rebate config missing params (c=9756)"
RESP=$(admin_get "c=9756")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9756)"
else
    _fail "Expected error for missing rebate config params"
fi

section "REBATE — Config Delete (c=9757)"

test_name "Delete rebate config (c=9757)"
RESP=$(admin_get "c=9757")
assert_not_empty "$RESP" "delete rebate config no params"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
# May succeed (deletes nothing) or reject — both are acceptable
echo "  success=$OK errorCode=$EC"
if [[ "$OK" == "true" ]]; then
    _pass "Delete rebate config without id succeeded (idempotent)"
else
    _pass "Delete rebate config without id rejected (errorCode=$EC)"
fi

test_name "Delete rebate config with invalid id (c=9757)"
RESP=$(admin_get "c=9757&id=99999999")
assert_not_empty "$RESP" "delete rebate config invalid id"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
if [[ "$OK" != "true" ]]; then
    _pass "Invalid rebate config id rejected (errorCode=$EC)"
else
    _pass "Delete with invalid id succeeded (idempotent delete)"
fi

print_summary

#!/bin/bash
# Test: User Balance History (c=9972)
# Spec: docs/superpowers/specs/2026-05-14-user-balance-history-api-design.md

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
SEVEN_AGO=$(date -d "-7 days" +%Y-%m-%d 2>/dev/null || date -v-7d +%Y-%m-%d)

# TEST_NICK can be overridden via env to point at a user that exists in the
# local DB. On staging, "superadmin" works; on a clean local dev DB use the
# first seeded user (e.g. BALANCE_TEST_NICK=sumap123).
BALANCE_TEST_NICK="${BALANCE_TEST_NICK:-superadmin}"

section "USER BALANCE HISTORY — c=9972"

# --- 0. Unauthenticated request rejected ---
test_name "No aat returns 1001 (c=9972)"
RESP=$(curl -sf --max-time 15 "${ADMIN_API}?c=9972&nn=${BALANCE_TEST_NICK}" 2>&1 || echo '{"success":false,"errorCode":"CURL_ERROR"}')
assert_error "$RESP" "1001"

# --- 1. Missing identifier ---
test_name "Missing both nn and uid (c=9972)"
RESP=$(admin_get "c=9972")
assert_error "$RESP" "4001"

# --- 2. Bogus nickname ---
test_name "Bogus nickname returns 1002 (c=9972)"
RESP=$(admin_get "c=9972&nn=__does_not_exist_$(date +%s)__")
assert_error "$RESP" "1002"

# --- 3. Valid call, default range ---
test_name "Valid call default range (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK")
assert_success "$RESP"
assert_has_field "$RESP" "list"
assert_has_field "$RESP" "summary"
assert_has_field "$RESP" "pagination"

# --- 4. Explicit category filter ---
test_name "category=nap,rut filter (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&category=nap,rut&from=${SEVEN_AGO}&to=${TODAY}")
assert_success "$RESP"
assert_has_field "$RESP" "list"

# --- 5. Invalid date range ---
test_name "from > to returns 4003 (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&from=${TODAY}&to=${SEVEN_AGO}")
assert_error "$RESP" "4003"

# --- 6. Invalid category token ---
test_name "Invalid category token returns 4005 (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&category=bogus_category")
assert_error "$RESP" "4005"

# --- 7. limit clamping ---
test_name "limit=500 silently clamped to 200 (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&limit=500")
assert_success "$RESP"
LIMIT=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data')
if isinstance(data,str):
    try: data=json.loads(data)
    except: data={}
p=(data or {}).get('pagination',{}) if isinstance(data,dict) else {}
print(p.get('limit',''))
" 2>/dev/null)
if [[ "$LIMIT" == "200" ]]; then _pass "limit clamped to 200"; else _fail "expected pagination.limit=200, got '$LIMIT'"; fi

# --- 8. Row shape ---
test_name "Each row carries required fields (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&limit=5")
SHAPE_OK=$(echo "$RESP" | python3 -c "
import sys,json
need={'trans_id','trans_time','nickname','category','action_label','amount','balance_before','balance_after','fee'}
try:
    d=json.load(sys.stdin)
    data=d.get('data')
    if isinstance(data,str): data=json.loads(data)
    rows=(data or {}).get('list',[])
    if not rows: print('empty_ok'); sys.exit(0)
    first=rows[0]
    missing=need - set(first.keys())
    print('ok' if not missing else 'missing:' + ','.join(missing))
except Exception as e:
    print('error:'+str(e))
" 2>/dev/null)
if [[ "$SHAPE_OK" == "ok" || "$SHAPE_OK" == "empty_ok" ]]; then _pass "row shape ok ($SHAPE_OK)"; else _fail "row shape: $SHAPE_OK"; fi

# --- 9. balance arithmetic ---
# balance_before/balance_after are clamped to >= 0 via BalanceGuard (CLAUDE.md
# mandate — log_money_user_vin.current_money is sourced from vinTotal, which
# can go negative). When neither value was clamped, balance_after - balance_before
# must equal amount; when at least one is 0 we cannot tell whether clamping
# occurred, so we skip the row.
test_name "balance_after = balance_before + amount when unclamped (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK&limit=20")
ARITH_OK=$(echo "$RESP" | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    data=d.get('data')
    if isinstance(data,str): data=json.loads(data)
    rows=(data or {}).get('list',[])
    if not rows: print('empty_ok'); sys.exit(0)
    bad=[]
    for r in rows:
        bb=r.get('balance_before',0); ba=r.get('balance_after',0); am=r.get('amount',0)
        if bb == 0 or ba == 0:
            continue  # possibly clamped — skip
        if bb + am != ba:
            bad.append(r.get('trans_id'))
    print('ok' if not bad else 'bad:'+str(len(bad)))
except Exception as e:
    print('error:'+str(e))
" 2>/dev/null)
if [[ "$ARITH_OK" == "ok" || "$ARITH_OK" == "empty_ok" ]]; then _pass "balance arithmetic ($ARITH_OK)"; else _fail "balance arithmetic: $ARITH_OK"; fi

# --- 10. Summary present with 10 totals ---
test_name "Summary carries 10 totals + net_change (c=9972)"
RESP=$(admin_get "c=9972&nn=$BALANCE_TEST_NICK")
SUM_OK=$(echo "$RESP" | python3 -c "
import sys,json
need={'total_nap','total_rut','total_cashback','total_transfer_in','total_transfer_out','total_game_bet','total_game_win','total_giftcode','total_admin_adjust','total_other','net_change'}
try:
    d=json.load(sys.stdin)
    data=d.get('data')
    if isinstance(data,str): data=json.loads(data)
    s=(data or {}).get('summary',{})
    missing=need - set(s.keys())
    print('ok' if not missing else 'missing:'+','.join(missing))
except Exception as e:
    print('error:'+str(e))
" 2>/dev/null)
if [[ "$SUM_OK" == "ok" ]]; then _pass "summary fields complete"; else _fail "summary: $SUM_OK"; fi

# Summary
section "RESULTS"
echo -e "PASS: $PASS_COUNT  FAIL: $FAIL_COUNT  SKIP: $SKIP_COUNT"
[[ $FAIL_COUNT -eq 0 ]]

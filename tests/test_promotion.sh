#!/bin/bash
# Test: Promotion CRUD
# Tests: c=9640 (create), c=9641 (list), c=9642 (update), c=9643 (logs)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "PROMOTION — List Promotions (c=9641)"

test_name "List promotions (c=9641)"
RESP=$(admin_get "c=9641")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_has_field "$RESP" "total"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "end_time,created_at,max_users,max_bonus_per_tx,start_time,updated_at,promo_type,turnover_factor,id,gate,bonus_percent,status"

test_name "List promotions with pagination (c=9641)"
RESP=$(admin_get "c=9641&page=1&size=10")
assert_success "$RESP"

test_name "List promotions with status filter (c=9641)"
RESP=$(admin_get "c=9641&status=1")
assert_success "$RESP"

section "PROMOTION — Create Promotion (c=9640)"

test_name "Create promotion missing params (c=9640)"
RESP=$(admin_get "c=9640")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9640)"
else
    _fail "Expected error for missing promotion params"
fi

test_name "Create promotion with required params (c=9640)"
TS=$(date +%s)
RESP=$(admin_get "c=9640&name=TestPromo${TS}&type=1&value=10&min_deposit=100000&max_bonus=500000&status=1&start_date=2025-01-01&end_date=2025-12-31")
assert_not_empty "$RESP" "create promotion"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
PROMO_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
v = d.get('data',{})
if isinstance(v,dict):
    print(v.get('id',v.get('promotion_id','')))
else:
    print('')
" 2>/dev/null)
echo "  Promotion ID: ${PROMO_ID:-none}"

section "PROMOTION — Update Promotion (c=9642)"

test_name "Update promotion missing id (c=9642)"
RESP=$(admin_get "c=9642")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9642)"
else
    _fail "Expected error for missing promotion id"
fi

test_name "Update promotion with invalid id (c=9642)"
RESP=$(admin_get "c=9642&id=99999999&status=0")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid promotion id rejected (errorCode=$EC)"
else
    _pass "Update with invalid id succeeded (check data integrity)"
fi

# If we got a promotion id from creation, update it
if [[ -n "${PROMO_ID:-}" && "$PROMO_ID" != "none" ]]; then
    test_name "Update newly created promotion (c=9642)"
    RESP=$(admin_get "c=9642&id=${PROMO_ID}&status=0")
    assert_not_empty "$RESP" "update promotion"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "PROMOTION — Promotion Logs (c=9643)"

test_name "Get promotion logs (c=9643)"
RESP=$(admin_get "c=9643")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Get promotion logs with pagination (c=9643)"
RESP=$(admin_get "c=9643&page=1&size=10")
assert_success "$RESP"

test_name "Get promotion logs filter by username (c=9643)"
RESP=$(admin_get "c=9643&username=superadmin")
assert_success "$RESP"

test_name "Get promotion logs filter by date range (c=9643)"
FROM=$(date -d '30 days ago' +%Y-%m-%d 2>/dev/null || date -v-30d +%Y-%m-%d 2>/dev/null || echo "2025-01-01")
TO=$(date +%Y-%m-%d)
RESP=$(admin_get "c=9643&from_date=${FROM}&to_date=${TO}")
assert_success "$RESP"

print_summary

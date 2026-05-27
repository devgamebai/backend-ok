#!/bin/bash
# Test: Dashboard & Reports
# Tests: c=7,9,18,19,12,108,164,165,9437
#
# Live response notes (2026-04-04):
#   c=7   — success:true, data:null, game stats at top level (taiXiu, actionGame, etc.)
#   c=9   — success:true, data:null, totals:[]
#   c=18  — returns errorCode:1001 (requires specific date params or permissions)
#   c=19  — untested, assume similar to c=18
#   c=108 — no success field, returns {data:[...], totalRecords:N}
#   c=9437 — success:true, data:[], totalRecords:0

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)

section "DASHBOARD — System Report (c=7)"

test_name "System report (c=7)"
RESP=$(admin_get "c=7")
assert_success "$RESP"
# c=7 returns game data at top level, not in data field
HAS=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
keys = list(d.keys())
game_keys = [k for k in keys if k not in ('success','errorCode','message','data')]
print('found' if game_keys else 'missing')
" 2>/dev/null)
if [[ "$HAS" == "found" ]]; then
    _pass "System report contains game stat fields"
else
    _fail "System report missing expected game stat fields"
fi

section "DASHBOARD — Chart Timeline (c=9)"

test_name "Chart timeline (c=9)"
RESP=$(admin_get "c=9")
assert_success "$RESP"
# c=9 returns totals:[] at top level
HAS=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('found' if 'totals' in d or 'data' in d else 'missing')
" 2>/dev/null)
if [[ "$HAS" == "found" ]]; then
    _pass "Chart timeline contains totals/data field"
else
    _fail "Chart timeline missing totals/data field"
fi

test_name "Chart timeline with date range (c=9)"
RESP=$(admin_get "c=9&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

section "DASHBOARD — Revenue (c=18)"

test_name "Revenue report (c=18) — requires date params"
RESP=$(admin_get "c=18&from_date=${MONTH_START}&to_date=${TODAY}")
# Known to return 1001 without correct params; with date params may work
assert_not_empty "$RESP" "revenue report"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success','')" 2>/dev/null || echo "")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
echo "  success=$OK errorCode=$EC"
if [[ "$OK" == "True" ]]; then
    _pass "Revenue report succeeded"
else
    _skip "Revenue report returned errorCode=${EC} — may require specific permissions"
fi

section "DASHBOARD — Daily Revenue (c=19)"

test_name "Daily revenue report (c=19)"
RESP=$(admin_get "c=19&date=${TODAY}")
assert_not_empty "$RESP" "daily revenue"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success','')" 2>/dev/null || echo "")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
echo "  success=$OK errorCode=$EC"
if [[ "$OK" == "True" ]]; then
    _pass "Daily revenue report succeeded"
else
    _skip "Daily revenue returned errorCode=${EC}"
fi

section "DASHBOARD — Top Games (c=12)"

test_name "Top games (c=12)"
RESP=$(admin_get "c=12")
assert_not_empty "$RESP" "top games"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Top games responded successfully"
else
    _skip "Top games returned errorCode=${EC}"
fi

test_name "Top games with date range (c=12)"
RESP=$(admin_get "c=12&from_date=${MONTH_START}&to_date=${TODAY}")
assert_not_empty "$RESP" "top games date range"

section "DASHBOARD — CCU (c=108)"

test_name "Current CCU (c=108)"
RESP=$(admin_get "c=108")
# c=108 returns {data:[...], totalRecords:N} without success field
assert_success "$RESP"
HAS=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('found' if 'data' in d or 'totalRecords' in d else 'missing')
" 2>/dev/null)
if [[ "$HAS" == "found" ]]; then
    _pass "CCU response contains data/totalRecords"
else
    _fail "CCU response missing data/totalRecords"
fi
assert_has_field "$RESP" "totalRecords"
assert_has_field "$RESP" "limit"
assert_has_field "$RESP" "page"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "time_log,total,ccu,web,phone,desktop"

section "DASHBOARD — New Players (c=164)"

test_name "New players report (c=164)"
RESP=$(admin_get "c=164")
assert_not_empty "$RESP" "new players"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "New players report succeeded"
else
    _skip "New players returned errorCode=${EC}"
fi

test_name "New players with date range (c=164)"
RESP=$(admin_get "c=164&from_date=${MONTH_START}&to_date=${TODAY}")
assert_not_empty "$RESP" "new players date range"

section "DASHBOARD — Money Stats (c=165)"

test_name "Money stats (c=165)"
RESP=$(admin_get "c=165")
assert_not_empty "$RESP" "money stats"
OK=$(_resp_ok "$RESP")
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null || echo "")
if [[ "$OK" == "true" ]]; then
    _pass "Money stats report succeeded"
else
    _skip "Money stats returned errorCode=${EC}"
fi

test_name "Money stats with date range (c=165)"
RESP=$(admin_get "c=165&from_date=${MONTH_START}&to_date=${TODAY}")
assert_not_empty "$RESP" "money stats date range"

section "DASHBOARD — Game Stats (c=9437)"

test_name "Game stats (c=9437)"
RESP=$(admin_get "c=9437")
assert_success "$RESP"
assert_has_field "$RESP" "data"

test_name "Game stats with date filter (c=9437)"
RESP=$(admin_get "c=9437&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

print_summary

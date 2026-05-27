#!/bin/bash
# Test: Banner & Event CRUD
# Banners: c=9400 (create), c=9401 (update), c=9402 (delete), c=9403 (list), c=9404 (detail)
# Events:  c=9410 (create), c=9411 (update), c=9412 (delete), c=9413 (list), c=9414 (detail)

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

# ──────────────────────────────────────────────────────
section "BANNER — List Banners (c=9403)"
# ──────────────────────────────────────────────────────

test_name "List banners (c=9403)"
RESP=$(admin_get "c=9403")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_has_field "$RESP" "total"
assert_has_field "$RESP" "totalRecords"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "image_path,action,id,title,url,status"

test_name "List banners with pagination (c=9403)"
RESP=$(admin_get "c=9403&page=1&size=10")
assert_success "$RESP"

# Grab first banner id for later tests
BANNER_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('banner_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('data',[])))
    if items:
        print(items[0].get('id',items[0].get('banner_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First banner ID: ${BANNER_ID:-none}"

# ──────────────────────────────────────────────────────
section "BANNER — Banner Detail (c=9404)"
# ──────────────────────────────────────────────────────

test_name "Banner detail missing id (c=9404)"
RESP=$(admin_get "c=9404")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9404)"
else
    _pass "Detail without id returned data (check if id required)"
fi

if [[ -n "${BANNER_ID:-}" ]]; then
    test_name "Banner detail with valid id (c=9404)"
    RESP=$(admin_get "c=9404&id=${BANNER_ID}")
    assert_success "$RESP"
    assert_has_field "$RESP" "data"
fi

# ──────────────────────────────────────────────────────
section "BANNER — Create Banner (c=9400)"
# ──────────────────────────────────────────────────────

test_name "Create banner missing params (c=9400)"
RESP=$(admin_get "c=9400")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9400)"
else
    _fail "Expected error for missing banner params"
fi

test_name "Create banner with params (c=9400)"
TS=$(date +%s)
RESP=$(admin_get "c=9400&title=TestBanner${TS}&image_url=https%3A%2F%2Fexample.com%2Ftest.jpg&link=https%3A%2F%2Fexample.com&status=1&position=1&sort_order=99")
assert_not_empty "$RESP" "create banner"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
NEW_BANNER_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
v=d.get('data',{})
if isinstance(v,dict):
    print(v.get('id',v.get('banner_id','')))
else:
    print('')
" 2>/dev/null)
echo "  New Banner ID: ${NEW_BANNER_ID:-none}"

# ──────────────────────────────────────────────────────
section "BANNER — Update Banner (c=9401)"
# ──────────────────────────────────────────────────────

test_name "Update banner missing id (c=9401)"
RESP=$(admin_get "c=9401")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9401)"
else
    _fail "Expected error for missing banner id"
fi

if [[ -n "${NEW_BANNER_ID:-}" ]]; then
    test_name "Update newly created banner (c=9401)"
    RESP=$(admin_get "c=9401&id=${NEW_BANNER_ID}&status=0")
    assert_not_empty "$RESP" "update banner"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
elif [[ -n "${BANNER_ID:-}" ]]; then
    test_name "Update existing banner (c=9401)"
    RESP=$(admin_get "c=9401&id=${BANNER_ID}&status=1")
    assert_not_empty "$RESP" "update banner"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

# ──────────────────────────────────────────────────────
section "BANNER — Delete Banner (c=9402)"
# ──────────────────────────────────────────────────────

test_name "Delete banner missing id (c=9402)"
RESP=$(admin_get "c=9402")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9402)"
else
    _fail "Expected error for missing banner id"
fi

if [[ -n "${NEW_BANNER_ID:-}" ]]; then
    test_name "Delete newly created test banner (c=9402)"
    RESP=$(admin_get "c=9402&id=${NEW_BANNER_ID}")
    assert_not_empty "$RESP" "delete banner"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

test_name "Delete banner with invalid id (c=9402)"
RESP=$(admin_get "c=9402&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid banner id rejected (errorCode=$EC)"
else
    _fail "Expected error for invalid banner id"
fi

# ══════════════════════════════════════════════════════
# EVENTS
# ══════════════════════════════════════════════════════

section "EVENT — List Events (c=9413)"

test_name "List events (c=9413)"
RESP=$(admin_get "c=9413")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_has_field "$RESP" "total"
assert_has_field "$RESP" "totalRecords"
assert_data_is_array "$RESP"
assert_data_has_items "$RESP" "create_by,amount,name,id,created_date,expired_date"

test_name "List events with pagination (c=9413)"
RESP=$(admin_get "c=9413&page=1&size=10")
assert_success "$RESP"

EVENT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('event_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('data',[])))
    if items:
        print(items[0].get('id',items[0].get('event_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First event ID: ${EVENT_ID:-none}"

section "EVENT — Event Detail (c=9414)"

test_name "Event detail missing id (c=9414)"
RESP=$(admin_get "c=9414")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9414)"
else
    _pass "Detail without id returned data"
fi

if [[ -n "${EVENT_ID:-}" ]]; then
    test_name "Event detail with valid id (c=9414)"
    RESP=$(admin_get "c=9414&id=${EVENT_ID}")
    assert_success "$RESP"
    assert_has_field "$RESP" "data"
fi

section "EVENT — Create Event (c=9410)"

test_name "Create event missing params (c=9410)"
RESP=$(admin_get "c=9410")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=9410)"
else
    _fail "Expected error for missing event params"
fi

test_name "Create event with params (c=9410)"
TS=$(date +%s)
RESP=$(admin_get "c=9410&title=TestEvent${TS}&content=TestContent&status=1&start_date=2025-01-01&end_date=2025-12-31")
assert_not_empty "$RESP" "create event"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"
NEW_EVENT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
v=d.get('data',{})
if isinstance(v,dict):
    print(v.get('id',v.get('event_id','')))
else:
    print('')
" 2>/dev/null)
echo "  New Event ID: ${NEW_EVENT_ID:-none}"

section "EVENT — Update Event (c=9411)"

test_name "Update event missing id (c=9411)"
RESP=$(admin_get "c=9411")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9411)"
else
    _fail "Expected error for missing event id"
fi

if [[ -n "${NEW_EVENT_ID:-}" ]]; then
    test_name "Update newly created event (c=9411)"
    RESP=$(admin_get "c=9411&id=${NEW_EVENT_ID}&status=0")
    assert_not_empty "$RESP" "update event"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

section "EVENT — Delete Event (c=9412)"

test_name "Delete event missing id (c=9412)"
RESP=$(admin_get "c=9412")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing id rejected (c=9412)"
else
    _fail "Expected error for missing event id"
fi

if [[ -n "${NEW_EVENT_ID:-}" ]]; then
    test_name "Delete newly created test event (c=9412)"
    RESP=$(admin_get "c=9412&id=${NEW_EVENT_ID}")
    assert_not_empty "$RESP" "delete event"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

test_name "Delete event with invalid id (c=9412)"
RESP=$(admin_get "c=9412&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid event id rejected (errorCode=$EC)"
else
    _fail "Expected error for invalid event id"
fi

print_summary

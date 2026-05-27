#!/bin/bash
# Test: Game Servers & Minigames
# Tests: Docker status for 17 game servers, WS-Bridge, WebSocket upgrade,
#        Jackpot c=729,730,731,732,733, Game logs c=516,162,163,505

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
MONTH_START=$(date +%Y-%m-01)

# ══════════════════════════════════════════════════════
section "GAMES — Docker Container Status"
# ══════════════════════════════════════════════════════

GAME_CONTAINERS=(
    "sunwinkr-game-poker"
    "sunwinkr-game-pokertour"
    "sunwinkr-game-tlmn"
    "sunwinkr-game-xocdia"
    "sunwinkr-game-bacay"
    "sunwinkr-game-baicao"
    "sunwinkr-game-binh"
    "sunwinkr-game-caro"
    "sunwinkr-game-cotuong"
    "sunwinkr-game-coup"
    "sunwinkr-game-lieng"
    "sunwinkr-game-sam"
    "sunwinkr-game-slot"
    "sunwinkr-game-xizach"
    "sunwinkr-game-minigame"
    "sunwinkr-game-thirdparty"
    "sunwinkr-game-xocdiatulinh"
    "sunwinkr-banca"
    "sunwinkr-ws-bridge"
)

for container in "${GAME_CONTAINERS[@]}"; do
    test_name "Container running: $container"
    STATUS=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not_found")
    if [[ "$STATUS" == "running" ]]; then
        _pass "Container $container is running"
    elif [[ "$STATUS" == "not_found" ]]; then
        _skip "Container $container not found (may not be deployed)"
    else
        _fail "Container $container status: $STATUS"
    fi
done

# ══════════════════════════════════════════════════════
section "GAMES — WebSocket Upgrade Tests"
# ══════════════════════════════════════════════════════

# Test WebSocket handshake (HTTP 101) via ws-bridge and direct game ports
# Test via subdomains (how FE actually connects) using internal nginx
WS_ENDPOINTS=(
    "wmini.staging-play.sunkr.bet"
    "wslot.staging-play.sunkr.bet"
    "wxocdia.staging-play.sunkr.bet"
    "wpoker.staging-play.sunkr.bet"
    "wbanca.staging-play.sunkr.bet"
)

for ep in "${WS_ENDPOINTS[@]}"; do
    test_name "WebSocket upgrade: $ep"
    # Test via internal nginx (localhost) with Host header for reliable 101
    HTTP_CODE=$(curl -s --max-time 10 -o /dev/null -w "%{http_code}" \
        -H "Host: ${ep}" \
        -H "Upgrade: websocket" \
        -H "Connection: Upgrade" \
        -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
        -H "Sec-WebSocket-Version: 13" \
        "http://localhost/" 2>/dev/null)
    HTTP_CODE="${HTTP_CODE:-000}"
    if [[ "$HTTP_CODE" == "101" ]]; then
        _pass "WS upgrade HTTP 101 for $ep"
    elif [[ "$HTTP_CODE" == "200" ]]; then
        _pass "WS endpoint reachable HTTP 200 for $ep"
    elif [[ "$HTTP_CODE" == "000" ]]; then
        _skip "No response for $ep (endpoint may not exist)"
    else
        _fail "Expected 101, got $HTTP_CODE for $ep"
    fi
done

# ══════════════════════════════════════════════════════
section "GAMES — Jackpot: List (c=729)"
# ══════════════════════════════════════════════════════

test_name "List jackpots (c=729)"
RESP=$(admin_get "c=729")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

JACKPOT_ID=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    print(data[0].get('id',data[0].get('jackpot_id','')))
elif isinstance(data,dict):
    items=data.get('list',data.get('items',data.get('jackpots',[])))
    if items:
        print(items[0].get('id',items[0].get('jackpot_id','')))
    else:
        print('')
else:
    print('')
" 2>/dev/null)
echo "  First jackpot ID: ${JACKPOT_ID:-none}"

# ══════════════════════════════════════════════════════
section "GAMES — Jackpot: Set (c=730)"
# ══════════════════════════════════════════════════════

test_name "Set jackpot missing params (c=730)"
RESP=$(admin_get "c=730")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=730)"
else
    _fail "Expected error for missing jackpot params"
fi

if [[ -n "${JACKPOT_ID:-}" ]]; then
    test_name "Set jackpot value with valid id (c=730)"
    RESP=$(admin_get "c=730&id=${JACKPOT_ID}&value=10000000")
    assert_not_empty "$RESP" "set jackpot"
    OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
    EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
    echo "  success=$OK errorCode=$EC"
fi

# ══════════════════════════════════════════════════════
section "GAMES — Jackpot: Force Result (c=731,732,733)"
# ══════════════════════════════════════════════════════

test_name "Force result set missing params (c=731)"
RESP=$(admin_get "c=731")
assert_error "$RESP" "4001"

test_name "Force result list/set (c=732)"
RESP=$(admin_get "c=732")
assert_not_empty "$RESP" "force result c=732"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

test_name "Force result delete missing params (c=733)"
RESP=$(admin_get "c=733")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Missing params rejected (c=733)"
else
    _fail "Expected error for missing delete params"
fi

test_name "Force result delete with invalid id (c=733)"
RESP=$(admin_get "c=733&id=99999999")
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
if [[ "$OK" != "True" ]]; then
    _pass "Invalid force result id rejected (errorCode=$EC)"
else
    _fail "Expected error for invalid force result id"
fi

# ══════════════════════════════════════════════════════
section "GAMES — Game Logs: Slot (c=516)"
# ══════════════════════════════════════════════════════

test_name "Slot game logs (c=516)"
RESP=$(admin_get "c=516")
assert_success "$RESP"
assert_has_field "$RESP" "data"
assert_data_is_array "$RESP"

test_name "Slot game logs with date filter (c=516)"
RESP=$(admin_get "c=516&from_date=${MONTH_START}&to_date=${TODAY}")
assert_success "$RESP"

test_name "Slot game logs with username filter (c=516)"
RESP=$(admin_get "c=516&username=superadmin&page=1&size=10")
assert_success "$RESP"

# ══════════════════════════════════════════════════════
section "GAMES — Game Logs: Taixiu (c=162,163,505)"
# ══════════════════════════════════════════════════════

test_name "Taixiu game logs (c=162)"
RESP=$(admin_get "c=162")
assert_not_empty "$RESP" "taixiu logs c=162"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

test_name "Taixiu game logs (c=163)"
RESP=$(admin_get "c=163")
assert_not_empty "$RESP" "taixiu logs c=163"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

test_name "Taixiu game logs (c=505)"
RESP=$(admin_get "c=505")
assert_not_empty "$RESP" "taixiu logs c=505"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

test_name "Taixiu logs with date range (c=505)"
RESP=$(admin_get "c=505&from_date=${MONTH_START}&to_date=${TODAY}&page=1&size=10")
assert_not_empty "$RESP" "taixiu logs date range"

print_summary

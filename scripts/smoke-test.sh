#!/bin/bash
# =============================================================================
# STAGING SMOKE TEST
# =============================================================================
# Tests all critical endpoints with a valid login session
#
# Usage: ./scripts/smoke-test.sh [domain] [username] [password]
# Default: staging-play.sunkr.bet / superadmin / admin123
# =============================================================================

DOMAIN="${1:-staging-play.sunkr.bet}"
USERNAME="${2:-superadmin}"
PASSWORD="${3:-admin123}"
PW_MD5=$(echo -n "$PASSWORD" | md5sum | cut -d' ' -f1)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

check() {
    local name="$1"
    local result="$2"
    local expected="$3"
    if echo "$result" | grep -q "$expected"; then
        echo -e "  ${GREEN}PASS${NC} $name"
        PASS=$((PASS+1))
    else
        echo -e "  ${RED}FAIL${NC} $name"
        echo -e "       ${YELLOW}Got: $(echo "$result" | head -c 200)${NC}"
        FAIL=$((FAIL+1))
    fi
}

echo "=== Staging Smoke Test ==="
echo "Domain: $DOMAIN"
echo "User:   $USERNAME"
echo ""

# ---------------------------------------------------------------
# 1. Health check
# ---------------------------------------------------------------
echo "[Health]"
RES=$(curl -sk -o /dev/null -w "%{http_code}" "https://$DOMAIN/health" --max-time 5 2>&1)
check "Nginx health" "$RES" "200"

RES=$(curl -sk -o /dev/null -w "%{http_code}" "https://staging-admin.sunkr.bet/health" --max-time 5 2>&1)
check "Admin health" "$RES" "200"

# ---------------------------------------------------------------
# 2. Server time (c=9)
# ---------------------------------------------------------------
echo ""
echo "[API]"
RES=$(curl -sk "https://$DOMAIN/api?c=9" --max-time 5 2>&1)
check "Server time (c=9)" "$RES" "^[0-9]"

# ---------------------------------------------------------------
# 3. Register test user
# ---------------------------------------------------------------
TEST_USER="smoke$(date +%H%M%S)"
RES=$(curl -sk "https://$DOMAIN/api?c=1&un=$TEST_USER&pw=$PW_MD5&pf=web" --max-time 10 2>&1)
check "Register ($TEST_USER)" "$RES" '"success":true'

# ---------------------------------------------------------------
# 4. Login
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=3&un=$USERNAME&pw=$PW_MD5&platform=web" --max-time 10 2>&1)
check "Login ($USERNAME)" "$RES" '"success":true'

# Extract access token
AT=$(echo "$RES" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
if [ -n "$AT" ] && [ "$AT" != "None" ]; then
    echo -e "       Token: ${AT:0:16}..."
else
    echo -e "  ${RED}Cannot extract token - remaining tests will fail${NC}"
    AT=""
fi

# ---------------------------------------------------------------
# 5. Get app config (c=6)
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=6&at=$AT" --max-time 10 2>&1)
RES=$(curl -sk "https://$DOMAIN/api?c=6&nn=$USERNAME&at=$AT" --max-time 10 2>&1)
check "App config (c=6)" "$RES" "status"

# ---------------------------------------------------------------
# 6. Get billing config (c=130)
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=130&at=$AT" --max-time 10 2>&1)
check "Billing config (c=130)" "$RES" "status"

# ---------------------------------------------------------------
# 7. Bank list (c=2011)
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=2011&at=$AT" --max-time 10 2>&1)
check "Bank list (c=2011)" "$RES" '"status":0'

# ---------------------------------------------------------------
# 8. Bank search (c=2008)
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=2008&nn=$USERNAME&pn=1&l=10&cp=R&cl=R&pf=web&at=$AT" --max-time 10 2>&1)
check "Bank search (c=2008)" "$RES" "success"

# ---------------------------------------------------------------
# 9. Payment config (c=2002)
# ---------------------------------------------------------------
RES=$(curl -sk "https://$DOMAIN/api?c=2002&nn=$USERNAME&at=$AT&pf=web" --max-time 10 2>&1)
check "Payment config (c=2002)" "$RES" "status"

# ---------------------------------------------------------------
# 10. Admin CMS login
# ---------------------------------------------------------------
RES=$(curl -sk -X POST "https://staging-admin.sunkr.bet/admin/login/loginODP" \
    -d "username=$USERNAME&password=$PW_MD5" --max-time 10 2>&1)
check "Admin CMS login" "$RES" '"1"'

# ---------------------------------------------------------------
# 11. WebSocket - Slot server
# ---------------------------------------------------------------
echo ""
echo "[WebSocket]"
if command -v node &>/dev/null || docker exec sunwinkr-ws-bridge node --version &>/dev/null 2>&1; then
    for game in slot minigame banca; do
        case $game in
            slot) HOST="game-slot:1844" ;;
            minigame) HOST="game-minigame:1641" ;;
            banca) HOST="banca:2083" ;;
        esac

        RES=$(docker exec sunwinkr-ws-bridge node -e "
            var W=require('ws'),w=new W('ws://$HOST/websocket',['default-protocol']);
            w.on('open',function(){console.log('OK');w.close();process.exit(0)});
            w.on('error',function(e){console.log('FAIL:'+e.message);process.exit(1)});
            setTimeout(function(){console.log('TIMEOUT');process.exit(1)},5000)
        " 2>&1)
        check "WS $game ($HOST)" "$RES" "OK"
    done

    # Test through nginx (path-based)
    for path in "/ws/slot" "/ws/minigame" "/ws/banca"; do
        RES=$(docker exec sunwinkr-ws-bridge node -e "
            var W=require('ws'),w=new W('ws://nginx:80$path',{headers:{Host:'$DOMAIN'}});
            w.on('open',function(){console.log('OK');w.close();process.exit(0)});
            w.on('error',function(e){console.log('FAIL:'+e.message);process.exit(1)});
            setTimeout(function(){console.log('TIMEOUT');process.exit(1)},5000)
        " 2>&1)
        check "WS nginx$path" "$RES" "OK"
    done

    # Test through Cloudflare tunnel (subdomain)
    RES=$(docker exec sunwinkr-ws-bridge node -e "
        var W=require('ws'),w=new W('wss://wbanca.staging-play.sunkr.bet/',{rejectUnauthorized:false});
        w.on('open',function(){console.log('OK');w.close();process.exit(0)});
        w.on('error',function(e){console.log('FAIL:'+e.message);process.exit(1)});
        setTimeout(function(){console.log('TIMEOUT');process.exit(1)},10000)
    " 2>&1)
    check "WSS wbanca (Cloudflare)" "$RES" "OK"
else
    echo -e "  ${YELLOW}SKIP${NC} Node.js not available for WebSocket tests"
fi

# ---------------------------------------------------------------
# 12. Container status
# ---------------------------------------------------------------
echo ""
echo "[Containers]"
for svc in portal-api backend-api payment-api game-slot game-minigame banca nginx cloudflared; do
    STATUS=$(docker ps --filter name=sunwinkr-$svc --format "{{.Status}}" 2>/dev/null | head -1)
    if echo "$STATUS" | grep -q "Up"; then
        check "Container $svc" "$STATUS" "Up"
    else
        check "Container $svc" "${STATUS:-NOT RUNNING}" "Up"
    fi
done

# ---------------------------------------------------------------
# Summary
# ---------------------------------------------------------------
echo ""
echo "================================"
TOTAL=$((PASS+FAIL))
echo -e "Results: ${GREEN}$PASS passed${NC} / ${RED}$FAIL failed${NC} / $TOTAL total"
echo "================================"

exit $FAIL

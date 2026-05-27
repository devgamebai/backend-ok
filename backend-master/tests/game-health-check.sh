#!/bin/bash
# =============================================================================
# GAME HEALTH CHECK — Comprehensive test suite for all games
# Run after any build/deploy to verify everything works.
# Usage: bash tests/game-health-check.sh
# =============================================================================

PASS=0
FAIL=0
WARN=0

pass() { echo "  ✅ $1"; ((PASS++)); }
fail() { echo "  ❌ $1"; ((FAIL++)); }
warn() { echo "  ⚠️  $1"; ((WARN++)); }

echo "=========================================="
echo " GAME HEALTH CHECK"
echo " $(date)"
echo "=========================================="

# =============================================================================
# 1. CONTAINER STATUS
# =============================================================================
echo ""
echo "📦 1. Container Status"
echo "------------------------------------------"

for svc in hazelcast mysql mongodb rabbitmq redis portal-api backend-api game-minigame game-slot game-poker game-bacay game-lieng game-binh game-sam game-tlmn game-caro game-cotuong game-xizach game-coup game-pokertour game-xocdia game-xocdiatulinh game-thirdparty ws-bridge banca; do
    status=$(docker ps --format "{{.Status}}" --filter "name=sunwinkr-$svc" 2>/dev/null | head -1)
    if [ -z "$status" ]; then
        fail "$svc: NOT RUNNING"
    elif echo "$status" | grep -qi "restart"; then
        fail "$svc: RESTARTING ($status)"
    else
        pass "$svc: $status"
    fi
done

# =============================================================================
# 2. PORTAL LOGIN
# =============================================================================
echo ""
echo "🔐 2. Portal Login"
echo "------------------------------------------"

login_result=$(curl -s --max-time 10 "http://localhost:80/api?c=3&un=nguoinaodo&pw=e10adc3949ba59abbe56e057f20f883e&cp=R&cl=R&pf=web&at=" 2>&1)
if echo "$login_result" | grep -q '"success":true'; then
    pass "Login: success"
else
    error_code=$(echo "$login_result" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode','?'))" 2>/dev/null)
    fail "Login FAILED: errorCode=$error_code"
    echo "       Response: $(echo "$login_result" | head -c 200)"
fi

# =============================================================================
# 3. BACKEND ADMIN LOGIN
# =============================================================================
echo ""
echo "🔑 3. Backend Admin Login"
echo "------------------------------------------"

admin_result=$(docker exec sunwinkr-fe-nginx curl -s --max-time 5 "http://backend-api:19082/api_backend?c=701&un=superadmin&pw=0192023a7bbd73250516f069df18b500" 2>&1)
if echo "$admin_result" | grep -q '"success":true'; then
    pass "Admin login: success"
else
    fail "Admin login FAILED: $(echo "$admin_result" | head -c 200)"
fi

# =============================================================================
# 4. CLASSPATH COMPATIBILITY (critical — source vs precompiled JARs)
# =============================================================================
echo ""
echo "🔗 4. Classpath Compatibility"
echo "------------------------------------------"

for svc in game-minigame game-slot portal-api backend-api; do
    errors=$(docker logs "sunwinkr-$svc" 2>&1 | grep -c "NoSuchMethodError\|NoSuchFieldError\|NoClassDefFoundError\|IncompatibleClassChangeError\|Could not find or load main class")
    if [ "$errors" -eq 0 ]; then
        pass "$svc: 0 classpath errors"
    else
        fail "$svc: $errors classpath errors"
        docker logs "sunwinkr-$svc" 2>&1 | grep "NoSuchMethodError\|NoSuchFieldError\|NoClassDefFoundError\|IncompatibleClassChangeError" | head -2 | while read line; do
            echo "       $line"
        done
    fi
done

# =============================================================================
# 5. MINIGAME — TaiXiu Classic Game Loop
# Issue: precompiled TaiXiuModule GameLoopTask dies silently if source VbeeCommon
# or VinPlayDAL classes override precompiled. ScheduledExecutorService swallows Errors.
# =============================================================================
echo ""
echo "🎲 5. TaiXiu Classic Game Loop"
echo "------------------------------------------"

taixiu_calc=$(docker exec sunwinkr-game-minigame grep "TaiXiuModule.*CalculatingTaiXiuPrize" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$taixiu_calc" ]; then
    ts=$(echo "$taixiu_calc" | awk '{print $1" "$2}')
    pass "CalculatingTaiXiuPrize running (last: $ts)"
else
    fail "TaiXiuModule CalculatingTaiXiuPrize NOT found — game loop is dead"
    echo "       Check: VbeeCommon must use FULL precompiled (no source overlay)"
    echo "       Check: VinPlayDAL must use FULL precompiled"
fi

# Check ServerReadyTask (confirms init() completed)
server_ready=$(docker exec sunwinkr-game-minigame grep "SERVER READY TASK" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$server_ready" ]; then
    pass "TaiXiuModule init() completed (ServerReadyTask ran)"
else
    fail "TaiXiuModule init() may have failed"
fi

# =============================================================================
# 6. MINIGAME — TaiXiu MD5 Game Loop
# =============================================================================
echo ""
echo "🎲 6. TaiXiu MD5 Game Loop"
echo "------------------------------------------"

md5_calc=$(docker exec sunwinkr-game-minigame grep "TaiXiuMD5Module.*CalculatingTaiXiuPrize" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$md5_calc" ]; then
    pass "TaiXiuMD5 CalculatingTaiXiuPrize running"
else
    fail "TaiXiuMD5 CalculatingTaiXiuPrize NOT running"
fi

# =============================================================================
# 6b. MINIGAME — BauCua, CaoThap, MiniPoker
# =============================================================================
echo ""
echo "🎲 6b. BauCua / CaoThap / MiniPoker"
echo "------------------------------------------"

for module in "BauCuaModule:BauCua" "CaoThapModule:CaoThap" "MiniPokerModule:MiniPoker"; do
    cls=$(echo "$module" | cut -d: -f1)
    name=$(echo "$module" | cut -d: -f2)
    init=$(docker exec sunwinkr-game-minigame grep -i "$cls init" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
    if [ -n "$init" ]; then
        pass "$name: initialized"
    else
        fail "$name: NOT initialized"
    fi
done

# BauCua game loop
baucua_ready=$(docker exec sunwinkr-game-minigame grep "BauCuaModule.*ServerReadyTask" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$baucua_ready" ]; then
    pass "BauCua: game loop scheduled"
else
    warn "BauCua: ServerReadyTask not seen"
fi

# CaoThap phien (round)
caothap=$(docker exec sunwinkr-game-minigame grep "CAO THAP phien" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$caothap" ]; then
    phien=$(echo "$caothap" | grep -oP 'phien: \d+' | grep -oP '\d+')
    pass "CaoThap: active (phien: $phien)"
else
    warn "CaoThap: no round data seen"
fi

# MiniPoker X2 task
minipoker=$(docker exec sunwinkr-game-minigame grep "MiniPoker Ngay X2" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$minipoker" ]; then
    pass "MiniPoker: X2 event scheduled"
else
    warn "MiniPoker: X2 event not seen"
fi

# Check minigame handlers for client commands
for cmd_game in "2000:TaiXiu" "4000:MiniPoker" "5000:BauCua" "6000:CaoThap" "28000:Sicbo"; do
    cmd=$(echo "$cmd_game" | cut -d: -f1)
    game=$(echo "$cmd_game" | cut -d: -f2)
    calls=$(docker exec sunwinkr-game-minigame grep "cmdId: $cmd " /app/logs/game-minigame/debug.log 2>/dev/null | wc -l)
    if [ "$calls" -gt 0 ]; then
        pass "$game (cmd $cmd): $calls client requests processed"
    else
        warn "$game (cmd $cmd): no client requests seen (no players yet?)"
    fi
done

# =============================================================================
# 7. MINIGAME — Sicbo Game Loop
# Issue: TaiXiuSicboModule game loop must run for Sicbo. Requires full precompiled
# runtime JARs. Client connects at cmd 28000 directly (no CMD_REDIRECT needed).
# =============================================================================
echo ""
echo "🎲 7. Sicbo Game Loop"
echo "------------------------------------------"

sicbo_bots=$(docker exec sunwinkr-game-minigame grep "SicBo BOTS VIN" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$sicbo_bots" ]; then
    count=$(echo "$sicbo_bots" | grep -oP '\d+$')
    pass "Sicbo bots active ($count bots)"
else
    fail "Sicbo bots NOT running"
fi

sicbo_dice=$(docker exec sunwinkr-game-minigame grep "SicBo GENERATE RESULT DICES" /app/logs/game-minigame/debug.log 2>/dev/null | tail -1)
if [ -n "$sicbo_dice" ]; then
    pass "Sicbo generating dice results"
else
    warn "Sicbo dice generation not seen (may need more time)"
fi

# =============================================================================
# 8. MINIGAME — Port Listening
# Issue: if init fails, port 1641 never opens. ws-bridge gets ECONNREFUSED.
# =============================================================================
echo ""
echo "🔌 8. Minigame Port"
echo "------------------------------------------"

mini_port=$(docker exec sunwinkr-game-minigame cat /proc/net/tcp6 2>/dev/null | awk '{print $2}' | grep -c "0669")
if [ "$mini_port" -gt 0 ]; then
    pass "Port 1641 listening ($mini_port connections)"
else
    fail "Port 1641 NOT listening — minigame server failed to start"
fi

# =============================================================================
# 9. SLOT — Handler Registration
# Issue: source Games enum must have DRAGONBALL, THAN_DEN, etc. for slot modules.
# Entrypoint.sh swaps Games.class for non-minigame containers.
# Must have 17 handlers (2000-19000 range).
# =============================================================================
echo ""
echo "🎰 9. Slot Handler Registration"
echo "------------------------------------------"

handler_count=$(docker logs sunwinkr-game-slot 2>&1 | grep -c "Registered slot handler")
if [ "$handler_count" -ge 17 ]; then
    pass "$handler_count handlers registered (expected 17)"
else
    fail "Only $handler_count handlers registered (expected 17)"
    echo "       Check: Games enum must have DRAGONBALL, THAN_DEN, etc."
    echo "       Check: entrypoint.sh Games.class swap for slot container"
fi

# Check Games swap
games_swap=$(docker logs sunwinkr-game-slot 2>&1 | grep "Swapped Games.class" | head -1)
if [ -n "$games_swap" ]; then
    pass "Games.class swapped to source version"
else
    fail "Games.class NOT swapped — slot modules will miss DRAGONBALL etc."
fi

# Check GameConfig conflict resolution
gameconfig=$(docker logs sunwinkr-game-slot 2>&1 | grep "Removed.*GameConfig\|GameConfig.*SlotMachine" | head -1)
if [ -n "$gameconfig" ]; then
    pass "GameConfig conflict resolved (Minigame's removed)"
else
    warn "GameConfig conflict resolution not logged"
fi

# =============================================================================
# 10. SLOT — Specific Games
# Issue: Pirate King uses CMD_REDIRECT 12xxx→7xxx (ThanTaiModule).
# Than Tai at handler 7000. isUserJackpot must be default method.
# =============================================================================
echo ""
echo "🎰 10. Slot Specific Games"
echo "------------------------------------------"

for handler_game in "2000:KhoBau" "7000:ThanTai" "6000:ChiemTinh" "4000:Avenger" "12000:KhoBau-PK" "16000:ChiemTinh-Bikini" "9000:DragonBall" "19000:ThanDen"; do
    handler=$(echo "$handler_game" | cut -d: -f1)
    game=$(echo "$handler_game" | cut -d: -f2)
    if docker logs sunwinkr-game-slot 2>&1 | grep -q "Registered slot handler $handler"; then
        pass "Handler $handler → $game"
    else
        fail "Handler $handler → $game NOT registered"
    fi
done

# Check Than Tai isUserJackpot error
thantai_err=$(docker logs sunwinkr-game-slot 2>&1 | grep "isUserJackpot" | head -1)
if [ -n "$thantai_err" ]; then
    fail "Than Tai isUserJackpot error — UserService needs default method"
else
    pass "Than Tai: no isUserJackpot errors"
fi

# =============================================================================
# 11. SLOT — RabbitMQ Connectivity
# Issue: rabbitmq_config.xml uses properties format, entrypoint must fix credentials.
# =============================================================================
echo ""
echo "🐰 11. Slot RabbitMQ"
echo "------------------------------------------"

rmq_errors=$(docker logs sunwinkr-game-slot 2>&1 | grep -c "ACCESS_REFUSED")
if [ "$rmq_errors" -eq 0 ]; then
    pass "0 RabbitMQ auth errors"
else
    fail "$rmq_errors RabbitMQ ACCESS_REFUSED errors"
    echo "       Check: entrypoint.sh must fix rabbitmq_config.xml credentials"
fi

# =============================================================================
# 12. WS-BRIDGE — Routing & Redirects
# Issue: Pirate King (12xxx) → ThanTai (7xxx) via CMD_REDIRECT
# Sicbo (28xxx) → direct to TaiXiuSicboModule (no redirect needed with precompiled)
# =============================================================================
echo ""
echo "🌉 12. WS-Bridge Config"
echo "------------------------------------------"

bridge_redirect=$(docker exec sunwinkr-ws-bridge grep "12003.*12000.*7000" /app/bridge.js 2>/dev/null)
if [ -n "$bridge_redirect" ]; then
    pass "Pirate King CMD_REDIRECT: 12xxx → 7xxx"
else
    fail "Pirate King CMD_REDIRECT missing"
fi

bridge_sicbo=$(docker exec sunwinkr-ws-bridge grep "28000.*DISABLED\|// 28000" /app/bridge.js 2>/dev/null | head -1)
if [ -n "$bridge_sicbo" ]; then
    pass "Sicbo: direct to handler 28000 (no redirect)"
else
    warn "Sicbo CMD_REDIRECT config may need review"
fi

# Check ws-bridge for ECONNREFUSED
bridge_refused=$(docker logs sunwinkr-ws-bridge --since 300s 2>&1 | grep -c "ECONNREFUSED")
if [ "$bridge_refused" -eq 0 ]; then
    pass "0 ECONNREFUSED errors (last 5 min)"
else
    warn "$bridge_refused ECONNREFUSED errors (last 5 min)"
fi

# =============================================================================
# 13. BANCA — Redis & MySQL Connectivity
# Issue: config.json passwords must match .env values.
# Release config at BanCaLiteNet/out/config.json is the one used at runtime.
# =============================================================================
echo ""
echo "🐟 13. BanCa"
echo "------------------------------------------"

banca_status=$(docker ps --format "{{.Status}}" --filter "name=sunwinkr-banca" 2>/dev/null | head -1)
if echo "$banca_status" | grep -q "Up"; then
    pass "BanCa running: $banca_status"
else
    fail "BanCa NOT running"
fi

banca_redis=$(docker logs sunwinkr-banca --since 300s 2>&1 | grep -c "Redis.*error\|UnableToConnect.*redis")
if [ "$banca_redis" -eq 0 ]; then
    pass "BanCa Redis: no connection errors"
else
    fail "BanCa Redis: $banca_redis connection errors"
    echo "       Check: banca/BanCaLiteNet/out/config.json redis-password"
fi

banca_mysql=$(docker logs sunwinkr-banca --since 300s 2>&1 | grep -c "MySql.*Authentication\|Access denied")
if [ "$banca_mysql" -eq 0 ]; then
    pass "BanCa MySQL: no auth errors"
else
    fail "BanCa MySQL: $banca_mysql auth errors"
    echo "       Check: banca/BanCaLiteNet/out/config.json mysql-connection pwd"
fi

# =============================================================================
# 14. MONGODB — Authentication
# Issue: MongoSecurityException during MGRoomTaiXiu init kills TaiXiu module.
# =============================================================================
echo ""
echo "🍃 14. MongoDB"
echo "------------------------------------------"

mongo_auth=$(docker logs sunwinkr-game-minigame 2>&1 | grep -c "MongoSecurityException\|Authentication failed.*mongo")
if [ "$mongo_auth" -eq 0 ]; then
    pass "No MongoDB auth errors"
else
    fail "$mongo_auth MongoDB auth errors"
fi

# =============================================================================
# 15. VBEECOMMON — JAR Composition Check
# Issue: source-built VbeeCommon classes break precompiled Minigame TaiXiu.
# VbeeCommon must use FULL PRECOMPILED + only genuinely new source classes.
# =============================================================================
echo ""
echo "📦 15. VbeeCommon Composition"
echo "------------------------------------------"

# Check UserModel is precompiled (32-param only, no backward-compat 28-param)
docker cp sunwinkr-game-minigame:/app/libs/runtime/VbeeCommon-1.0.jar /tmp/check-vbee.jar 2>/dev/null
um_constructors=$(cd /tmp && jar xf check-vbee.jar com/vinplay/vbee/common/models/UserModel.class 2>/dev/null && javap -p com/vinplay/vbee/common/models/UserModel.class 2>&1 | grep -c "UserModel(")
if [ "$um_constructors" -eq 2 ]; then
    pass "UserModel: precompiled (2 constructors: no-arg + 32-param)"
elif [ "$um_constructors" -eq 3 ]; then
    fail "UserModel: source-built (3 constructors with backward-compat) — breaks Minigame"
else
    warn "UserModel: $um_constructors constructors (unexpected)"
fi

# Check BaseResponse exists (source-only class)
br_exists=$(cd /tmp && jar tf check-vbee.jar 2>/dev/null | grep -c "BaseResponse.class")
if [ "$br_exists" -gt 0 ]; then
    pass "BaseResponse.class present (source-only addition)"
else
    fail "BaseResponse.class missing — Portal APIs will fail"
fi

# =============================================================================
# SUMMARY
# =============================================================================
echo ""
echo "=========================================="
echo " RESULTS: $PASS passed, $FAIL failed, $WARN warnings"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
    echo " ❌ HEALTH CHECK FAILED"
    exit 1
else
    echo " ✅ ALL CHECKS PASSED"
    exit 0
fi

#!/bin/bash
set -e

# =============================================================================
# Entrypoint for all Java containers (API servers + game servers)
# Runs BEFORE Java starts. All config must be valid before exec.
# =============================================================================

SERVICE_NAME=$(echo "$@" | grep -oP 'service\.name=\K[a-zA-Z0-9-]+' || echo "unknown")
echo "[$SERVICE_NAME] Starting entrypoint..."

# =============================================================================
# 1. DATABASE CONFIG TEMPLATING
# =============================================================================

for propfile in $(find /app -name "db_pool.properties" 2>/dev/null); do
  sed -i 's|//[^:]*:3306|//mysql:3306|g' "$propfile"
  sed -i "s|\.user=.*|.user=${MYSQL_USER}|g" "$propfile"
  sed -i "s|\.password=.*|.password=${MYSQL_PASSWORD}|g" "$propfile"
  grep -q "useSSL=false" "$propfile" || sed -i "s|characterEncoding=UTF-8|characterEncoding=UTF-8\&useSSL=false\&allowPublicKeyRetrieval=true|g" "$propfile"
done

for propfile in $(find /app -name "mongo.properties" 2>/dev/null); do
  sed -i "s|^host=.*|host=mongodb|g" "$propfile"
  sed -i "s|^hostslave1=.*|hostslave1=mongodb|g" "$propfile"
  sed -i "s|^hostslave2=.*|hostslave2=mongodb|g" "$propfile"
  sed -i "s|^username=.*|username=${MONGO_USER}|g" "$propfile"
  sed -i "s|^password=.*|password=${MONGO_PASSWORD}|g" "$propfile"
done

for propfile in $(find /app -name "rmq.properties" 2>/dev/null); do
  sed -i "s|^rmq_server=.*|rmq_server=rabbitmq|g" "$propfile"
  sed -i "s|^rmq_username=.*|rmq_username=${RABBITMQ_USER}|g" "$propfile"
  sed -i "s|^rmq_password=.*|rmq_password=${RABBITMQ_PASSWORD}|g" "$propfile"
done

for xmlfile in $(find /app -name "rabbitmq_config.xml" 2>/dev/null); do
  sed -i "s|<server_addr>.*</server_addr>|<server_addr>rabbitmq</server_addr>|g" "$xmlfile"
  sed -i "s|<user>.*</user>|<user>${RABBITMQ_USER}</user>|g" "$xmlfile"
  sed -i "s|<pass>.*</pass>|<pass>${RABBITMQ_PASSWORD}</pass>|g" "$xmlfile"
  sed -i "s|^rmq_username=.*|rmq_username=${RABBITMQ_USER}|g" "$xmlfile"
  sed -i "s|^rmq_password=.*|rmq_password=${RABBITMQ_PASSWORD}|g" "$xmlfile"
  sed -i "s|^rmq_server=.*|rmq_server=rabbitmq|g" "$xmlfile"
done

for propfile in $(find /app -name "hazelcast.properties" 2>/dev/null); do
  sed -i "s|^address=.*|address=hazelcast|g" "$propfile"
  sed -i 's|//[^:]*:5701|//hazelcast:5701|g' "$propfile"
  sed -i "s|^group_name=.*|group_name=${HAZELCAST_GROUP}|g" "$propfile"
  sed -i "s|^group_password=.*|group_password=${HAZELCAST_PASSWORD}|g" "$propfile"
done

# =============================================================================
# 2. API SERVER CONFIG SETUP
# =============================================================================

API_NAME=$(echo "$@" | grep -oP 'service\.name=\K(portal-api|backend-api|vbee)' || true)
if [ -n "$API_NAME" ]; then
  case "$API_NAME" in
    portal-api) API_DIR="/app/api/VinPlayPortal" ;;
    backend-api) API_DIR="/app/api/VinPlayBackend" ;;
    vbee) API_DIR="/app/api/vbee" ;;
  esac
  if [ -n "$API_DIR" ] && [ -d "$API_DIR/config" ]; then
    mkdir -p /app/config
    # SUN-1248: was `cp -n` — that preserved a stale /app/config/ on every
    # boot, which silently lost queue declarations added in newer versions
    # of /api/<svc>/config/rabbitmq_config.xml. Last incident was vbee
    # missing queue_log_gsc_bets_async after a redeploy → bet history
    # froze. Force-overwrite is the correct semantics: per-service
    # canonical config at /api/<svc>/config/ wins on every boot.
    cp -f "$API_DIR/config/"* /app/config/ 2>/dev/null || true
    echo "[$SERVICE_NAME] Merged $API_DIR/config -> /app/config (force-overwrite)"
  fi
fi

# =============================================================================
# 3. GAME SERVER CONFIG SETUP
# =============================================================================

GAME_NAME=$(echo "$@" | grep -oP 'service\.name=game-\K[a-zA-Z0-9]+' || true)
if [ -n "$GAME_NAME" ]; then
  # Resolve game directory
  GAME_DIR="/app/game/$GAME_NAME"
  if [ ! -d "$GAME_DIR/config" ]; then
    CAP_NAME="$(echo "$GAME_NAME" | sed 's/./\U&/')"
    [ -d "/app/game/$CAP_NAME/config" ] && GAME_DIR="/app/game/$CAP_NAME"
  fi
  case "$GAME_NAME" in
    bacay) GAME_DIR="/app/game/bacayServer" ;;
    xocdiatulinh) GAME_DIR="/app/game/xocdiatulinh" ;;
  esac

  # Link config/
  if [ -d "$GAME_DIR/config" ] && [ -f "$GAME_DIR/config/core.xml" ] && [ ! -f "/app/config/core.xml" ]; then
    rm -rf /app/config 2>/dev/null || true
    ln -sf "$GAME_DIR/config" /app/config
    echo "[$SERVICE_NAME] Linked config -> $GAME_DIR/config"
  fi
  # Link conf/
  if [ -d "$GAME_DIR/conf" ]; then
    [ ! -d "/app/conf" ] && ln -sf "$GAME_DIR/conf" /app/conf
    [ ! -d "/conf" ] && ln -sf "$GAME_DIR/conf" /conf
    echo "[$SERVICE_NAME] Linked conf -> $GAME_DIR/conf"
  fi

  # =========================================================================
  # 3a. CLUSTER.PROPERTIES VALIDATION
  # GameUtils static initializer requires these keys. Missing keys cause
  # NumberFormatException that permanently breaks the class.
  # =========================================================================
  CLUSTER_PROPS="/app/conf/cluster.properties"
  if [ -f "$CLUSTER_PROPS" ]; then
    for key in isCheat isHuVang isBot isLog dev_mod; do
      if ! grep -q "^${key}=" "$CLUSTER_PROPS"; then
        echo "${key}=0" >> "$CLUSTER_PROPS"
        echo "[$SERVICE_NAME] WARNING: Added missing key '${key}=0' to cluster.properties"
      fi
    done
    sed -i 's/^\([a-zA-Z_]*\) *= */\1=/g' "$CLUSTER_PROPS"
  fi

  # =========================================================================
  # 3b. RABBITMQ CONFIG FILE
  # If rabbitmq_config.xml is missing, create from rmq.properties.
  # =========================================================================
  if [ -f "/app/config/rmq.properties" ] && [ ! -f "/app/config/rabbitmq_config.xml" ]; then
    cp /app/config/rmq.properties /app/config/rabbitmq_config.xml
    echo "[$SERVICE_NAME] Created rabbitmq_config.xml from rmq.properties"
  fi

  # =========================================================================
  # 3c. GAMECONFIG CLASSPATH CONFLICT RESOLUTION
  # Both Minigame.jar and SlotMachine-1.0.jar contain game.GameConfig.GameConfig
  # with incompatible fields. Remove the wrong one per-container at startup.
  # TODO: Deduplicate GameConfig into a single shared module to eliminate this.
  # =========================================================================
  case "$GAME_NAME" in
    slot)
      zip -qd /app/libs/app/Minigame.jar "game/GameConfig/*" 2>/dev/null && \
        echo "[$SERVICE_NAME] Removed Minigame GameConfig (using SlotMachine's)" || true
      ;;
    minigame)
      zip -qd /app/libs/app/SlotMachine-1.0.jar "game/GameConfig/*" 2>/dev/null && \
        echo "[$SERVICE_NAME] Removed SlotMachine GameConfig (using Minigame's)" || true
      ;;
  esac

  # =========================================================================
  # 3d. TCP PORT CONFIGURATION
  # Each game server needs a unique TCP port in server.xml.
  # =========================================================================
  case "$GAME_NAME" in
    slot)           GAME_TCP_PORT=1843 ;;
    minigame)       GAME_TCP_PORT=1643 ;;
    xocdia)         GAME_TCP_PORT=2343 ;;
    xocdiatulinh)   GAME_TCP_PORT=1650 ;;
    poker)          GAME_TCP_PORT=1743 ;;
    pokertour)      GAME_TCP_PORT=1738 ;;
    tlmn)           GAME_TCP_PORT=2143 ;;
    lieng)          GAME_TCP_PORT=1543 ;;
    binh)           GAME_TCP_PORT=1243 ;;
    baicao)         GAME_TCP_PORT=1143 ;;
    bacay)          GAME_TCP_PORT=1043 ;;
    sam)            GAME_TCP_PORT=1943 ;;
    caro)           GAME_TCP_PORT=1343 ;;
    cotuong)        GAME_TCP_PORT=1443 ;;
    coup)           GAME_TCP_PORT=2443 ;;
    xizach)         GAME_TCP_PORT=2243 ;;
    *) GAME_TCP_PORT="" ;;
  esac

  if [ -n "$GAME_TCP_PORT" ]; then
    for serverxml in $(find /app -name "server.xml" 2>/dev/null); do
      sed -i "s|port=\"[0-9]*\" type=\"TCP\"|port=\"$GAME_TCP_PORT\" type=\"TCP\"|g" "$serverxml"
    done
    echo "[$SERVICE_NAME] TCP port set to $GAME_TCP_PORT"
  fi
fi

# =============================================================================
# 4. STARTUP
# =============================================================================
echo "[$SERVICE_NAME] Config validation complete. Starting Java..."
eval exec "$@"

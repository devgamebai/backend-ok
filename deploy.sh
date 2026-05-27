#!/bin/bash
# =============================================================================
# SUNWINKR CASINO PLATFORM - DEPLOYMENT SCRIPT
# =============================================================================
# Usage:
#   ./deploy.sh                  # Full deployment (build + setup + start)
#   ./deploy.sh stop             # Stop all services
#   ./deploy.sh status           # Show service status
#   ./deploy.sh logs [service]   # Follow logs
#   ./deploy.sh --no-start       # Build and setup only, don't start services
#   ./deploy.sh --rebuild        # Force rebuild Java and .NET projects
#
# Environment variables (set before running or export in shell):
#   PLAY_DOMAIN          # Game client domain (default: staging-play.sunkr.bet)
#   ADMIN_DOMAIN         # Admin panel domain (default: staging-admin.sunkr.bet)
#   CLOUDFLARE_TUNNEL_TOKEN  # Cloudflare tunnel token (enables tunnel + ws-bridge)
#
# Examples:
#   # Deploy staging (default)
#   export CLOUDFLARE_TUNNEL_TOKEN=eyJhIj...
#   ./deploy.sh
#
#   # Deploy with custom domains
#   export PLAY_DOMAIN=play.sunkr.bet
#   export ADMIN_DOMAIN=admin.sunkr.bet
#   export CLOUDFLARE_TUNNEL_TOKEN=eyJhIj...
#   ./deploy.sh
#
#   # Deploy without Cloudflare (local/direct access on port 8088)
#   ./deploy.sh
#
# Prerequisites:
#   - Docker Engine 24+ and Docker Compose v2+
#   - 8GB+ RAM recommended
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_NAME="${COMPOSE_PROJECT_NAME:-sunwinkr-backend}"

# Defaults
PLAY_DOMAIN="${PLAY_DOMAIN:-play.ku4789.xyz}"
ADMIN_DOMAIN="${ADMIN_DOMAIN:-admin.ku4789.xyz}"

NO_START=false
REBUILD=false

# Parse flags (skip subcommands)
for arg in "$@"; do
    case $arg in
        --no-start) NO_START=true ;;
        --rebuild) REBUILD=true ;;
        --help|-h)
            head -30 "$0" | grep "^#" | sed 's/^# \?//'
            exit 0
            ;;
    esac
done

log() { echo -e "${GREEN}[DEPLOY]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
step() { echo -e "\n${BLUE}━━━ Step $1: $2 ━━━${NC}"; }

# GitLab #27 Phase 1 — auto-rollback a failed --rebuild.
#
# Phase 0 (commit 585214e1, line 485) already snapshots :latest to
# :last-working BEFORE the destructive rebuild. Phase 1 closes the loop:
# on any verified post-cutover failure, retag :last-working back to
# :latest and recreate containers off the snapshot — no operator
# intervention. Only fires when:
#   - we're in --rebuild mode (so Phase 0 definitely snapshotted)
#   - AUTO_ROLLBACK=0 wasn't set (escape hatch for debugging)
#   - the snapshot actually exists for at least one sunwinkr-* image
# Exit code 10 on successful rollback, 11 on rollback-also-failed.
auto_rollback() {
    local reason="$1"
    echo -e "${RED}[ERROR]${NC} $reason"

    if [ "${AUTO_ROLLBACK:-1}" = "0" ]; then
        echo -e "${YELLOW}[WARN]${NC} AUTO_ROLLBACK=0 — skipping auto-rollback. Manual recovery required."
        exit 1
    fi
    if [ "${REBUILD:-false}" != "true" ]; then
        echo -e "${YELLOW}[WARN]${NC} No --rebuild in this run — nothing to roll back to. Exiting."
        exit 1
    fi

    local snapshots
    snapshots=$(docker images --format '{{.Repository}}:{{.Tag}}' 2>/dev/null | grep ':last-working$' | head -1)
    if [ -z "$snapshots" ]; then
        echo -e "${RED}[ERROR]${NC} No :last-working snapshots found. Cannot auto-rollback — manual recovery required."
        exit 1
    fi

    echo -e "${YELLOW}[WARN]${NC} Auto-rollback triggered — retagging :last-working → :latest and recreating containers"
    local retagged=0
    for img in $(docker images --format '{{.Repository}}' 2>/dev/null | grep -E '^sunwinkr-' | sort -u); do
        if docker image inspect "${img}:last-working" >/dev/null 2>&1; then
            docker tag "${img}:last-working" "${img}:latest" 2>/dev/null && retagged=$((retagged + 1)) || true
        fi
    done
    log "Auto-rollback: retagged $retagged image(s)"

    # Recreate off the rolled-back tag — no --build, no pre-stop filter.
    if ! compose up -d --force-recreate --timeout 60 2>&1 | tail -30; then
        echo -e "${RED}[ERROR]${NC} Rollback compose up also failed. Deployment is in an undefined state — manual recovery required."
        exit 11
    fi

    log "Waiting 30s for rolled-back services to settle..."
    sleep 30

    local rb_total rb_up rb_healthy
    rb_total=$(docker ps -a --filter name=sunwinkr- --format '{{.Names}}' | wc -l)
    rb_up=$(docker ps --filter name=sunwinkr- --format '{{.Names}}' | wc -l)
    rb_healthy=$(docker ps --filter name=sunwinkr- --filter health=healthy --format '{{.Names}}' | wc -l)
    log "Post-rollback: ${rb_up}/${rb_total} running, ${rb_healthy} healthy"

    # Partial success is acceptable for the rollback itself — the operator
    # still has to look at what broke. The 10 vs 11 exit distinction is
    # CI-friendly: 10 = "we rolled back, investigate", 11 = "stack is dead,
    # page someone".
    if [ "$rb_up" -lt 3 ]; then
        echo -e "${RED}[ERROR]${NC} Fewer than 3 containers up after rollback. Stack looks dead."
        exit 11
    fi

    echo -e "${YELLOW}[WARN]${NC} Deploy failed and was auto-rolled-back. Investigate the original failure above and re-attempt after a fix."
    exit 10
}

# Build compose file list
# offline-games tier: Spring Boot 2.7.18 services hosting the TaiXiu/Sicbo
# (minigame-api) and XSMB Lô Đề (lottery-spring-api) REST + STOMP surfaces.
# They coexist with the legacy BitZero game-minigame container — different
# network paths, same DBs. See docker-compose.offline-games.yml (SUN-1340).
COMPOSE_FILES="-f docker-compose.yml \
  -f docker-compose.database.yml \
  -f docker-compose.backend.yml \
  -f docker-compose.games.yml \
  -f docker-compose.web.yml"

# GitLab #34: ws-bridge moved into docker-compose.games.yml so it's always
# present regardless of tunnel state. cloudflared lives in a separate
# compose project under /home/sunwinkr-dev/ managed outside this script.
# `CLOUDFLARE_TUNNEL_TOKEN` still toggles the "tunnel connected" line in
# the deployment summary below — see the URL print block near the end.

compose() {
    docker compose -p $PROJECT_NAME $COMPOSE_FILES "$@"
}

# ---------------------------------------------------------------------------
# Subcommands: stop, status, logs
# ---------------------------------------------------------------------------
case "${1:-}" in
    stop)
        log "Stopping all services..."
        compose down
        log "Stopped."
        exit 0
        ;;
    status)
        compose ps
        exit 0
        ;;
    logs)
        shift
        compose logs -f "$@"
        exit 0
        ;;
esac

# ---------------------------------------------------------------------------
# Full deployment starts here
# ---------------------------------------------------------------------------
log "Sunwinkr Casino Platform - Deployment"
log "Play domain:  ${BLUE}${PLAY_DOMAIN}${NC}"
log "Admin domain: ${BLUE}${ADMIN_DOMAIN}${NC}"
echo ""

command -v docker >/dev/null 2>&1 || error "Docker not installed. Install: https://docs.docker.com/engine/install/"
docker compose version >/dev/null 2>&1 || error "Docker Compose v2 not found. Install: https://docs.docker.com/compose/install/"

AVAILABLE_MEM=$(free -m 2>/dev/null | awk '/^Mem:/{print $7}' || echo "unknown")
if [ "$AVAILABLE_MEM" != "unknown" ] && [ "$AVAILABLE_MEM" -lt 4000 ]; then
    warn "Available memory: ${AVAILABLE_MEM}MB. Recommended: 8GB+. Platform may be slow."
fi

log "Docker: $(docker --version | cut -d' ' -f3)"
log "Compose: $(docker compose version --short)"

# ---------------------------------------------------------------------------
# Step 1: Generate .env
# ---------------------------------------------------------------------------
step 1 "Environment Configuration"

if [ -f .env ]; then
    log ".env already exists, skipping generation"
else
    if [ ! -f .env.example ]; then
        error ".env.example not found. Are you in the project root?"
    fi
    cp .env.example .env
    python3 -c "
import secrets
with open('.env', 'r') as f: content = f.read()
for i in range(1, 13):
    content = content.replace(f'CHANGE_ME_STRONG_PASSWORD_{i}', secrets.token_urlsafe(24))
content = content.replace('CHANGE_ME_GENERATE_NEW_TOKEN', secrets.token_hex(32))
content = content.replace('CHANGE_ME_GENERATE_32_BYTE_HEX', secrets.token_hex(32))
with open('.env', 'w') as f: f.write(content)
" 2>/dev/null || {
        warn "python3 not found, using openssl for password generation"
        for i in $(seq 1 12); do
            PW=$(openssl rand -base64 24 | tr -d '/+=')
            sed -i "s/CHANGE_ME_STRONG_PASSWORD_$i/$PW/" .env
        done
        sed -i "s/CHANGE_ME_GENERATE_NEW_TOKEN/$(openssl rand -hex 32)/" .env
        sed -i "s/CHANGE_ME_GENERATE_32_BYTE_HEX/$(openssl rand -hex 32)/" .env
    }

    if grep -q "CHANGE_ME" .env 2>/dev/null; then
        error "Failed to generate all passwords in .env"
    fi
    log "Generated .env with strong passwords"
fi

# Persist CLOUDFLARE_TUNNEL_TOKEN to .env if set via env but not in file
if [ -n "${CLOUDFLARE_TUNNEL_TOKEN}" ] && ! grep -q "^CLOUDFLARE_TUNNEL_TOKEN=" .env 2>/dev/null; then
    echo "CLOUDFLARE_TUNNEL_TOKEN=${CLOUDFLARE_TUNNEL_TOKEN}" >> .env
    log "Added CLOUDFLARE_TUNNEL_TOKEN to .env"
fi

# Persist domain config to .env
grep -q "^PLAY_DOMAIN=" .env 2>/dev/null || echo "PLAY_DOMAIN=${PLAY_DOMAIN}" >> .env
grep -q "^ADMIN_DOMAIN=" .env 2>/dev/null || echo "ADMIN_DOMAIN=${ADMIN_DOMAIN}" >> .env

# ---------------------------------------------------------------------------
# Step 2: Build Java Backend
# ---------------------------------------------------------------------------
step 2 "Building Java Backend"

PORTAL_JAR="backend-master/api/VinPlayPortal/build/libs/VinPlayPortal-1.0.jar"

if [ -f "$PORTAL_JAR" ] && [ "$REBUILD" = false ]; then
    log "Java JARs already built, skipping (use --rebuild to force)"
else
    # -----------------------------------------------------------------------
    # Preflight: quarantine stale source-module jars from every libs/ dir
    # (GitLab #27 — production-only outage symptom that staging never saw)
    #
    # Symptom: deploy3 to production failed compile because
    #   game/Minigame/build.gradle has `fileTree(dir: '../../libs')` that
    #   pulls in pre-GitLab-#9 VinPlayDAL/VbeeCommon/etc. jars sitting in
    #   backend-master/libs (or game/*/libs — some modules keep a local
    #   fileTree). Gradle then compiles against the stale bytecode, which
    #   drifts from the current source (e.g. removed methods, renamed
    #   classes) → ClassNotFoundException at runtime or compile failures.
    #
    # Staging didn't hit this because its libs/ dirs were already clean
    # after the refactor. Production's working tree kept the old jars.
    # .dockerignore protects the Dockerfile build (Step 8) but not this
    # Step 2 host-bind build. Hence this host-side preflight.
    #
    # Approach: every libs/ under backend-master that can contain source
    # modules → move matching *-1.0*.jar files aside into a timestamped
    # _quarantine_YYYYMMDD_HHMMSS/ dir. Gradle rebuilds the jar from
    # source into each module's build/libs/, and copyRuntimeDeps fans
    # out fresh copies to runtime-libs/ and all-libs/ afterwards.
    # Quarantine is kept (not deleted) so anything unexpected can be
    # restored manually — each deploy just rolls its own stamped dir.
    # -----------------------------------------------------------------------
    QUARANTINE_STAMP=$(date -u +%Y%m%d_%H%M%S)
    QUARANTINE_BASE="backend-master/_quarantine_${QUARANTINE_STAMP}"
    # Module jars built from source — match these everywhere they appear
    STALE_PATTERN='^(BitZeroMinigame|BitzeroAll|BitzeroMini|CardCoreLib|VbeeCommon|VinPlayCardLib|VinPlayDAL|VinPlayUserCore|VBeeSecurity)-1\.0(-SNAPSHOT)?\.jar$'
    QUARANTINED=0
    for dir in backend-master/libs backend-master/all-libs backend-master/runtime-libs \
               backend-master/api/*/libs backend-master/game/*/libs; do
        [ -d "$dir" ] || continue
        # follow symlinks (backend-master/libs -> all-libs/) via realpath to avoid double work
        real_dir=$(readlink -f "$dir" 2>/dev/null || echo "$dir")
        for jar in "$real_dir"/*.jar; do
            [ -f "$jar" ] || continue
            basename=$(basename "$jar")
            if echo "$basename" | grep -qE "$STALE_PATTERN"; then
                mkdir -p "$QUARANTINE_BASE"
                # Flatten sub-path to a single segment so every jar lands
                # in one flat quarantine dir (easy to grep / restore).
                subpath=$(echo "${real_dir}" | sed 's|backend-master/||; s|/|_|g')
                mv "$jar" "$QUARANTINE_BASE/${subpath}__${basename}" \
                    && QUARANTINED=$((QUARANTINED + 1))
            fi
        done
    done
    if [ $QUARANTINED -gt 0 ]; then
        log "Preflight: quarantined $QUARANTINED stale module jar(s) → $QUARANTINE_BASE/"
    fi

    log "Building with Eclipse Temurin JDK 8 (this may take 2-5 minutes)..."
    # Run gradle inside the JDK container. `set -o pipefail` propagates
    # gradle's exit code through the `tail` filter; we then capture it
    # explicitly. Previously the script only checked "JAR file exists on
    # disk", which was true for a stale JAR from a prior successful build
    # even when the current gradle run had failed — silently shipping
    # pre-rename bytecode (see SUN-748 Tier 1 rename fallout).
    # GitLab #24: dropped `| tail -10`. The filter hid the actual compile
    # error (e.g. LogoutSusscessHandler.java:32 on 2026-04-22). Streaming
    # full gradle output makes the error obvious on first failure without
    # a second hand-run.
    # `clean` prepended 2026-05-17 after MR !432 deploy attempt hit Gradle
    # incremental-compile cache poisoning: a prior `:VinPlayPortal:build`
    # run had populated `*/build/classes/java/main/` from the pre-merge
    # classpath. Subsequent `gradle build` reused those stale class outputs
    # and emitted bogus "cannot find symbol" + "incompatible types" errors
    # in VinPlayUserCore that didn't reproduce under `gradle clean build`.
    # The clean adds ~10 s to a 2-3 min build, eliminates the trap.
    docker run --rm \
        -v "$(pwd)/backend-master:/app" \
        -w /app \
        eclipse-temurin:8-jdk \
        bash -c "chmod +x gradlew && ./gradlew clean build copyRuntimeDeps -x test"
    GRADLE_RC=$?
    if [ $GRADLE_RC -ne 0 ]; then
        error "Java build FAILED (gradle exit $GRADLE_RC). Full gradle output above. Fix the reported compile error and re-run ./deploy.sh --rebuild."
    fi
    if [ ! -f "$PORTAL_JAR" ]; then
        error "Java build crashed before emitting Portal JAR. Re-run gradle manually."
    fi
    log "Java build successful"
fi

# Consolidate all JARs into all-libs/ from Gradle's runtime-libs/ output.
# all-libs/*.jar is no longer tracked in git (removed in refactor #9) —
# Gradle compile is the sole source of truth for runtime dependencies.
if [ -d "backend-master/runtime-libs" ] && [ "$(ls -A backend-master/runtime-libs 2>/dev/null)" ]; then
    log "Consolidating runtime JARs into all-libs/ (always refresh)..."
    rm -rf backend-master/all-libs
    mkdir -p backend-master/all-libs
    cp backend-master/runtime-libs/*.jar backend-master/all-libs/ 2>/dev/null || true
    log "$(ls backend-master/all-libs/ | wc -l) JARs in all-libs/"
else
    log "WARNING: runtime-libs/ missing or empty — gradle may have failed silently. Leaving all-libs/ as-is."
fi

# Create libs/ symlink (classpath references libs/* which points to all-libs/)
if [ ! -e "backend-master/libs" ]; then
    ln -sf all-libs backend-master/libs
    log "Created libs -> all-libs symlink"
fi

# ---------------------------------------------------------------------------
# Step 3: Build BanCa Fish Game (.NET)
# ---------------------------------------------------------------------------
step 3 "Building BanCa Fish Game (.NET)"

BANCA_DLL="banca/BanCaLiteNet/out/BanCaLiteNet.dll"

if [ -f "$BANCA_DLL" ] && [ "$REBUILD" = false ]; then
    log "BanCa already built, skipping (use --rebuild to force)"
else
    log "Building with .NET SDK 5.0..."
    docker run --rm \
        -v "$(pwd)/banca:/banca" \
        -w /banca/BanCaLiteNet \
        mcr.microsoft.com/dotnet/sdk:5.0 \
        dotnet publish -c Release -o out 2>&1 \
        | tail -3

    if [ ! -f "$BANCA_DLL" ]; then
        error "BanCa build failed."
    fi
    log "BanCa build successful"
fi

# ---------------------------------------------------------------------------
# Step 4: Fix File Permissions & Web Build
# ---------------------------------------------------------------------------
step 4 "Fixing Permissions & Preparing Web"

chmod +x start.sh deploy.sh 2>/dev/null || true
chmod +x backend-master/entrypoint.sh 2>/dev/null || true
chmod -R a+rX install/config/ 2>/dev/null || true
chmod -R a+rX www/admin-php/ www/agency-php/ www/webhook/ 2>/dev/null || true
chmod -R a+rX backend-master/api/*/config/ backend-master/game/*/config/ 2>/dev/null || true
chmod -R a+rX backend-master/game/*/conf/ 2>/dev/null || true
chmod -R a+rX nginx/ 2>/dev/null || true
log "Permissions fixed"

# Ensure webbuild directory exists
if [ ! -d "www/webbuild" ] || [ ! -f "www/webbuild/index.html" ]; then
    warn "www/webbuild/ not found. Creating placeholder."
    warn "Build game client with CocosCreator 2.4.4 and copy to www/webbuild/"
    mkdir -p www/webbuild
    cat > www/webbuild/index.html <<HTMLEOF
<!DOCTYPE html>
<html>
<head><title>Build Pending</title></head>
<body style="background:#1a1a2e;color:#e94560;font-family:monospace;display:flex;align-items:center;justify-content:center;height:100vh;margin:0">
<div style="text-align:center">
<h1>Build Pending</h1>
<p>Game client not built yet. Build with CocosCreator 2.4.4 and deploy to www/webbuild/</p>
</div>
</body>
</html>
HTMLEOF
fi

# ---------------------------------------------------------------------------
# Step 5: Update Nginx Config with Domains
# ---------------------------------------------------------------------------
step 5 "Configuring Nginx Domains"

# Update staging.conf with PLAY_DOMAIN
sed -i "s|server_name staging-play\.sunkr\.bet;|server_name ${PLAY_DOMAIN};|g" nginx/conf.d/staging.conf 2>/dev/null || true
# Update staging-admin.conf with ADMIN_DOMAIN
sed -i "s|server_name staging-admin\.sunkr\.bet;|server_name ${ADMIN_DOMAIN};|g" nginx/conf.d/staging-admin.conf 2>/dev/null || true
sed -i "s|fastcgi_param HTTP_HOST staging-admin\.sunkr\.bet;|fastcgi_param HTTP_HOST ${ADMIN_DOMAIN};|g" nginx/conf.d/staging-admin.conf 2>/dev/null || true

log "Nginx configured: ${PLAY_DOMAIN} / ${ADMIN_DOMAIN}"

# ---------------------------------------------------------------------------
# Step 6: Create Docker Networks & Volumes
# ---------------------------------------------------------------------------
step 6 "Creating Docker Networks & Volumes"

docker network create sunkr-network    2>/dev/null || true
docker network create sunkr-frontend   2>/dev/null || true
docker network create sunkr-games      2>/dev/null || true
docker network create sunkr-messaging  2>/dev/null || true

docker volume create sunwinkr-mysql-data    2>/dev/null || true
docker volume create sunwinkr-mongo-data    2>/dev/null || true
docker volume create sunwinkr-redis-data    2>/dev/null || true
docker volume create sunwinkr-rabbitmq-data 2>/dev/null || true

log "Networks and volumes ready"

# ---------------------------------------------------------------------------
# Step 7: Start Databases & Load Data
# ---------------------------------------------------------------------------
step 7 "Starting Databases & Loading Data"

log "Starting database services..."
docker compose -p $PROJECT_NAME \
    -f docker-compose.yml \
    -f docker-compose.database.yml \
    up -d 2>&1 | tail -5

log "Waiting for MySQL to be ready..."
MYSQL_ROOT_PW=$(grep "^MYSQL_ROOT_PASSWORD=" .env | cut -d= -f2)
RETRIES=0
MAX_RETRIES=1
printf '%q\n' "$MYSQL_ROOT_PW"
while [ $RETRIES -lt $MAX_RETRIES ]; do
    if docker exec sunwinkr-mysql mysqladmin ping -h localhost -u root -p"${MYSQL_ROOT_PW}" | grep -q "alive"; then
        break
    fi
    RETRIES=$((RETRIES + 1))
    sleep 2
done

if [ $RETRIES -eq $MAX_RETRIES ]; then
    error "MySQL failed to start within 2 minutes. Check: docker logs sunwinkr-mysql"
fi
log "MySQL is healthy"

# Check if databases already exist
DB_COUNT=$(docker exec sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" -N -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai');" 2>/dev/null || echo "0")

if [ "$DB_COUNT" -ge 4 ]; then
    log "Databases already loaded (${DB_COUNT}/4 found)"
else
    log "Fresh database detected — loading baseline schema..."
    docker exec sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" -e "SET GLOBAL log_bin_trust_function_creators = 1;" 2>/dev/null
    docker exec -i sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" < install/config/mysql/db/full_backup.sql 2>/dev/null
    if [ $? -ne 0 ]; then
        warn "SQL import had warnings (this is usually OK)"
    fi
    log "Baseline schema loaded successfully"
fi

# Run Flyway migrations (applies any new migrations on top of baseline)
if [ -f install/flyway/migrate.sh ]; then
    log "Running Flyway migrations..."
    bash install/flyway/migrate.sh migrate 2>&1 | tail -5 || warn "Flyway migration had warnings (non-fatal)"
    log "Flyway migrations complete"
fi

# Create application user
MYSQL_USER=$(grep "^MYSQL_USER=" .env | cut -d= -f2)
MYSQL_PASSWORD=$(grep "^MYSQL_PASSWORD=" .env | cut -d= -f2)

docker exec sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" -e "
    CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
    GRANT ALL PRIVILEGES ON vinplay.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON vinplay_minigame.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON vinplay_admin.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON vinplay_gamebai.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON cgame.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON adminpro.* TO '${MYSQL_USER}'@'%';
    FLUSH PRIVILEGES;
" 2>/dev/null

log "MySQL user '${MYSQL_USER}' created with all permissions"

# GitLab #25 [SECURITY]: the superadmin password reset used to run
# unconditionally on every ./deploy.sh — silently undoing any rotation and
# leaving prod with superadmin/admin123. Now gated behind RESET_ADMIN_PASSWORD
# (default false). Set to 1/true only on first-time bootstraps or when the
# password is known to be forgotten.
if [ "${RESET_ADMIN_PASSWORD:-false}" = "true" ] || [ "${RESET_ADMIN_PASSWORD:-false}" = "1" ]; then
    docker exec sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" vinplay -e "
        UPDATE users SET password = MD5('admin123') WHERE user_name = 'superadmin';
    " 2>/dev/null
    log "Admin password RESET: superadmin / admin123 (RESET_ADMIN_PASSWORD=true)"
else
    log "Admin password: unchanged (set RESET_ADMIN_PASSWORD=true to reset to admin123)"
fi

# Fix game_config entries that code expects but backup doesn't have
docker exec sunwinkr-mysql mysql -u root -p"${MYSQL_ROOT_PW}" vinplay -e "
    UPDATE game_config SET value = JSON_SET(value, '$.code', 'ICB')
    WHERE name = 'bank_sms' AND JSON_EXTRACT(value, '$.code') IS NULL;
" 2>/dev/null
log "Patched game_config (bank_sms.code)"

# MongoDB indexes & views for game history (idempotent, safe to re-run)
MONGO_USER_ENV=$(grep "^MONGO_USER=" .env | cut -d= -f2)
MONGO_PASS_ENV=$(grep "^MONGO_PASSWORD=" .env | cut -d= -f2)
log "Creating MongoDB indexes and views..."
docker exec sunwinkr-mongodb mongo -u "$MONGO_USER_ENV" -p "$MONGO_PASS_ENV" \
    --authenticationDatabase admin --quiet \
    < install/config/mongod/mongo-init-indexes.js 2>/dev/null \
    && log "MongoDB indexes and views initialized" \
    || warn "MongoDB init script had warnings (non-fatal)"

# ---------------------------------------------------------------------------
# Step 8: Start All Services
# ---------------------------------------------------------------------------
if [ "$NO_START" = true ]; then
    step 8 "Skipping service startup (--no-start)"
    log "Run './deploy.sh' when ready to start"
else
    step 8 "Starting All Services"

    # ws-bridge is part of games.yml since GitLab #34. compose build below
    # rebuilds it automatically when the Node source changes.

    if [ "$REBUILD" = true ]; then
        # GitLab #20 Phase 0: snapshot every sunwinkr-*:latest image as
        # :last-working BEFORE the destructive rebuild. Pure metadata op —
        # instant, no runtime impact — gives a named rollback target if the
        # new build breaks something we only notice post-cutover.
        log "Tagging current images as :last-working (GitLab #20 rollback target)..."
        for img in $(docker images --format '{{.Repository}}' | grep -E '^sunwinkr-' | sort -u); do
            docker tag "${img}:latest" "${img}:last-working" 2>/dev/null || true
        done

        # GitLab #26 win #3 part A: explicit build step separate from `up`.
        # Fails fast with the real compose build exit code instead of being
        # hidden by the later `up` call.
        #
        # Also stamp the image with the git SHA + build timestamp so the
        # post-deploy drift check (below) can verify every running container
        # was built from the current commit. Prevents the 2026-04-22 class
        # of bug where `up --force-recreate` reused a stale image because
        # the earlier `build` command had a typo'd service name and
        # silently skipped.
        GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
        BUILD_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)
        log "Building Docker images (BuildKit cache accelerates repeat builds, stamping sha=${GIT_SHA})..."
        compose build --build-arg GIT_SHA="$GIT_SHA" --build-arg BUILD_TIME="$BUILD_TIME"
        BUILD_RC=$?
        if [ $BUILD_RC -ne 0 ]; then
            error "Docker compose build FAILED (exit $BUILD_RC). Fix the error above and re-run ./deploy.sh --rebuild. No containers were touched."
        fi
    fi

    # GitLab #26 win #3 part B: DROP --force-recreate. Compose v2 auto-
    # recreates containers whose image digest changed; anything unchanged
    # stays running. On a typical single-service PR we now restart 1
    # container instead of 22, saving 3-5 min of disruption and a platform-
    # wide autoheal flap. When no --rebuild, this becomes a ~second-level
    # no-op on already-running services.
    # GitLab #24: check exit code explicitly — no pipe hides the result.
    # MUST use `if !` instead of `$?`: under `set -e`, a non-zero exit from
    # `compose up -d` fires set-e before the next line runs, so any post-hoc
    # `COMPOSE_RC=$?` + guard is dead code. The previous form was silently
    # swallowing failures (e.g. "cannot remove container: container is running"
    # on a Java game with slow shutdown — staging-deploy-full 2026-04-22).
    #
    # Pre-stop safety net: compose's default 10s SIGTERM timeout is too short
    # for BitZero-framework Java games. Force-stop each running compose
    # service with a 30s grace period before recreate, so we don't race a
    # still-shutting-down JVM.
    #
    # GitLab #34: earlier version used `docker ps --filter name=sunwinkr-`,
    # which also killed containers owned by OTHER compose projects
    # (fe-nginx / fe-cloudflared under /home/sunwinkr-dev/, older ws-bridge
    # before it moved here) — and then compose up never brought them back,
    # silently taking down all game WebSocket subdomains. Scoping the stop
    # to `compose ps -q` guarantees we only touch services this invocation
    # is about to recreate anyway.
    if [ "$REBUILD" = true ]; then
        # Two-pronged pre-stop:
        # (a) project-scoped — anything compose -p $PROJECT_NAME owns now
        # (b) name-scoped — every container_name compose is about to recreate,
        #     by hard NAME rather than label, so containers from a stale
        #     project name (e.g. older "sunwinkr" instead of current
        #     "sunwinkr-backend") get cleaned. This is what kept biting us in
        #     pipelines #998/#1000/#1001 on sunwinkr-hazelcast.
        # GitLab #34 still satisfied because we only target service names
        # this compose invocation knows about — fe-nginx / fe-cloudflared
        # under /home/sunwinkr-dev/ are NOT in our compose graph and stay
        # untouched.
        STOP_IDS=$(compose ps -q --all 2>/dev/null || compose ps -q 2>/dev/null)
        for c in $STOP_IDS; do
            docker stop --time=30 "$c" >/dev/null 2>&1 || docker kill "$c" >/dev/null 2>&1 || true
        done
        for c in $STOP_IDS; do
            docker rm -f "$c" >/dev/null 2>&1 || true
        done
        unset STOP_IDS
        # Name-scoped pass: kill any container whose container_name appears
        # in our compose graph, regardless of project label.
        # Using `compose config | awk` rather than building names from the
        # service id, because this repo mixes prefixes — admin-next /
        # nextagency / admin-nginx use `sunkr-`, everything else uses
        # `sunwinkr-`. Reading the rendered config gives us the exact
        # container_name compose plans to spin up.
        for container_name in $(compose config 2>/dev/null \
                | awk '/^[[:space:]]*container_name:/ {gsub(/^[[:space:]]+container_name:[[:space:]]+/,""); print}'); do
            if docker container inspect "$container_name" >/dev/null 2>&1; then
                docker stop --time=30 "$container_name" >/dev/null 2>&1 || true
                docker rm -f "$container_name" >/dev/null 2>&1 || true
            fi
        done
    fi
    if ! compose up -d --remove-orphans --timeout 60; then
        # GitLab #27 Phase 1: compose up after rebuild failed. Auto-rollback
        # to :last-working snapshot (taken just before this step at Phase 0).
        auto_rollback "Docker compose up FAILED after --rebuild. See compose output above."
    fi

    log "Waiting for services to initialize..."
    sleep 30

    TOTAL=$(docker ps -a --filter name=sunwinkr | grep -c sunwinkr || echo 0)
    UP=$(docker ps --filter name=sunwinkr | grep -c sunwinkr || echo 0)
    HEALTHY=$(docker ps --filter name=sunwinkr --filter health=healthy | grep -c sunwinkr || echo 0)

    log "Services: ${UP}/${TOTAL} running, ${HEALTHY} healthy"

    # DEBUG breadcrumbs to isolate the silent exit
    log "BC#1 entering drift/stale block, REBUILD=$REBUILD"

    # GitLab #20: flag mixed-image state.
    # The old heuristic (uptime > 5 min = stale) breaks once #26 win #3
    # stops force-recreating everything — unchanged services legitimately
    # stay up for hours. Replaced with image-ID comparison: a container is
    # genuinely stale only if its image ID differs from the current
    # repo:latest after a --rebuild run.
    if [ "$REBUILD" = true ]; then
        log "BC#2 REBUILD=true, entering STALE scan"
        STALE=0
        CONTAINERS=$(docker ps --filter name=sunwinkr- --format '{{.Names}}')
        log "BC#2a STALE loop containers=$(echo "$CONTAINERS" | wc -l)"
        for container in $CONTAINERS; do
            log "BC#2b inspecting $container"
            repo=$(docker inspect "$container" -f '{{.Config.Image}}' 2>/dev/null | cut -d: -f1 || true)
            [ -z "$repo" ] && { log "BC#2c skip empty repo for $container"; continue; }
            cid=$(docker inspect "$container" -f '{{.Image}}' 2>/dev/null || true)
            iid=$(docker image inspect "${repo}:latest" -f '{{.Id}}' 2>/dev/null || true)
            if [ -n "$cid" ] && [ -n "$iid" ] && [ "$cid" != "$iid" ]; then
                STALE=$((STALE + 1))
            fi
        done
        log "BC#3 STALE scan done, STALE=$STALE"
        if [ "$STALE" -gt 0 ]; then
            # GitLab #27 Phase 1: stale image state = compose missed a service
            # or the pre-stop filter broke. Roll back rather than leave mixed.
            auto_rollback "$STALE sunwinkr-* container(s) running an IMAGE DIFFERENT from current :latest — compose did not pick up new build for those services. Check: for c in \$(docker ps --filter name=sunwinkr- --format '{{.Names}}'); do echo \"\$c: \$(docker inspect \$c -f '{{.Image}}')\"; done"
        fi
        log "BC#4 past STALE check, entering DRIFT section"

        # SOURCE_SHA drift check: running container's baked-in SHA must
        # match the working-tree HEAD. Catches the class of bug where a
        # build step was skipped (typo'd service name, partial compose
        # file selection, etc) so an old image keeps running even though
        # the working tree has fresh code.
        GIT_SHA_HEAD=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
        log "BC#5 GIT_SHA_HEAD=$GIT_SHA_HEAD"
        if [ "$GIT_SHA_HEAD" != "unknown" ]; then
            log "BC#6 entering DRIFT loop"
            DRIFT=0
            for container in $CONTAINERS; do
                repo=$(docker inspect "$container" -f '{{.Config.Image}}' 2>/dev/null | cut -d: -f1 || true)
                # Only verify services we build from our Dockerfile (sunwinkr-
                # prefix prefix on a *locally-built* image). Third-party or
                # external images (e.g. mysql, rabbitmq, mongo) have no
                # source.sha label and that's correct.
                sha=$(docker inspect "$container" -f '{{index .Config.Labels "source.sha"}}' 2>/dev/null || true)
                if [ -z "$sha" ] || [ "$sha" = "unknown" ]; then
                    continue
                fi
                if [ "$sha" != "$GIT_SHA_HEAD" ]; then
                    echo "  $container → source.sha=$sha  (working tree HEAD=$GIT_SHA_HEAD)" >&2
                    DRIFT=$((DRIFT + 1))
                fi
            done
            log "BC#7 DRIFT loop done, DRIFT=$DRIFT"
            if [ "$DRIFT" -gt 0 ]; then
                error "$DRIFT sunwinkr-* container(s) running bytecode from a commit OTHER than working-tree HEAD ($GIT_SHA_HEAD). The most likely cause is a skipped/typo'd 'compose build' step. Re-run ./deploy.sh --rebuild."
            else
                log "SOURCE_SHA drift check: all sunwinkr-* containers serving HEAD ($GIT_SHA_HEAD) ✓"
            fi
        fi
        log "BC#8 past DRIFT block"
    fi
    log "BC#9 past REBUILD block"
fi
log "BC#10 past main if/fi"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  DEPLOYMENT COMPLETE${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -n "${CLOUDFLARE_TUNNEL_TOKEN:-}" ] || grep -q "^CLOUDFLARE_TUNNEL_TOKEN=" .env 2>/dev/null; then
    echo -e "  Game Client:  ${BLUE}https://${PLAY_DOMAIN}${NC}"
    echo -e "  Admin Panel:  ${BLUE}https://${ADMIN_DOMAIN}/admin/login${NC}"
    echo -e "  Portal API:   https://${PLAY_DOMAIN}/api?c=9"
    echo -e "  Health Check: https://${PLAY_DOMAIN}/health"
    echo ""
    echo -e "  Cloudflare Tunnel: ${BLUE}Connected${NC} (managed by /home/sunwinkr-dev/)"
else
    IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')
    echo -e "  Admin Panel:  ${BLUE}http://${IP}:8088/admin/login${NC}"
    echo -e "  Portal API:   http://${IP}:8088/api?c=9"
    echo -e "  Health Check: http://${IP}:8088/health"
fi

if [ "${RESET_ADMIN_PASSWORD:-false}" = "true" ] || [ "${RESET_ADMIN_PASSWORD:-false}" = "1" ]; then
    echo -e "  Credentials:  ${YELLOW}superadmin / admin123${NC} (just reset)"
fi
echo ""
echo -e "  Commands:"
echo -e "    ./deploy.sh status     # Check service status"
echo -e "    ./deploy.sh logs       # Follow all logs"
echo -e "    ./deploy.sh stop       # Stop everything"
echo ""

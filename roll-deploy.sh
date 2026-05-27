#!/bin/bash
# =============================================================================
# SUNWINKR — ROLLING DEPLOY SCRIPT (zero-downtime service updates)
# =============================================================================
#
# What this does (vs the full deploy.sh):
#   - Pre-builds new image BEFORE touching the running container
#   - Tags the running image as :rollback for fast revert
#   - Force-recreates the service with up to STOP_GRACE seconds for in-flight
#     work to finish
#   - Waits for the new container to reach a service-specific READY signal
#     (Docker healthcheck for APIs, "BitZero Listening Sockets" for games,
#     port-listen check for ws-bridge) — NOT just docker's "Up" status
#   - Auto-rolls back to the previous image if the new one doesn't become
#     ready within READY_TIMEOUT
#
# Usage:
#   ./roll-deploy.sh <service> [<service2> ...]    # roll one or more services
#   ./roll-deploy.sh --list                        # list deployable services
#   ./roll-deploy.sh --check <service>             # only check current state
#   ./roll-deploy.sh --rollback <service>          # revert to :rollback tag
#   ./roll-deploy.sh --dry-run <service>           # show what would run
#
# Environment:
#   STOP_GRACE=30      Grace period (s) before SIGKILL on stop
#   READY_TIMEOUT=180  Max wait (s) for new container to become ready
#   SKIP_BUILD=0       Set to 1 to skip image build (use cached/prior image)
#
# Examples:
#   ./roll-deploy.sh backend-api
#   ./roll-deploy.sh backend-api portal-api vbee
#   STOP_GRACE=60 ./roll-deploy.sh game-minigame      # let TaiXiu round finish
#   SKIP_BUILD=1 ./roll-deploy.sh ws-bridge            # rollforward only
#   ./roll-deploy.sh --rollback game-minigame
#
# Pairs with the 3-node Hazelcast cluster (install/config/hazelcast/hazelcast.xml)
# — when a client disconnects from the rolled service mid-game, the user's
# `users` IMap entry survives in the surviving cluster members, so reconnect
# is instant from cache instead of MySQL.
# =============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREY='\033[0;90m'
NC='\033[0m'

log()  { printf "${GREY}[%s]${NC} %s\n" "$(date -u +%H:%M:%S)" "$*"; }
ok()   { printf "${GREEN}[%s] ✓${NC} %s\n" "$(date -u +%H:%M:%S)" "$*"; }
warn() { printf "${YELLOW}[%s] !${NC} %s\n" "$(date -u +%H:%M:%S)" "$*"; }
err()  { printf "${RED}[%s] ✗${NC} %s\n" "$(date -u +%H:%M:%S)" "$*" >&2; }
hdr()  { printf "\n${BLUE}═══ %s ═══${NC}\n" "$*"; }

STOP_GRACE="${STOP_GRACE:-30}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"
SKIP_BUILD="${SKIP_BUILD:-0}"

# All compose files we care about — same set the full deploy.sh uses
COMPOSE_FILES=(
    -f docker-compose.yml
    -f docker-compose.backend.yml
    -f docker-compose.database.yml
    -f docker-compose.web.yml
    -f docker-compose.games.yml
)

cd "$(dirname "$0")"

# ---------------------------------------------------------------------------
# Service registry — readiness signal for each known service
# ---------------------------------------------------------------------------
# READY_KIND:
#   healthy  — wait for docker healthcheck to report (healthy)
#   loglog   — wait for a regex match in container logs
#   listen   — wait for a TCP port to accept connections inside the container
#   compose  — just wait for docker compose's `up -d` to return
# ---------------------------------------------------------------------------
declare -A READY_KIND READY_ARG CONTAINER_NAME

# API services — Docker healthcheck is the source of truth
READY_KIND[backend-api]=healthy;          CONTAINER_NAME[backend-api]=sunwinkr-backend-api
READY_KIND[portal-api]=healthy;           CONTAINER_NAME[portal-api]=sunwinkr-portal-api
READY_KIND[vbee]=loglog;                  CONTAINER_NAME[vbee]=sunwinkr-vbee
READY_ARG[vbee]='Queue queue_log_money already exists|Consumer started'
READY_KIND[admin-next]=loglog;            CONTAINER_NAME[admin-next]=sunkr-admin-next
READY_ARG[admin-next]='middleware'
READY_KIND[admin-nextagency]=loglog;      CONTAINER_NAME[admin-nextagency]=sunkr-nextagency
READY_ARG[admin-nextagency]='middleware|OK'
READY_KIND[ws-bridge]=loglog;             CONTAINER_NAME[ws-bridge]=sunwinkr-ws-bridge
READY_ARG[ws-bridge]='Subdomain route|Listening|started'

# Game servers — every game has a Docker HEALTHCHECK (port 8888 /health),
# which is the cleanest cross-service signal. The previous "BitZeroServer
# Max" log marker was wrong for game-pokertour (tournament scheduler — no
# BitZero socket) and would false-fail rollbacks.
READY_KIND[game-minigame]=healthy;        CONTAINER_NAME[game-minigame]=sunwinkr-game-minigame
READY_KIND[game-baicao]=healthy;          CONTAINER_NAME[game-baicao]=sunwinkr-game-baicao
READY_KIND[game-poker]=healthy;           CONTAINER_NAME[game-poker]=sunwinkr-game-poker
READY_KIND[game-binh]=healthy;            CONTAINER_NAME[game-binh]=sunwinkr-game-binh
READY_KIND[game-xizach]=healthy;          CONTAINER_NAME[game-xizach]=sunwinkr-game-xizach
READY_KIND[game-tlmn]=healthy;            CONTAINER_NAME[game-tlmn]=sunwinkr-game-tlmn
READY_KIND[game-bacay]=healthy;           CONTAINER_NAME[game-bacay]=sunwinkr-game-bacay
READY_KIND[game-lieng]=healthy;           CONTAINER_NAME[game-lieng]=sunwinkr-game-lieng
READY_KIND[game-sam]=healthy;             CONTAINER_NAME[game-sam]=sunwinkr-game-sam
READY_KIND[game-coup]=healthy;            CONTAINER_NAME[game-coup]=sunwinkr-game-coup
READY_KIND[game-caro]=healthy;            CONTAINER_NAME[game-caro]=sunwinkr-game-caro
READY_KIND[game-cotuong]=healthy;         CONTAINER_NAME[game-cotuong]=sunwinkr-game-cotuong
READY_KIND[game-pokertour]=healthy;       CONTAINER_NAME[game-pokertour]=sunwinkr-game-pokertour
READY_KIND[game-slot]=healthy;            CONTAINER_NAME[game-slot]=sunwinkr-game-slot
READY_KIND[game-xocdia]=healthy;          CONTAINER_NAME[game-xocdia]=sunwinkr-game-xocdia
READY_KIND[game-xocdiatulinh]=healthy;    CONTAINER_NAME[game-xocdiatulinh]=sunwinkr-game-xocdiatulinh
READY_KIND[game-thirdparty]=healthy;      CONTAINER_NAME[game-thirdparty]=sunwinkr-game-thirdparty

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

list_services() {
    printf "Deployable services (group: readiness signal):\n\n"
    printf "  ${BLUE}APIs${NC} (docker healthcheck):\n"
    printf "    backend-api  portal-api  admin-next  admin-nextagency\n\n"
    printf "  ${BLUE}Brokers / WS${NC} (log marker):\n"
    printf "    vbee  ws-bridge\n\n"
    printf "  ${BLUE}Game servers${NC} (wait for 'BitZeroServer Max' marker):\n"
    printf "    game-minigame  game-baicao  game-poker  game-binh  game-xizach\n"
    printf "    game-tlmn  game-bacay  game-lieng  game-sam  game-coup\n"
    printf "    game-caro  game-cotuong  game-pokertour  game-slot\n"
    printf "    game-xocdia  game-xocdiatulinh  game-thirdparty\n\n"
}

is_known() {
    [[ -n "${READY_KIND[$1]:-}" ]]
}

current_image_id() {
    local cn="$1"
    docker inspect --format='{{.Image}}' "$cn" 2>/dev/null || echo ""
}

tag_rollback() {
    local cn="$1"
    local img
    img=$(current_image_id "$cn") || return 1
    if [[ -z "$img" ]]; then
        warn "no current image to tag for rollback (container $cn missing)"
        return 0
    fi
    docker tag "$img" "rollback/${cn}:previous" >/dev/null 2>&1 || true
    log "tagged ${cn}:previous → $img for rollback"
}

build_service() {
    local svc="$1"
    if [[ "$SKIP_BUILD" == "1" ]]; then
        log "SKIP_BUILD=1 — using current image for ${svc}"
        return 0
    fi
    log "building ${svc}…"
    local sha; sha=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    local ts; ts=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    if docker compose "${COMPOSE_FILES[@]}" build \
            --build-arg GIT_SHA="$sha" \
            --build-arg BUILD_TIME="$ts" \
            "$svc" 2>&1 | tail -5; then
        ok "built ${svc} (sha=$sha)"
    else
        err "build failed for ${svc}"
        return 1
    fi
}

wait_ready() {
    local svc="$1"
    local cn="${CONTAINER_NAME[$svc]}"
    local kind="${READY_KIND[$svc]}"
    local arg="${READY_ARG[$svc]:-}"
    local deadline=$(( $(date +%s) + READY_TIMEOUT ))

    log "waiting for ${svc} ready (kind=${kind}, timeout=${READY_TIMEOUT}s)…"

    while (( $(date +%s) < deadline )); do
        case "$kind" in
            healthy)
                if docker ps --filter "name=^${cn}$" --format '{{.Status}}' | grep -q "(healthy)"; then
                    ok "${svc} healthy"
                    return 0
                fi
                ;;
            loglog)
                if docker logs --since 5m "$cn" 2>&1 | grep -qE "$arg"; then
                    ok "${svc} matched ready signal /${arg}/"
                    return 0
                fi
                ;;
            listen)
                if docker exec "$cn" sh -c "exec 3<>/dev/tcp/127.0.0.1/${arg}" 2>/dev/null; then
                    ok "${svc} listening on :${arg}"
                    return 0
                fi
                ;;
            compose)
                ok "${svc} compose up returned"
                return 0
                ;;
        esac
        sleep 3
    done
    err "${svc} did not become ready within ${READY_TIMEOUT}s"
    return 1
}

roll_one() {
    local svc="$1"
    if ! is_known "$svc"; then
        err "unknown service: $svc (try --list)"
        return 1
    fi

    local cn="${CONTAINER_NAME[$svc]}"
    hdr "ROLL ${svc} → ${cn}"

    log "current container state:"
    docker ps --filter "name=^${cn}$" --format '  {{.Names}}  {{.Status}}  ({{.Image}})' || true

    # 1. Build new image (will be tagged sunwinkr-backend-${svc}:latest)
    build_service "$svc" || return 1

    # 2. Tag CURRENT running image as rollback target
    tag_rollback "$cn"

    # 3. Stop current with grace, then recreate from new image
    log "stopping current ${svc} (grace ${STOP_GRACE}s)…"
    docker compose "${COMPOSE_FILES[@]}" stop -t "$STOP_GRACE" "$svc" 2>&1 | tail -3 || true

    log "recreating ${svc} from new image…"
    if ! docker compose "${COMPOSE_FILES[@]}" up -d --no-deps --force-recreate "$svc" 2>&1 | tail -5; then
        err "compose up failed for ${svc} — attempting rollback"
        rollback_one "$svc" || true
        return 1
    fi

    # 4. Wait for new container to reach READY signal
    if ! wait_ready "$svc"; then
        warn "ready check failed for ${svc} — rolling back to previous image"
        rollback_one "$svc" || true
        return 1
    fi

    # 5. Final sanity: container is still running (not crash-looping)
    sleep 2
    local status
    status=$(docker ps --filter "name=^${cn}$" --format '{{.Status}}' || true)
    if [[ -z "$status" ]] || [[ "$status" == *"Restarting"* ]]; then
        err "${svc} unstable after deploy: status=${status:-MISSING}"
        rollback_one "$svc" || true
        return 1
    fi

    ok "${svc} deployed: $status"
}

rollback_one() {
    local svc="$1"
    if ! is_known "$svc"; then err "unknown service: $svc"; return 1; fi
    local cn="${CONTAINER_NAME[$svc]}"

    hdr "ROLLBACK ${svc}"
    if ! docker image inspect "rollback/${cn}:previous" >/dev/null 2>&1; then
        err "no rollback image found for ${cn}. Cannot revert."
        return 1
    fi

    log "stopping ${cn}…"
    docker stop -t "$STOP_GRACE" "$cn" 2>&1 || true

    # Find what tag the compose-managed container expects, then re-tag rollback to it
    local current_image
    current_image=$(docker inspect --format='{{.Config.Image}}' "$cn" 2>/dev/null || echo "")
    if [[ -n "$current_image" ]]; then
        log "re-tagging rollback/${cn}:previous → $current_image"
        docker tag "rollback/${cn}:previous" "$current_image"
    fi

    log "recreating ${svc} from rollback image…"
    docker compose "${COMPOSE_FILES[@]}" up -d --no-deps --force-recreate "$svc" 2>&1 | tail -5

    if wait_ready "$svc"; then
        ok "${svc} rolled back to previous image"
    else
        err "${svc} still not ready after rollback — manual intervention needed"
        return 1
    fi
}

check_one() {
    local svc="$1"
    if ! is_known "$svc"; then err "unknown service: $svc"; return 1; fi
    local cn="${CONTAINER_NAME[$svc]}"
    hdr "CHECK ${svc}"
    docker ps --filter "name=^${cn}$" --format '{{.Names}}\t{{.Status}}\t{{.Image}}' || true
    if docker image inspect "rollback/${cn}:previous" >/dev/null 2>&1; then
        local rb_id; rb_id=$(docker image inspect --format '{{.Id}}' "rollback/${cn}:previous" | head -c 19)
        log "rollback image available: ${rb_id}"
    else
        log "no rollback image (first deploy or rolled-back already)"
    fi
}

# ---------------------------------------------------------------------------
# Entry
# ---------------------------------------------------------------------------

if [[ $# -eq 0 ]]; then
    head -40 "$0" | grep "^#" | sed 's/^# \?//'
    echo
    list_services
    exit 0
fi

case "${1:-}" in
    --list|-l)        list_services; exit 0 ;;
    --check|-c)       shift; for s in "$@"; do check_one "$s"; done; exit 0 ;;
    --rollback|-r)    shift; for s in "$@"; do rollback_one "$s" || exit 1; done; exit 0 ;;
    --dry-run|-n)
        shift
        export SKIP_BUILD=1
        for s in "$@"; do
            if ! is_known "$s"; then err "unknown service: $s"; exit 1; fi
            echo "would: build $s, stop -t $STOP_GRACE $s, recreate, wait ready (kind=${READY_KIND[$s]} arg=${READY_ARG[$s]:-—})"
        done
        exit 0
        ;;
    --help|-h)        head -40 "$0" | grep "^#" | sed 's/^# \?//'; exit 0 ;;
    -*)               err "unknown flag: $1"; exit 1 ;;
esac

# Validate ALL requested services first — fail fast
for svc in "$@"; do
    if ! is_known "$svc"; then
        err "unknown service: $svc"
        echo "(run with --list to see deployable services)"
        exit 1
    fi
done

# Roll one at a time (intentional — keeps dependents stable)
overall_ok=1
for svc in "$@"; do
    if roll_one "$svc"; then
        :
    else
        overall_ok=0
        warn "deploy failed for ${svc}; subsequent services NOT attempted"
        break
    fi
done

if (( overall_ok == 1 )); then
    hdr "ALL DONE"
    ok "deployed: $*"
else
    hdr "FAILED"
    err "see logs above"
    exit 1
fi

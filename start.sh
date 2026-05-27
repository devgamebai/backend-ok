#!/bin/bash
# =============================================================================
# SUNWINKR CASINO PLATFORM - STARTUP SCRIPT
# =============================================================================
# Usage:
#   ./start.sh              # Start everything
#   ./start.sh database     # Start databases only
#   ./start.sh backend      # Start databases + backend APIs
#   ./start.sh games        # Start databases + backend + game servers
#   ./start.sh web          # Start databases + backend + web (nginx + PHP)
#   ./start.sh banca          # Start databases + fish game
#   ./start.sh offline-games  # Start databases + Spring offline-game APIs (lottery-spring-api + minigame-api)
#   ./start.sh lottery        # Start databases + lottery scraper
#   ./start.sh stop         # Stop everything
#   ./start.sh status       # Show status of all services
#   ./start.sh logs [svc]   # Follow logs (optionally for specific service)
# =============================================================================

set -e

COMPOSE_FILES="-f docker-compose.yml"
PROJECT_NAME="sunwinkr"

# SUN-1023: Pin compose project name for every sub-invocation. Prevents the
# orphan-container class of outage (2026-04-21): if a developer runs
# `docker compose up` directly from a renamed/cloned workdir without -p,
# compose defaults the project to the directory name and creates duplicate
# containers under the wrong label, blocking `./start.sh` recovery.
export COMPOSE_PROJECT_NAME="$PROJECT_NAME"

# Check .env file exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Copy .env.example to .env and fill in your passwords:"
    echo "  cp .env.example .env"
    exit 1
fi

# SUN-1023 preflight: detect containers that share our expected names but
# belong to a different compose project label. On hit, print remediation
# and abort — better to fail fast than compose up with half-orphaned state.
preflight_project_label() {
    # Skip for read-only subcommands; we only care when mutating state.
    case "${1:-}" in status|logs|"") ;; *)
        local expected_services=(
            sunwinkr-mysql sunwinkr-mongodb sunwinkr-redis sunwinkr-rabbitmq
            sunwinkr-hazelcast sunwinkr-backend-api sunwinkr-portal-api
            sunwinkr-vbee sunwinkr-vbee-2 sunwinkr-game-minigame
            sunwinkr-game-thirdparty sunwinkr-nginx
        )
        local bad=()
        for c in "${expected_services[@]}"; do
            local project
            project=$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$c" 2>/dev/null || true)
            if [ -n "$project" ] && [ "$project" != "$PROJECT_NAME" ]; then
                bad+=("$c (project=$project)")
            fi
        done
        if [ ${#bad[@]} -gt 0 ]; then
            echo "ERROR: Found containers matching our names but labeled with a different compose project:" >&2
            for b in "${bad[@]}"; do echo "  - $b" >&2; done
            echo "" >&2
            echo "This is the '2026-04-21 outage' class of failure — compose will refuse to" >&2
            echo "bring up our services because names collide. To fix, tear down the stale" >&2
            echo "project(s) first. Example:" >&2
            local stale_projects
            stale_projects=$(printf '%s\n' "${bad[@]}" | sed -n 's/.*project=\([^)]*\).*/\1/p' | sort -u)
            for p in $stale_projects; do
                echo "  docker compose -p $p down" >&2
            done
            echo "" >&2
            echo "Then re-run ./start.sh." >&2
            exit 2
        fi
    ;;
    esac
}
preflight_project_label "${1:-}"

# SUN-1033 / SUN-1027 preflight: validate file-target bind-mount sources
# actually exist as regular files. If a source path is missing when compose
# runs, Docker auto-creates an empty DIRECTORY at that path and the
# container fails with "not a directory" (2026-04-20: admin-nginx). List
# every file-target bind-mount here so the check lives in one place.
preflight_bind_mounts() {
    case "${1:-}" in status|logs|"") return ;; esac
    local missing=()
    local file_mounts=(
        # host_path:required_type  -- bind-mount sources that MUST be files
        "../sunkr-admin/nginx/default.conf:file"
        "./ws-bridge/bridge.js:file"
    )
    for spec in "${file_mounts[@]}"; do
        local path="${spec%:*}"
        local type="${spec##*:}"
        if [ "$type" = "file" ]; then
            if [ -d "$path" ]; then
                missing+=("$path (exists as DIRECTORY — Docker auto-created it from a missing source)")
            elif [ ! -f "$path" ]; then
                missing+=("$path (missing)")
            fi
        fi
    done
    if [ ${#missing[@]} -gt 0 ]; then
        echo "ERROR: bind-mount source validation failed:" >&2
        for m in "${missing[@]}"; do echo "  - $m" >&2; done
        echo "" >&2
        echo "Docker will auto-create missing source paths as empty DIRECTORIES," >&2
        echo "which fails file-target bind-mounts with 'not a directory'. Restore" >&2
        echo "the original file(s) and re-run ./start.sh. For sunkr-admin, clone" >&2
        echo "from git\@gitlab.com:aidenpearce001/sunkr-admin.git into" >&2
        echo "/root/sunwinkr/sunkr-admin." >&2
        exit 3
    fi
}
preflight_bind_mounts "${1:-}"

# Create networks if they don't exist
create_networks() {
    docker network create sunwinkr-frontend   2>/dev/null || true
    docker network create sunwinkr-backend --internal 2>/dev/null || true
    docker network create sunwinkr-games   --internal 2>/dev/null || true
    docker network create sunwinkr-database --internal 2>/dev/null || true
    docker network create sunwinkr-messaging --internal 2>/dev/null || true
}

# Create volumes if they don't exist
create_volumes() {
    docker volume create sunwinkr-mysql-data    2>/dev/null || true
    docker volume create sunwinkr-mongo-data    2>/dev/null || true
    docker volume create sunwinkr-redis-data    2>/dev/null || true
    docker volume create sunwinkr-rabbitmq-data 2>/dev/null || true
}

case "${1:-all}" in

    database|db)
        echo "Starting databases..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            up -d
        ;;

    backend|api)
        echo "Starting databases + backend APIs..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            up -d
        ;;

    games)
        echo "Starting databases + backend + game servers..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.games.yml \
            up -d
        ;;

    web)
        echo "Starting databases + backend + web..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.web.yml \
            up -d
        ;;

    banca|fish)
        echo "Starting databases + fish game..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.banca.yml \
            up -d
        ;;

    offline-games)
        echo "Starting databases + offline-game Spring APIs (lottery-spring-api + minigame-api)..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.offline-games.yml \
            up -d
        ;;

    all)
        echo "Starting ALL services..."
        create_networks && create_volumes
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.games.yml \
            -f docker-compose.banca.yml \
            -f docker-compose.web.yml \
            -f docker-compose.offline-games.yml \
            up -d
        ;;

    stop)
        echo "Stopping ALL services..."
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.games.yml \
            -f docker-compose.banca.yml \
            -f docker-compose.web.yml \
            -f docker-compose.offline-games.yml \
            down
        ;;

    status)
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.games.yml \
            -f docker-compose.banca.yml \
            -f docker-compose.web.yml \
            -f docker-compose.offline-games.yml \
            ps
        ;;

    logs)
        shift
        docker compose -p $PROJECT_NAME \
            -f docker-compose.yml \
            -f docker-compose.database.yml \
            -f docker-compose.backend.yml \
            -f docker-compose.games.yml \
            -f docker-compose.banca.yml \
            -f docker-compose.web.yml \
            -f docker-compose.offline-games.yml \
            logs -f $@
        ;;

    *)
        echo "Usage: $0 {database|backend|games|web|banca|offline-games|all|stop|status|logs}"
        exit 1
        ;;
esac

echo "Done."

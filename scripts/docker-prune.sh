#!/bin/bash
# =============================================================================
# Docker Prune — weekly reclaim of dangling images and build cache.
# =============================================================================
# Runs weekly via cron. Deletes:
#   - Untagged (dangling) images
#   - Stopped containers (safety: only containers that are NOT running)
#   - Unused networks
#   - Build cache entries (these accumulate fast from --no-cache rebuilds)
#
# NEVER prunes volumes — those hold MySQL / Mongo / RMQ data.
# NEVER prunes running-container images (docker refuses automatically).
#
# Usage:  docker-prune.sh              # real run
#         docker-prune.sh --dry-run    # summary only
# =============================================================================

set -u

DRY_RUN=false
[ "${1:-}" = "--dry-run" ] && DRY_RUN=true

ts() { date '+%Y-%m-%d %H:%M:%S'; }
log() { echo "[$(ts)] $*"; }

log "START  docker prune  dry_run=${DRY_RUN}"
log "BEFORE $(df -h / | awk 'NR==2 {print $3" used / "$4" avail ("$5" full)"}')"

# Show what will be freed
log "docker system df:"
docker system df 2>&1 | sed 's/^/    /'

if $DRY_RUN; then
    log "DRY RUN — skipping actual prune"
else
    # 1. Prune dangling images (non-running, no tag). -a = also tagged-but-unused.
    log "Running: docker image prune -a -f --filter 'until=168h'"
    docker image prune -a -f --filter 'until=168h' 2>&1 | sed 's/^/    /' || log "  image prune non-fatal error"

    # 2. Prune stopped containers (they are not restartable anyway).
    log "Running: docker container prune -f --filter 'until=168h'"
    docker container prune -f --filter 'until=168h' 2>&1 | sed 's/^/    /' || log "  container prune non-fatal error"

    # 3. Prune unused networks.
    log "Running: docker network prune -f"
    docker network prune -f 2>&1 | sed 's/^/    /' || log "  network prune non-fatal error"

    # 4. Prune build cache (biggest recoverer on this box).
    log "Running: docker builder prune -a -f --filter 'until=168h'"
    docker builder prune -a -f --filter 'until=168h' 2>&1 | sed 's/^/    /' || log "  builder prune non-fatal error"
fi

log "AFTER  $(df -h / | awk 'NR==2 {print $3" used / "$4" avail ("$5" full)"}')"
log "END"

#!/usr/bin/env bash
# =============================================================================
# SUN-1025 — Redis AOF recovery runbook
# =============================================================================
# Recovers sunwinkr-redis from a corrupted AOF incremental file (the failure
# mode from the 2026-04-21 outage: host reboot without graceful stop → AOF
# tail has partial write → Redis refuses to start → restart-loop).
#
# Actions (idempotent):
#   1. Stop the redis container (idempotent if already stopped).
#   2. Snapshot the current AOF dir to backups/redis-aof/<timestamp>/.
#   3. Run redis-check-aof --fix against the manifest to truncate the
#      corrupted tail.
#   4. Start the container back up.
#   5. Verify with PING (via the container's healthcheck command).
#
# Usage:
#   ./scripts/recover-redis-aof.sh             # uses defaults
#   DRY_RUN=1 ./scripts/recover-redis-aof.sh   # prints plan, doesn't act
#
# Safe to run when Redis is healthy: it will still snapshot + fix (no-op
# repair if clean), then start back up.
# =============================================================================

set -euo pipefail

CONTAINER="${CONTAINER:-sunwinkr-redis}"
VOLUME="${VOLUME:-sunwinkr-redis-data}"
IMAGE="${IMAGE:-redis:7-alpine}"
BACKUP_ROOT="${BACKUP_ROOT:-$(pwd)/backups/redis-aof}"
TS="$(date -u +%Y%m%dT%H%M%SZ)"
DRY_RUN="${DRY_RUN:-0}"

log() { printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
run() {
    if [ "$DRY_RUN" = "1" ]; then
        printf '  DRY-RUN: %s\n' "$*"
    else
        eval "$@"
    fi
}

log "SUN-1025 recovery starting (container=$CONTAINER volume=$VOLUME)"

# 1. Stop the container (idempotent).
if docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null | grep -q true; then
    log "Stopping $CONTAINER (grace 30s)…"
    run "docker stop -t 30 '$CONTAINER'"
else
    log "Container $CONTAINER not running — skipping stop."
fi

# 2. Snapshot current AOF dir.
SNAP_DIR="$BACKUP_ROOT/$TS"
log "Snapshotting AOF to $SNAP_DIR"
run "mkdir -p '$SNAP_DIR'"
# Use a throwaway container to tar the volume out — host doesn't touch /var/lib/docker.
run "docker run --rm -v '$VOLUME':/data -v '$SNAP_DIR':/out '$IMAGE' sh -c 'cd /data && tar -czf /out/redis-data.tgz appendonlydir dump.rdb 2>/dev/null || tar -czf /out/redis-data.tgz appendonlydir 2>/dev/null || true'"
if [ "$DRY_RUN" != "1" ] && [ -s "$SNAP_DIR/redis-data.tgz" ]; then
    log "Snapshot size: $(du -h "$SNAP_DIR/redis-data.tgz" | awk '{print $1}')"
fi

# 3. Run redis-check-aof --fix.
log "Running redis-check-aof --fix on appendonlydir manifest…"
run "docker run --rm -i -v '$VOLUME':/data '$IMAGE' sh -c 'ls /data/appendonlydir/appendonly.aof.manifest >/dev/null && echo y | redis-check-aof --fix /data/appendonlydir/appendonly.aof.manifest'"

# 4. Start back up.
log "Starting $CONTAINER…"
run "docker start '$CONTAINER'"

# 5. Verify. Give Redis up to 60s to load AOF + start accepting connections.
if [ "$DRY_RUN" != "1" ]; then
    log "Verifying (up to 60s)…"
    ok=0
    for _ in $(seq 1 30); do
        if docker exec "$CONTAINER" sh -c 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli ping' 2>/dev/null | grep -q PONG; then
            ok=1
            break
        fi
        sleep 2
    done
    if [ $ok -eq 1 ]; then
        log "OK — Redis is accepting connections."
        docker logs --tail 20 "$CONTAINER" | grep -E 'Ready to accept|DB loaded|AOF|incr' || true
    else
        log "FAIL — Redis did not come back within 60s. Last logs:"
        docker logs --tail 40 "$CONTAINER" >&2
        exit 1
    fi
fi

log "Done. Snapshot kept at $SNAP_DIR"

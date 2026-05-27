#!/usr/bin/env bash
# Backfill vinplay.gsc_bets from Mongo log_gsc_bets.
#
# Cross-store traceability follow-up #5. One-shot tool. The 5p2-async
# dual-write handles new rows once GSC_BETS_MYSQL_DUAL_WRITE=true; this
# script catches up the historical rows that landed in Mongo before the
# flag flipped.
#
# Idempotent: the DAO uses INSERT IGNORE on uk_wager_code, so a re-run
# is harmless. Catch-up runs after the dual-write has been on for some
# time are also safe — overlapping rows just no-op.
#
# Run order:
#   1. install/config/mysql/migrations/2026_05_02_gsc_bets_table.sql
#   2. Set GSC_BETS_MYSQL_DUAL_WRITE=true on the api/vbee container.
#   3. Run this script (smoke first: --limit 10).
#   4. Verify counts: SELECT COUNT(*) FROM vinplay.gsc_bets;
#
# Usage:
#   bin/backfill_gsc_bets_to_mysql.sh [--limit N] [--dry-run]
#
# Defaults: full backfill (no limit), live INSERTs.
#
# Exit codes:
#   0  — completed (check stdout for inserted/skipped/errors counts)
#   1  — pre-flight failed (container not running, jar missing, etc.)
#   2  — Java tool returned non-zero

set -euo pipefail

CONTAINER="${GSC_BACKFILL_CONTAINER:-sunwinkr-backend-api}"
MAIN_CLASS="com.vinplay.dal.tools.GscBetsBackfill"

# Pre-flight: container must be running.
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: container '${CONTAINER}' is not running" >&2
    echo "       (override with GSC_BACKFILL_CONTAINER=<name> if needed)" >&2
    exit 1
fi

# The api container already has the runtime classpath assembled at /app
# (see Dockerfile / entrypoint.sh). We re-use it: java -cp '/app/*' ...
# The CONFIG dir resolves db_pool.properties via VBeePath.
# Fall back to /app if /app/lib doesn't exist.
CP_HINT="$(docker exec "${CONTAINER}" sh -c 'ls /app/lib/*.jar 2>/dev/null | head -1' || true)"
if [[ -n "${CP_HINT}" ]]; then
    CLASSPATH='/app/lib/*:/app/config:/app'
else
    CLASSPATH='/app/*:/app/config:/app'
fi

echo "[backfill_gsc_bets] container=${CONTAINER} cp=${CLASSPATH} args=$*"
docker exec -e MYSQL_PASSWORD="${MYSQL_PASSWORD:-}" \
    "${CONTAINER}" \
    java -cp "${CLASSPATH}" "${MAIN_CLASS}" "$@" \
    || { echo "ERROR: backfill tool exited non-zero" >&2; exit 2; }

echo "[backfill_gsc_bets] DONE"

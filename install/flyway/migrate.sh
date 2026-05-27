#!/bin/bash
# =============================================================================
# Flyway Migration Runner
# =============================================================================
# Usage:
#   ./install/flyway/migrate.sh              # Run pending migrations
#   ./install/flyway/migrate.sh info         # Show migration status
#   ./install/flyway/migrate.sh repair       # Fix failed migrations
#   ./install/flyway/migrate.sh baseline     # Mark current DB as V1 (existing deployments)
#   ./install/flyway/migrate.sh validate     # Validate applied migrations
#   ./install/flyway/migrate.sh create NAME  # Create new migration file
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | grep -v '^$' | xargs)
fi

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_ROOT_PW="${MYSQL_ROOT_PASSWORD:-}"
FLYWAY_IMAGE="flyway/flyway:10.15.2"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[FLYWAY]${NC} $1"; }
warn() { echo -e "${YELLOW}[FLYWAY]${NC} $1"; }
error() { echo -e "${RED}[FLYWAY]${NC} $1"; exit 1; }

# ── Create new migration ──────────────────────────────────────────
if [ "${1:-}" = "create" ]; then
    NAME="${2:-unnamed}"
    # Find next version number
    LAST_V=$(ls "$SCRIPT_DIR/sql/" 2>/dev/null | grep -oP '^V\K\d+' | sort -n | tail -1)
    NEXT_V=$(( ${LAST_V:-1} + 1 ))
    FILENAME="V${NEXT_V}__${NAME}.sql"
    cat > "$SCRIPT_DIR/sql/$FILENAME" << 'SQLEOF'
-- Migration: ${NAME}
-- Created: $(date +%Y-%m-%d)
-- Description: TODO

-- Add your SQL here
-- Example:
-- ALTER TABLE users ADD COLUMN new_field VARCHAR(100) DEFAULT NULL;
-- CREATE TABLE IF NOT EXISTS new_table (...);
SQLEOF
    # Fix template variables
    sed -i "s/\${NAME}/$NAME/g; s/\$(date +%Y-%m-%d)/$(date +%Y-%m-%d)/g" "$SCRIPT_DIR/sql/$FILENAME"
    log "Created: sql/$FILENAME"
    exit 0
fi

# ── Run Flyway via Docker ─────────────────────────────────────────
COMMAND="${1:-migrate}"

log "Running: flyway $COMMAND"

docker run --rm \
    --network sunkr-network \
    -v "$SCRIPT_DIR/sql:/flyway/sql:ro" \
    -v "$SCRIPT_DIR/conf:/flyway/conf:ro" \
    -e FLYWAY_URL="jdbc:mariadb://${MYSQL_HOST}:3306/?useSSL=false&allowPublicKeyRetrieval=true" \
    -e FLYWAY_USER="root" \
    -e FLYWAY_PASSWORD="${MYSQL_ROOT_PW}" \
    "$FLYWAY_IMAGE" \
    -locations="filesystem:/flyway/sql" \
    -schemas="vinplay,vinplay_minigame,vinplay_admin,vinplay_gamebai" \
    -defaultSchema="vinplay" \
    -table="schema_migrations" \
    -baselineOnMigrate=true \
    -baselineVersion=2 \
    -outOfOrder=true \
    -placeholderReplacement=false \
    -connectRetries=10 \
     -X \
    "$COMMAND"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    log "flyway $COMMAND completed successfully"
else
    error "flyway $COMMAND failed (exit code: $EXIT_CODE)"
fi

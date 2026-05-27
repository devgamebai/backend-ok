#!/bin/bash
# =============================================================================
# Compose ↔ in-container cache sanity check.
#
# Catches the 2026-04-28 mongo OOM regression class:
#   docker-compose.database.yml had mongodb mem_limit: 2G
#   install/config/mongod/mongod.conf had wiredTiger.cacheSizeGB: 4
#   → mongo OOM-killed 188 times; vbee MongoSocketException storms; data loss
#
# Rule: for every service that has both an internal cache config AND a Docker
# memory limit, the limit must be at least 1.5× the cache target. The 0.5×
# headroom covers connections, indexes, network buffers, and growth.
#
# Currently checks:
#   mongo : wiredTiger.cacheSizeGB  vs  mongodb.deploy.resources.limits.memory
#
# Extensible: add more (service, parser) tuples in CHECKS below as more
# services gain explicit cache configs (mysql innodb_buffer_pool_size,
# redis maxmemory, hazelcast cluster.size, ...).
#
# Exit codes:
#   0 = all checks passed
#   1 = at least one violation (build / CI should fail)
#   2 = parse error (config file malformed or service missing)
#
# Designed for CI:
#   ./backend-master/scripts/check-compose-resource-sanity.sh
#
# Designed for Dockerfile build stage:
#   add "RUN ./backend-master/scripts/check-compose-resource-sanity.sh" early
# =============================================================================

set -euo pipefail

# Resolve repo root (script in backend-master/scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors (TTY only)
if [ -t 1 ]; then
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; CYAN=''; NC=''
fi

log()  { echo -e "${CYAN}[resource-lint]${NC} $1"; }
ok()   { echo -e "${GREEN}[resource-lint] ✅${NC} $1"; }
fail() { echo -e "${RED}[resource-lint] ❌${NC} $1" >&2; }
warn() { echo -e "${YELLOW}[resource-lint] ⚠${NC}  $1"; }

VIOLATIONS=0

# Parse "6G", "512M", "2G" → number of GB (float)
parse_mem_to_gb() {
    local s="$1"
    [[ -z "$s" || "$s" == "null" ]] && { echo "MISSING"; return; }
    local val=$(echo "$s" | grep -oE "^[0-9]+(\.[0-9]+)?")
    local unit=$(echo "$s" | grep -oE "[KMGTkmgt]" | head -1 | tr 'a-z' 'A-Z')
    case "$unit" in
        G|"") echo "$val" ;;
        M) echo "$val" | awk '{printf "%.4f", $1/1024}' ;;
        K) echo "$val" | awk '{printf "%.4f", $1/1024/1024}' ;;
        T) echo "$val" | awk '{printf "%.0f", $1*1024}' ;;
        *) echo "BAD_UNIT" ;;
    esac
}

# ─────────────────────────────────────────────────────────────────────
# Check 1: mongodb wiredTiger.cacheSizeGB vs container memory
# ─────────────────────────────────────────────────────────────────────
check_mongodb() {
    log "Checking mongodb cache vs container memory…"
    local conf="$REPO_ROOT/install/config/mongod/mongod.conf"
    local compose="$REPO_ROOT/docker-compose.database.yml"

    [ -f "$conf" ] || { fail "missing $conf"; return 2; }
    [ -f "$compose" ] || { fail "missing $compose"; return 2; }

    # Extract cacheSizeGB (line: "cacheSizeGB: 2", possibly indented)
    local cache_gb=$(grep -oE "cacheSizeGB:[[:space:]]*[0-9]+(\.[0-9]+)?" "$conf" \
                     | tail -1 \
                     | grep -oE "[0-9]+(\.[0-9]+)?" \
                     | tail -1)
    if [ -z "$cache_gb" ]; then
        warn "mongodb cacheSizeGB not found in $conf — skipping"
        return 0
    fi

    # Extract mongodb mem_limit. Format inside compose:
    #   mongodb:
    #     ...
    #     deploy:
    #       resources:
    #         limits:
    #           memory: 6G
    # Use awk to scope to the mongodb stanza.
    local mem_str=$(awk '
        /^  mongodb:/ { in_svc=1; next }
        in_svc && /^  [a-z]/ && !/^    / { in_svc=0 }
        in_svc && /^[[:space:]]+memory:/ { print $2; exit }
    ' "$compose")

    if [ -z "$mem_str" ]; then
        fail "mongodb has no memory limit set in $compose — required (host has finite RAM)"
        return 1
    fi

    local mem_gb=$(parse_mem_to_gb "$mem_str")
    [ "$mem_gb" = "MISSING" ] || [ "$mem_gb" = "BAD_UNIT" ] && {
        fail "mongodb memory '$mem_str' could not be parsed"
        return 1
    }

    # Required: mem_limit ≥ 1.5 × cacheSizeGB
    local min_gb=$(awk -v c="$cache_gb" 'BEGIN{ printf "%.2f", c * 1.5 }')
    local headroom=$(awk -v m="$mem_gb" -v c="$cache_gb" 'BEGIN{ printf "%.2f", m - c }')

    if awk -v m="$mem_gb" -v r="$min_gb" 'BEGIN{ exit !(m + 0.001 < r) }'; then
        fail "mongodb mem_limit=${mem_gb}G < required ${min_gb}G (1.5× cacheSizeGB=${cache_gb}G)"
        fail "  → headroom=${headroom}G is too tight; container will OOM under load"
        fail "  → fix: bump 'memory' in $compose (mongodb:deploy:resources:limits) OR lower cacheSizeGB in $conf"
        return 1
    fi

    ok "mongodb: mem_limit=${mem_gb}G  cacheSizeGB=${cache_gb}G  headroom=${headroom}G"
    return 0
}

# ─────────────────────────────────────────────────────────────────────
# Check 2 (informational): MySQL innodb_buffer_pool_size if set
# ─────────────────────────────────────────────────────────────────────
check_mysql() {
    log "Checking MySQL innodb_buffer_pool_size (informational)…"
    local cnf="$REPO_ROOT/install/config/mysql/conf.d/my.cnf"
    local compose="$REPO_ROOT/docker-compose.database.yml"
    [ -f "$cnf" ] || { warn "no MySQL cnf at $cnf — skip"; return 0; }

    # Look for innodb_buffer_pool_size
    local pool_str=$(grep -oE "^[[:space:]]*innodb_buffer_pool_size[[:space:]]*=[[:space:]]*[0-9]+[KMGT]?" "$cnf" 2>/dev/null \
                     | head -1 | grep -oE "[0-9]+[KMGT]?" | head -1)
    if [ -z "$pool_str" ]; then
        ok "mysql innodb_buffer_pool_size not explicitly set (uses 128M default)"
        return 0
    fi

    local pool_gb=$(parse_mem_to_gb "$pool_str")
    local mem_str=$(awk '
        /^  mysql:/ { in_svc=1; next }
        in_svc && /^  [a-z]/ && !/^    / { in_svc=0 }
        in_svc && /^[[:space:]]+memory:/ { print $2; exit }
    ' "$compose")
    local mem_gb=$(parse_mem_to_gb "$mem_str")

    local min_gb=$(awk -v c="$pool_gb" 'BEGIN{ printf "%.2f", c * 1.5 }')
    if awk -v m="$mem_gb" -v r="$min_gb" 'BEGIN{ exit !(m + 0.001 < r) }'; then
        fail "mysql mem_limit=${mem_gb}G < required ${min_gb}G (1.5× innodb_buffer_pool_size=${pool_gb}G)"
        return 1
    fi
    ok "mysql: mem_limit=${mem_gb}G  innodb_buffer_pool_size=${pool_gb}G"
    return 0
}

# ─────────────────────────────────────────────────────────────────────
# Run all checks
# ─────────────────────────────────────────────────────────────────────
log "Repo root: $REPO_ROOT"

check_mongodb || VIOLATIONS=$((VIOLATIONS + 1))
check_mysql   || VIOLATIONS=$((VIOLATIONS + 1))

echo ""
if [ "$VIOLATIONS" -eq 0 ]; then
    ok "All resource sanity checks passed"
    exit 0
else
    fail "$VIOLATIONS violation(s) — fix before merge"
    exit 1
fi

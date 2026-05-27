#!/usr/bin/env bash
# =============================================================================
# DB REDESIGN INVENTORY RUNNER — Phase 0 discovery, read-only
# =============================================================================
# Runs both diagnostic scripts against sunwinkr-mysql and drops TSV files into
# /tmp/db_inventory_<ts>/. Splits each result set on its "==Rn==..." sentinel
# row so a reviewer can open a single file per section in a spreadsheet.
#
# Usage:
#   ./install/config/mysql/diagnostics/run_inventory.sh
#   ./install/config/mysql/diagnostics/run_inventory.sh --skip-orphans  # schema only (fast)
#   MYSQL_CONTAINER=sunwinkr-mysql-replica ./run_inventory.sh
#
# Exits non-zero if any mysql invocation fails. Does NOT write to DB.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-sunwinkr-mysql}"
# Capture caller-supplied override BEFORE sourcing .env (which sets a non-root
# MYSQL_USER=sunwinkr_user that cannot read information_schema fully).
DIAG_USER_OVERRIDE="${DIAG_MYSQL_USER:-}"

# Pull password from .env if not already in env.
if [[ -z "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  if [[ -f "$REPO_ROOT/.env" ]]; then
    # shellcheck disable=SC1091
    set -a; . "$REPO_ROOT/.env"; set +a
  fi
fi

# Prefer explicit override, else hard-default to root — information_schema
# visibility on user-schema tables needs root grants.
MYSQL_USER="${DIAG_USER_OVERRIDE:-root}"

if [[ -z "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  echo "ERROR: MYSQL_ROOT_PASSWORD not set. Set env var or run from repo with .env present." >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  echo "ERROR: container '$MYSQL_CONTAINER' is not running. Set MYSQL_CONTAINER env or start the DB." >&2
  exit 1
fi

TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="/tmp/db_inventory_${TS}"
mkdir -p "$OUT_DIR"
echo "Inventory output dir: $OUT_DIR"

run_sql () {
  local label="$1" sql_file="$2" raw_out="$3"
  echo "[$(date +%H:%M:%S)] Running $label ..."
  if ! docker exec -i "$MYSQL_CONTAINER" \
         mysql --batch --raw \
               -u"$MYSQL_USER" -p"$MYSQL_ROOT_PASSWORD" \
               < "$sql_file" > "$raw_out" 2>"$raw_out.stderr"; then
    echo "ERROR: $label failed. See $raw_out.stderr" >&2
    cat "$raw_out.stderr" >&2
    exit 1
  fi
  # Silence benign "Using a password on the command line" warning.
  grep -v '^mysql: \[Warning\] Using a password' "$raw_out.stderr" >&2 || true
  rm -f "$raw_out.stderr"
}

# Phase 1: schema inventory (fast, ~5s).
run_sql "db_inventory" \
        "$SCRIPT_DIR/db_inventory.sql" \
        "$OUT_DIR/raw_inventory.tsv"

# Phase 2: orphan scan (slow, can skip).
if [[ "${1:-}" != "--skip-orphans" ]]; then
  run_sql "db_orphan_scan" \
          "$SCRIPT_DIR/db_orphan_scan.sql" \
          "$OUT_DIR/raw_orphans.tsv"
else
  echo "--skip-orphans: not running orphan scan."
fi

# Split each raw output on sentinel rows "==Rn==<name>==" into per-section TSVs.
split_sections () {
  local raw="$1"
  [[ -f "$raw" ]] || return 0
  awk -v outdir="$OUT_DIR" '
    /^==R[0-9]+==.*==$/ {
      # Close prior section before starting the next.
      if (current != "") close(current)
      name = $0
      gsub(/^==R[0-9]+==|==$/, "", name)
      section_num++
      current = outdir "/R" section_num "_" name ".tsv"
      # Skip the sentinel row itself + skip the next line (header dup from mysql --batch).
      skip_next = 1
      next
    }
    {
      if (current == "") next
      if (skip_next) { skip_next = 0; next }   # mysql echoes "section" header
      print > current
    }
  ' "$raw"
}

split_sections "$OUT_DIR/raw_inventory.tsv"
split_sections "$OUT_DIR/raw_orphans.tsv"

echo
echo "=== Per-section files ==="
ls -la "$OUT_DIR"/R*.tsv 2>/dev/null || echo "(no split files — check raw_*.tsv)"

echo
echo "=== Quick stats ==="
for f in "$OUT_DIR"/R*.tsv; do
  [[ -f "$f" ]] || continue
  lines=$(($(wc -l < "$f") - 1))   # minus header
  [[ $lines -lt 0 ]] && lines=0
  printf '%-45s %6d rows\n' "$(basename "$f")" "$lines"
done

echo
echo "Open in spreadsheet:  libreoffice $OUT_DIR/R*.tsv"
echo "Or inspect raw:       less $OUT_DIR/raw_inventory.tsv"

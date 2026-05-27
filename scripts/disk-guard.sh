#!/bin/bash
# =============================================================================
# Disk Guard — hourly safety net against runaway application logs.
# =============================================================================
# Runs every hour via cron. Two responsibilities:
#   1. Cap every active .log file (main.log / debug.log) at MAX_LOG_MB.
#      If a file exceeds the cap, it is tail-truncated to LAST_KEEP_MB of its
#      tail (so recent context is preserved for debugging). Truncation uses
#      an in-place rewrite so the JVM's open file handle keeps working.
#   2. Delete rotated day-stamped log files older than RETENTION_DAYS.
#      Logback already respects maxHistory, but this is belt-and-suspenders
#      for services that crash mid-rotation or have mis-tuned configs.
#
# Money / audit CSVs are NEVER touched. They are financial audit trails.
#
# Usage:  disk-guard.sh               # normal hourly run
#         disk-guard.sh --dry-run     # show what would happen, do nothing
# =============================================================================

set -u

LOG_DIR="/root/sunwinkr/sunwinkr/logs"
MAX_LOG_MB=200          # per-file hard cap
LAST_KEEP_MB=50         # how much tail to keep when truncating
RETENTION_DAYS=3        # delete rotated *.YYYY-MM-DD.log older than this

DRY_RUN=false
[ "${1:-}" = "--dry-run" ] && DRY_RUN=true

ts() { date '+%Y-%m-%d %H:%M:%S'; }

log() { echo "[$(ts)] $*"; }

# ---------- 1. Truncate oversized active .log files ----------
TRUNCATED=0
TRUNCATED_BYTES=0
while IFS= read -r f; do
    [ -z "$f" ] && continue
    size=$(stat -c '%s' "$f" 2>/dev/null || echo 0)
    size_mb=$((size / 1024 / 1024))
    log "OVERSIZE  ${size_mb}MB  $f"
    TRUNCATED=$((TRUNCATED + 1))
    TRUNCATED_BYTES=$((TRUNCATED_BYTES + size))
    if $DRY_RUN; then
        continue
    fi
    # In-place tail truncation: keep only the last LAST_KEEP_MB bytes.
    # Using `dd` + tmpfile + mv keeps the target inode STABLE so any JVM
    # holding it with an open FD continues writing at the tail (not losing data).
    tmpfile="${f}.guard.$$"
    if tail -c ${LAST_KEEP_MB}M "$f" > "$tmpfile" 2>/dev/null; then
        # Preserve inode: overwrite in place, then truncate to new length.
        dd if="$tmpfile" of="$f" bs=1M conv=notrunc 2>/dev/null
        new_size=$(stat -c '%s' "$tmpfile")
        truncate -s "$new_size" "$f"
        rm -f "$tmpfile"
    else
        rm -f "$tmpfile"
        log "  tail failed, falling back to full truncate on $f"
        : > "$f"
    fi
done < <(find "$LOG_DIR" -type f -name '*.log' ! -name '*.20*.log' -size +${MAX_LOG_MB}M 2>/dev/null)

# ---------- 2. Delete rotated day-stamped files past retention ----------
DELETED=$(find "$LOG_DIR" -type f \( -name '*.20*.log' -o -name '*.20*.log.gz' \) -mtime +${RETENTION_DAYS} 2>/dev/null | wc -l)
if [ "$DELETED" -gt 0 ]; then
    log "DELETE ${DELETED} rotated files older than ${RETENTION_DAYS} days"
    if ! $DRY_RUN; then
        find "$LOG_DIR" -type f \( -name '*.20*.log' -o -name '*.20*.log.gz' \) -mtime +${RETENTION_DAYS} -delete 2>/dev/null
    fi
fi

# ---------- 3. Report ----------
if [ "$TRUNCATED" -gt 0 ] || [ "$DELETED" -gt 0 ]; then
    freed_mb=$((TRUNCATED_BYTES / 1024 / 1024))
    log "SUMMARY  truncated=${TRUNCATED} files (~${freed_mb}MB), deleted=${DELETED} rotated files  dry_run=${DRY_RUN}"
else
    log "SUMMARY  nothing to do"
fi

# ---------- 4. Report disk headroom at the end ----------
df -h / | awk 'NR==2 {printf "[%s] DISK  size=%s used=%s avail=%s pct=%s\n", strftime("%Y-%m-%d %H:%M:%S"), $2, $3, $4, $5}'

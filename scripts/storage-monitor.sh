#!/bin/bash
# =============================================================================
# Storage Monitor — 24h disk-growth acceptance script for SUN-796.
# =============================================================================
# Appends one CSV line per invocation to logs/storage-monitor-YYYYMMDD.csv.
# Designed to be run every 15 minutes via cron. Zero-cost sampling — all the
# numbers are cheap metadata reads (df, du -s, docker exec one-shots).
#
# Crontab:
#   */15 * * * * /root/sunwinkr/sunwinkr/scripts/storage-monitor.sh
#
# After 24 hours (96 samples), run:
#   /root/sunwinkr/sunwinkr/scripts/storage-monitor.sh --report
# to print a pass/fail summary against the SUN-796 acceptance criteria.
#
# Output CSV columns:
#   ts, disk_used_mb, disk_free_mb, disk_pct,
#   mysql_vol_mb, mongo_vol_mb, rmq_vol_mb,
#   logs_dir_mb, binh_main_log_mb, binh_debug_log_mb,
#   mongo_slot_docs_total, rmq_live_queue_max, rmq_live_queue_max_name,
#   binlog_files, npe_per_min
# =============================================================================

set -u

REPO_ROOT="/root/sunwinkr/sunwinkr"
CSV_DIR="$REPO_ROOT/logs"
CSV_FILE="$CSV_DIR/storage-monitor-$(date +%Y%m%d).csv"
MYSQL_ROOT_PW=$(grep '^MYSQL_ROOT_PASSWORD=' "$REPO_ROOT/.env" | cut -d= -f2-)
MONGO_USER=$(grep '^MONGO_USER=' "$REPO_ROOT/.env" | cut -d= -f2-)
MONGO_PASS=$(grep '^MONGO_PASSWORD=' "$REPO_ROOT/.env" | cut -d= -f2-)

mkdir -p "$CSV_DIR"

# Write header if the day's file doesn't exist yet
if [ ! -s "$CSV_FILE" ]; then
    echo "ts,disk_used_mb,disk_free_mb,disk_pct,mysql_vol_mb,mongo_vol_mb,rmq_vol_mb,logs_dir_mb,binh_main_log_mb,binh_debug_log_mb,mongo_slot_docs_total,rmq_live_queue_max,rmq_live_queue_max_name,binlog_files,npe_per_min" > "$CSV_FILE"
fi

# ---------- --report mode ----------
if [ "${1:-}" = "--report" ]; then
    if [ ! -s "$CSV_FILE" ]; then
        echo "No samples yet in $CSV_FILE"
        exit 1
    fi
    samples=$(tail -n +2 "$CSV_FILE" | wc -l)
    echo "=== Storage monitor report — $CSV_FILE ==="
    echo "samples: $samples"

    first_used=$(awk -F, 'NR==2 {print $2; exit}' "$CSV_FILE")
    last_used=$(awk -F, 'END {print $2}' "$CSV_FILE")
    delta_mb=$(( ${last_used:-0} - ${first_used:-0} ))
    echo "disk_used start=${first_used} MB end=${last_used} MB  delta=${delta_mb} MB"

    max_binh_main=$(awk -F, 'NR>1 && $9+0 > max {max=$9+0} END{print max+0}' "$CSV_FILE")
    max_binh_debug=$(awk -F, 'NR>1 && $10+0 > max {max=$10+0} END{print max+0}' "$CSV_FILE")
    echo "binh logs max: main=${max_binh_main} MB  debug=${max_binh_debug} MB"

    max_live_q=$(awk -F, 'NR>1 && $12+0 > max {max=$12+0; n=$13} END{print max "\t" n}' "$CSV_FILE")
    echo "max live RMQ queue depth: $max_live_q"

    max_npe_rate=$(awk -F, 'NR>1 && $15+0 > max {max=$15+0} END{print max+0}' "$CSV_FILE")
    echo "max NPE rate: ${max_npe_rate} lines/min"

    echo
    echo "=== SUN-796 Acceptance Criteria ==="
    pass=0; fail=0
    [ "$delta_mb" -lt 500 ] && { echo "[PASS] disk delta < 500 MB ($delta_mb MB)"; pass=$((pass+1)); } || { echo "[FAIL] disk delta >= 500 MB ($delta_mb MB)"; fail=$((fail+1)); }
    [ "${max_binh_main:-0}" -lt 200 ] && [ "${max_binh_debug:-0}" -lt 200 ] && { echo "[PASS] no log file >200 MB"; pass=$((pass+1)); } || { echo "[FAIL] a log file exceeded 200 MB"; fail=$((fail+1)); }
    depth=$(echo "$max_live_q" | cut -f1)
    [ "${depth:-0}" -lt 1000 ] && { echo "[PASS] RMQ live queue <1000 sustained"; pass=$((pass+1)); } || { echo "[FAIL] RMQ live queue backed up"; fail=$((fail+1)); }
    [ "${max_npe_rate:-0}" -lt 1 ] && { echo "[PASS] NPE rate <1 MB/min"; pass=$((pass+1)); } || { echo "[FAIL] NPE burst detected"; fail=$((fail+1)); }
    echo
    echo "RESULT: $pass passed, $fail failed (of 4 auto-checkable criteria)"
    [ $fail -eq 0 ]
    exit $?
fi

# ---------- sample mode (default) ----------
TS=$(date '+%Y-%m-%d %H:%M:%S')

# 1. Disk top-level
read -r disk_used_mb disk_free_mb disk_pct <<<"$(df -BM --output=used,avail,pcent / | tail -1 | tr -d 'M%')"

# 2. Per-volume sizes
mysql_vol_mb=$(du -sBM /var/lib/docker/volumes/sunwinkr-mysql-data 2>/dev/null | cut -f1 | tr -d 'M')
mongo_vol_mb=$(du -sBM /var/lib/docker/volumes/sunwinkr-mongo-data 2>/dev/null | cut -f1 | tr -d 'M')
rmq_vol_mb=$(du -sBM /var/lib/docker/volumes/sunwinkr-rabbitmq-data 2>/dev/null | cut -f1 | tr -d 'M')

# 3. Repo logs dir + binh-specific files
logs_dir_mb=$(du -sBM "$REPO_ROOT/logs" 2>/dev/null | cut -f1 | tr -d 'M')
binh_main_log_mb=$(stat -c '%s' "$REPO_ROOT/logs/game-binh/main.log" 2>/dev/null | awk '{printf "%d", $1/1024/1024}')
binh_debug_log_mb=$(stat -c '%s' "$REPO_ROOT/logs/game-binh/debug.log" 2>/dev/null | awk '{printf "%d", $1/1024/1024}')

# 4. Mongo slot-log total docs (one count query covers all 5 target collections)
mongo_slot_docs_total=$(timeout 20 docker exec sunwinkr-mongodb mongosh -u "$MONGO_USER" -p "$MONGO_PASS" --authenticationDatabase admin --quiet --eval "
let db = db.getSiblingDB('win123club');
let t = 0;
['log_SieuAnhHung','log_KhoBau','log_NuDiepVien','log_VuongQuocVin','log_money_user_vin'].forEach(c => { try { t += db.getCollection(c).estimatedDocumentCount() || 0; } catch(e) {} });
print(t);
" 2>/dev/null | tail -1 | tr -dc '0-9')
mongo_slot_docs_total=${mongo_slot_docs_total:-0}

# 5. Max RMQ live queue depth (skip DLQs — we track scars separately)
# NOTE: rabbitmqctl output has header lines ("Timeout: 60.0 seconds...", "Listing queues...", "name\tmessages").
# Filter to rows that start with "queue_" so the numeric header tokens don't poison the max.
rmq_line=$(timeout 15 docker exec sunwinkr-rabbitmq rabbitmqctl list_queues name messages 2>/dev/null | awk '$1 ~ /^queue_/ && $1 !~ /_dlq$/ && $2+0 > max {max=$2+0; n=$1} END{print (max+0) "|" (n ? n : "none")}')
rmq_live_queue_max=$(echo "$rmq_line" | cut -d'|' -f1)
rmq_live_queue_max_name=$(echo "$rmq_line" | cut -d'|' -f2)
rmq_live_queue_max=${rmq_live_queue_max:-0}
rmq_live_queue_max_name=${rmq_live_queue_max_name:-none}

# 6. MySQL binlog file count (should stay bounded via 3-day retention)
binlog_files=$(docker exec sunwinkr-mysql sh -c 'ls /var/lib/mysql/binlog.[0-9]* 2>/dev/null | wc -l' 2>/dev/null | tr -dc '0-9')
binlog_files=${binlog_files:-0}

# 7. NPE rate in binh's debug.log over the last minute (stack-trace burst detector)
npe_last_min=$(tail -c 2M "$REPO_ROOT/logs/game-binh/debug.log" 2>/dev/null | awk -v t="$(date -d '1 minute ago' '+%Y-%m-%d %H:%M')" '$0 ~ t && /NullPointerException/' | wc -l)
npe_per_min=${npe_last_min:-0}

# Write one CSV line
printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
  "$TS" \
  "${disk_used_mb:-0}" "${disk_free_mb:-0}" "${disk_pct:-0}" \
  "${mysql_vol_mb:-0}" "${mongo_vol_mb:-0}" "${rmq_vol_mb:-0}" \
  "${logs_dir_mb:-0}" "${binh_main_log_mb:-0}" "${binh_debug_log_mb:-0}" \
  "$mongo_slot_docs_total" \
  "$rmq_live_queue_max" "$rmq_live_queue_max_name" \
  "$binlog_files" \
  "$npe_per_min" \
  >> "$CSV_FILE"

#!/bin/bash
# Log retention cleanup script - run daily via cron
# Cleans: MongoDB docs without create_time, old app log files, docker volume logs
#
# Usage: ./scripts/cleanup-logs.sh [days]  (default: 3)

RETENTION_DAYS=${1:-3}
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "$(date) - Starting log cleanup (retention: ${RETENTION_DAYS} days)"

# --- 1. MongoDB: Delete old docs without create_time (TTL can't clean these) ---
MONGO_USER=$(grep MONGO_USER "$SCRIPT_DIR/.env" | cut -d= -f2)
MONGO_PASS=$(grep MONGO_PASSWORD "$SCRIPT_DIR/.env" | cut -d= -f2)

if [ -n "$MONGO_USER" ] && docker ps --format '{{.Names}}' | grep -q sunwinkr-mongodb; then
  echo "[MongoDB] Cleaning docs older than ${RETENTION_DAYS} days without create_time..."
  docker exec sunwinkr-mongodb mongosh -u "$MONGO_USER" -p "$MONGO_PASS" --authenticationDatabase admin --quiet --eval "
    let mydb = db.getSiblingDB('win123club');
    let cols = mydb.getCollectionNames().filter(c => c.startsWith('log_') || c.startsWith('user_bet') || c.startsWith('bau_cua'));
    let cutoff = new Date(Date.now() - ${RETENTION_DAYS} * 24 * 3600 * 1000);
    let totalDeleted = 0;
    for (let c of cols) {
      try {
        let info = mydb.getCollectionInfos({name: c});
        if (info.length > 0 && info[0].type === 'view') continue;
        // Delete docs with no create_time and old _id (ObjectId encodes timestamp)
        let result = mydb.getCollection(c).deleteMany({
          create_time: { \$exists: false },
          _id: { \$lt: ObjectId.createFromTime(cutoff.getTime() / 1000) }
        });
        if (result.deletedCount > 0) {
          print(c + ': deleted ' + result.deletedCount + ' old docs without create_time');
          totalDeleted += result.deletedCount;
        }
      } catch(e) {}
    }
    print('Total deleted: ' + totalDeleted);
  " 2>/dev/null
fi

# --- 2. App logs: Delete .log files older than retention ---
echo "[App logs] Cleaning files older than ${RETENTION_DAYS} days..."
find "$SCRIPT_DIR/logs" -name "*.log.*" -mtime +${RETENTION_DAYS} -delete 2>/dev/null
find "$SCRIPT_DIR/logs" -name "*.csv.*" -mtime +${RETENTION_DAYS} -delete 2>/dev/null
find "$SCRIPT_DIR/backend-master/logs" -name "*.log.*" -mtime +${RETENTION_DAYS} -delete 2>/dev/null
find "$SCRIPT_DIR/backend-master/logs" -name "*.csv.*" -mtime +${RETENTION_DAYS} -delete 2>/dev/null

# Count cleaned
CLEANED=$(find "$SCRIPT_DIR/logs" "$SCRIPT_DIR/backend-master/logs" -name "*.log.*" -mtime +${RETENTION_DAYS} 2>/dev/null | wc -l)
echo "  Remaining old files: $CLEANED"

# --- 3. Truncate active logs if > 100MB ---
echo "[Active logs] Truncating logs > 100MB..."
find "$SCRIPT_DIR/logs" "$SCRIPT_DIR/backend-master/logs" -name "*.log" -size +100M 2>/dev/null | while read f; do
  SIZE=$(du -h "$f" | awk '{print $1}')
  echo "  Truncating $f ($SIZE)"
  : > "$f"
done

echo "$(date) - Cleanup complete"

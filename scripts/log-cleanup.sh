#!/bin/bash
# =============================================================================
# Log Cleanup - Delete files older than 30 days
# =============================================================================
# Run daily via cron: 0 3 * * * /root/sunwinkr/sunwinkr/scripts/log-cleanup.sh
# =============================================================================

LOG_DIR="${1:-/root/sunwinkr/sunwinkr/logs}"
RETENTION_DAYS=30

echo "[$(date)] Cleaning logs older than ${RETENTION_DAYS} days in ${LOG_DIR}"

count=$(find "$LOG_DIR" -type f \( -name "*.log*" -o -name "*.csv*" -o -name "*.txt*" \) -mtime +${RETENTION_DAYS} | wc -l)
find "$LOG_DIR" -type f \( -name "*.log*" -o -name "*.csv*" -o -name "*.txt*" \) -mtime +${RETENTION_DAYS} -delete
find "$LOG_DIR" -type d -empty -delete 2>/dev/null

echo "[$(date)] Deleted $count files"

#!/bin/bash
# =============================================================================
# Wallet drift snapshot — hourly cron that populates wallet_drift_snapshot
# from v_wallet_drift (users.vin vs money_account PLAYER_VIN balance).
#
# Phase 0 created the table + view but nothing populates the table. This
# script closes that gap:
#
#   1. Insert one row per hour into vinplay.wallet_drift_snapshot
#      (total_users, drifting_users, max_abs_drift, sum_abs_drift).
#   2. Write a Prometheus textfile snapshot to
#      /var/lib/node_exporter/textfile_collector/wallet_drift.prom so the
#      existing node-exporter scrape surfaces it in Grafana.
#   3. Fire a Telegram alert when drifting_users > 0 OR sum_abs_drift > 1000
#      (the v2 RFC addendum H6 gate).
#
# Style matches scripts/ledger-drift-alarm.sh and scripts/disk-guard.sh:
#   - reads MYSQL_ROOT_PASSWORD / TELEGRAM_* from .env
#   - mysql via `docker exec sunwinkr-mysql mysql -N ...`
#   - prints one summary line per run for cron mail / journald
#
# Cron: 0 * * * * /root/sunwinkr/sunwinkr/scripts/wallet-drift-snapshot.sh
# =============================================================================

set -u

# ENV_FILE supports both dev (sunwinkr/) and prod (sunwinkr-backend/) layouts.
ENV_FILE="${ENV_FILE:-}"
if [ -z "$ENV_FILE" ]; then
    for candidate in \
        "/root/sunwinkr/sunwinkr/.env" \
        "/root/sunwinkr/sunwinkr-backend/.env" \
        "$(dirname "$(readlink -f "$0")")/../.env"
    do
        if [ -f "$candidate" ]; then ENV_FILE="$candidate"; break; fi
    done
fi
if [ -z "$ENV_FILE" ] || [ ! -f "$ENV_FILE" ]; then
    echo "wallet-drift-snapshot: cannot locate .env" >&2
    exit 2
fi

MYSQL_PWD="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
TG_TOKEN="$(grep '^TELEGRAM_BOT_TOKEN=' "$ENV_FILE" | cut -d= -f2- || true)"
TG_CHAT="$(grep '^TELEGRAM_OPS_CHAT_ID=' "$ENV_FILE" | cut -d= -f2- || true)"

# Thresholds — v2 RFC addendum H6.
ALERT_DRIFT_USERS="${WALLET_DRIFT_ALERT_USERS:-0}"     # >0 fires alert
ALERT_SUM_ABS="${WALLET_DRIFT_ALERT_SUM_VND:-1000}"    # >1000 VND fires alert

PROM_DIR="${PROM_TEXTFILE_DIR:-/var/lib/node_exporter/textfile_collector}"
PROM_FILE="$PROM_DIR/wallet_drift.prom"

mysql_q() {
    docker exec sunwinkr-mysql mysql -N -uroot -p"$MYSQL_PWD" vinplay -e "$1" \
        2> >(grep -v '\[Warning\] Using a password' >&2)
}

send_telegram() {
    [ -z "$TG_TOKEN" ] && { echo "wallet-drift-snapshot: TG_TOKEN unset; skipping alert" >&2; return 0; }
    [ -z "$TG_CHAT" ]  && { echo "wallet-drift-snapshot: TG_OPS_CHAT_ID unset; skipping alert" >&2; return 0; }
    curl -sS --max-time 10 \
        "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
        --data-urlencode "chat_id=${TG_CHAT}" \
        --data-urlencode "text=$1" \
        --data-urlencode "parse_mode=HTML" >/dev/null || true
}

# ---------- 1. Insert snapshot row + capture computed values --------------
# Single round-trip: the INSERT ... SELECT computes from v_wallet_drift + users
# atomically. Then we read back the row we just wrote so the Prometheus
# textfile and the alert see EXACTLY the values that landed in the table
# (no race with a writer between INSERT and a separate SELECT COUNT).
INSERT_SQL=$(cat <<'SQL'
INSERT INTO vinplay.wallet_drift_snapshot
    (total_users, drifting_users, max_abs_drift, sum_abs_drift)
SELECT
    (SELECT COUNT(*) FROM vinplay.users WHERE is_bot = 0)        AS total_users,
    (SELECT COUNT(*) FROM vinplay.v_wallet_drift)                AS drifting_users,
    (SELECT IFNULL(MAX(ABS(drift_vnd)), 0) FROM vinplay.v_wallet_drift) AS max_abs_drift,
    (SELECT IFNULL(SUM(ABS(drift_vnd)), 0) FROM vinplay.v_wallet_drift) AS sum_abs_drift;
SELECT snapshot_id, total_users, drifting_users, max_abs_drift, sum_abs_drift
  FROM vinplay.wallet_drift_snapshot
 ORDER BY snapshot_id DESC LIMIT 1;
SQL
)
ROW="$(mysql_q "$INSERT_SQL" | tail -n 1)"
if [ -z "$ROW" ]; then
    echo "wallet-drift-snapshot: insert returned no row (mysql unreachable?)" >&2
    exit 3
fi

# tab-separated from mysql -N
SNAP_ID=$(echo "$ROW"   | awk '{print $1}')
TOTAL=$(echo "$ROW"     | awk '{print $2}')
DRIFTING=$(echo "$ROW"  | awk '{print $3}')
MAX_ABS=$(echo "$ROW"   | awk '{print $4}')
SUM_ABS=$(echo "$ROW"   | awk '{print $5}')

# ---------- 2. Prometheus textfile collector ------------------------------
# Atomic write: render to .tmp on the same filesystem, then mv. node_exporter
# detects partial files via mtime, so this avoids a half-written scrape.
if mkdir -p "$PROM_DIR" 2>/dev/null; then
    TMP="${PROM_FILE}.tmp.$$"
    {
        echo "# HELP wallet_drift_total_users Number of non-bot users compared against the ledger."
        echo "# TYPE wallet_drift_total_users gauge"
        echo "wallet_drift_total_users ${TOTAL}"
        echo "# HELP wallet_drift_drifting_users Users whose users.vin != ledger PLAYER_VIN balance."
        echo "# TYPE wallet_drift_drifting_users gauge"
        echo "wallet_drift_drifting_users ${DRIFTING}"
        echo "# HELP wallet_drift_max_abs_vnd Largest absolute drift across all users (VND)."
        echo "# TYPE wallet_drift_max_abs_vnd gauge"
        echo "wallet_drift_max_abs_vnd ${MAX_ABS}"
        echo "# HELP wallet_drift_sum_abs_vnd Sum of absolute drift across all users (VND)."
        echo "# TYPE wallet_drift_sum_abs_vnd gauge"
        echo "wallet_drift_sum_abs_vnd ${SUM_ABS}"
        echo "# HELP wallet_drift_snapshot_id Last snapshot row id written."
        echo "# TYPE wallet_drift_snapshot_id counter"
        echo "wallet_drift_snapshot_id ${SNAP_ID}"
        echo "# HELP wallet_drift_last_run_timestamp_seconds Unix time of last successful run."
        echo "# TYPE wallet_drift_last_run_timestamp_seconds gauge"
        echo "wallet_drift_last_run_timestamp_seconds $(date +%s)"
    } > "$TMP"
    mv -f "$TMP" "$PROM_FILE"
else
    echo "wallet-drift-snapshot: PROM_DIR=${PROM_DIR} not writable; metrics skipped" >&2
fi

# ---------- 3. Alerting --------------------------------------------------
ALERT=0
if [ "$DRIFTING" -gt "$ALERT_DRIFT_USERS" ]; then ALERT=1; fi
if [ "$SUM_ABS"  -gt "$ALERT_SUM_ABS" ];     then ALERT=1; fi

if [ "$ALERT" -eq 1 ]; then
    msg="⚠️ <b>WALLET DRIFT (Phase 0 monitor)</b>%0A"
    msg="${msg}snapshot_id=<b>${SNAP_ID}</b>%0A"
    msg="${msg}total_users=${TOTAL}  drifting_users=<b>${DRIFTING}</b>%0A"
    msg="${msg}max_abs_drift=${MAX_ABS} VND  sum_abs_drift=<b>${SUM_ABS}</b> VND%0A"
    msg="${msg}Thresholds: drifting_users>${ALERT_DRIFT_USERS} OR sum_abs_drift>${ALERT_SUM_ABS}%0A"
    msg="${msg}Run: SELECT * FROM vinplay.v_wallet_drift ORDER BY ABS(drift_vnd) DESC LIMIT 20;"
    send_telegram "$msg"
fi

echo "wallet-drift-snapshot: snapshot_id=${SNAP_ID} total=${TOTAL} drifting=${DRIFTING} max_abs=${MAX_ABS} sum_abs=${SUM_ABS} alert=${ALERT}"

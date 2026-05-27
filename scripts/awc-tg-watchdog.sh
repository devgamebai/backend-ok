#!/usr/bin/env bash
# Cron-friendly watchdog for the AWC Telegram listener.
#
# What it does (every cron tick — recommend 1m):
#  - Verifies the awc_tg.py listener pid is alive.
#  - Restarts it as a daemon if missing (pkill any stale procs first).
#  - Writes a heartbeat to /tmp/awc-tg-watchdog.heartbeat so an external
#    monitor (or another cron) can spot prolonged silence.
#  - Best-effort: writes nothing on healthy ticks, prints a one-liner only
#    when it actually had to act. Cron mail box stays clean unless the
#    listener was actually down.
#
# Install:
#   sudo crontab -e
#   * * * * * /root/sunwinkr/sunwinkr/scripts/awc-tg-watchdog.sh >> /tmp/awc-tg-watchdog.log 2>&1
#
# Logs:
#   /tmp/awc-tg-watchdog.log         (cron stdout/stderr — only events)
#   /tmp/awc-tg-listener.out         (listener's own stdout, kept by listener)
#   /tmp/awc-tg.log                  (per-message TG transcript, written by listener)

set -uo pipefail

CHAT_ID="${AWC_TG_CHAT_ID:--5175104147}"
LISTENER_OUT=/tmp/awc-tg-listener.out
HEARTBEAT=/tmp/awc-tg-watchdog.heartbeat
SCRIPT_PY=/root/sunwinkr/sunwinkr/scripts/awc_tg.py

date -u +%FT%TZ > "${HEARTBEAT}" 2>/dev/null || true

if pgrep -f "awc_tg.py listen ${CHAT_ID}" >/dev/null 2>&1; then
    exit 0
fi

# Kill any stale listeners (different chat id, hung process, etc.)
pkill -9 -f "awc_tg.py listen" >/dev/null 2>&1 || true
sleep 1

nohup python3 "${SCRIPT_PY}" listen "${CHAT_ID}" >> "${LISTENER_OUT}" 2>&1 &
disown $! 2>/dev/null || true

sleep 3
if pgrep -f "awc_tg.py listen ${CHAT_ID}" >/dev/null 2>&1; then
    echo "$(date -u +%FT%TZ) listener restarted"
    exit 0
else
    echo "$(date -u +%FT%TZ) listener FAILED to start — see ${LISTENER_OUT}" >&2
    exit 1
fi

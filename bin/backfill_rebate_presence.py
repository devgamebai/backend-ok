#!/usr/bin/env python3
"""
Presence-only backfill for rebate_logs.

Reads bet events from log_money_user_vin (mongo) within a date window,
checks if a corresponding rebate_logs row exists, and if not inserts a
single stub row representing "this bet happened" — amount=0, no cascade,
no agency_wallet credit. Pure visibility recovery.

Idempotency: uses note prefix 'BACKFILL_PRESENCE_v1 ' + sourceKey. Re-run
won't duplicate rows. Production tx already-landed rows (with notes like
'AUTO_COMMISSION source=...') are also detected via sourceKey substring,
so we don't double-insert when the original cascade DID land.

Output: prints one progress line per 500 events processed, with running
counts: scanned / already_present / inserted / skipped (no agent / no
volume / etc.).

Designed to be safe: SELECT-then-INSERT-IF-MISSING per row, no UPDATEs,
no money side-effects. Max insert rate is throttled by MySQL roundtrip
(~50/sec).

Run inside the audit-bot container (has pymongo + mysql-connector + creds):
    docker exec sunwinkr-audit-bot python3 /app/backfill_rebate_presence.py \\
        --start 2026-04-24 --end 2026-04-29
"""

import argparse
import os
import sys
import time
from datetime import datetime, timezone, timedelta

import pymongo
import mysql.connector


def env(name, default=None, required=False):
    v = os.environ.get(name, default)
    if required and not v:
        print(f"ERROR: env {name} is required", file=sys.stderr)
        sys.exit(2)
    return v


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", required=True, help="ISO date (UTC), inclusive: 2026-04-24")
    ap.add_argument("--end",   required=True, help="ISO date (UTC), exclusive: 2026-04-29")
    ap.add_argument("--batch", type=int, default=500, help="mongo cursor batch + progress print interval")
    ap.add_argument("--dry-run", action="store_true", help="don't write")
    args = ap.parse_args()

    start_dt = datetime.fromisoformat(args.start).replace(tzinfo=timezone.utc)
    end_dt   = datetime.fromisoformat(args.end).replace(tzinfo=timezone.utc)
    print(f"[backfill] window: [{start_dt.isoformat()}, {end_dt.isoformat()})")

    # ── connect ──────────────────────────────────────────────────────
    mongo = pymongo.MongoClient(
        f"mongodb://{env('MONGO_USER', required=True)}:{env('MONGO_PASSWORD', required=True)}"
        f"@{env('MONGO_HOST', 'mongodb')}:{int(env('MONGO_PORT', '27017'))}/admin",
        serverSelectionTimeoutMS=10000,
        connectTimeoutMS=10000,
        socketTimeoutMS=120000,
        retryReads=True,
        retryWrites=True,
        maxPoolSize=20,
    )
    db = mongo[env("MONGO_DB", "win123club")]

    sql = mysql.connector.connect(
        host=env("MYSQL_HOST", required=True),
        user=env("MYSQL_USER", required=True),
        password=env("MYSQL_PASSWORD", required=True),
        database="vinplay",
        autocommit=False,
    )
    cur = sql.cursor()

    # ── pre-build (player_nick → user_id) lookup, single round-trip ──
    cur.execute("SELECT user_name, nick_name, id FROM vinplay.users")
    user_by_nick = {nick: uid for (un, nick, uid) in cur.fetchall() if nick}
    print(f"[backfill] users cached: {len(user_by_nick)}")

    INSERT_SQL = """
        INSERT INTO vinplay.rebate_logs
            (agent_user_id, agent_nickname, agent_level, period_start, period_end, period_type,
             total_f1_volume, rebate_percentage, share_percentage, own_percentage, child_percentage, differential_pct,
             rebate_amount, share_amount, net_rebate, status, note, rebate_type, created_at)
        VALUES (%s, %s, 0, %s, %s, 'DAILY',
                %s, 0, 0, 0, 0, 0,
                0.00, 0, 0.00, 'PAID', %s, 'SELF', %s)
    """

    EXISTS_SQL = "SELECT 1 FROM vinplay.rebate_logs WHERE note LIKE %s LIMIT 1"

    scanned = 0
    inserted = 0
    already = 0
    skipped_no_user = 0
    skipped_no_volume = 0
    skipped_not_bet = 0
    last_print = time.time()

    cursor = db.log_money_user_vin.find(
        {
            "create_time": {"$gte": start_dt, "$lt": end_dt},
            "is_bot": {"$ne": True},
            "play_game": True,
            "money_exchange": {"$exists": True, "$ne": 0},
        },
        no_cursor_timeout=False,
        batch_size=args.batch,
    ).sort("create_time", 1)

    try:
        for doc in cursor:
            scanned += 1

            nick = doc.get("nick_name")
            action = doc.get("action_name")
            service = doc.get("service_name") or ""
            money = doc.get("money_exchange")
            create_time = doc.get("create_time")
            trans_time = doc.get("trans_time")

            if money is None:
                skipped_not_bet += 1
                continue

            money_int = int(money) if not isinstance(money, dict) else int(money.get("low", 0))
            if money_int >= 0:  # only bets (debit, money_exchange < 0)
                skipped_not_bet += 1
                continue
            volume = abs(money_int)

            if not nick or nick not in user_by_nick:
                skipped_no_user += 1
                continue
            user_id = user_by_nick[nick]

            if volume <= 0:
                skipped_no_volume += 1
                continue

            # sourceKey = the unique per-message identifier as built by
            # LogMoneyUserExtraProcessor.buildSourceKey(): if msg.id is
            # set, use that; else nick|action|service|money|create_time.
            # log_money_user_vin doesn't have msg.id so we use the fallback.
            source_key = f"{nick}|{action}|{service}|{money_int}|{trans_time or create_time.strftime('%Y-%m-%d %H:%M:%S')}"
            note = f"BACKFILL_PRESENCE_v1 source={source_key} type=SELF user={nick} action={action} service={service}"

            # idempotency: skip if a row with this sourceKey already exists
            # (matches both backfill rows AND legitimate AUTO_COMMISSION rows
            # that landed during the gap)
            cur.execute(EXISTS_SQL, (f"%source={source_key}%",))
            if cur.fetchone():
                already += 1
                if scanned % args.batch == 0 or (time.time() - last_print) > 10:
                    _print_progress(scanned, inserted, already, skipped_no_user, skipped_no_volume, skipped_not_bet)
                    last_print = time.time()
                continue

            if args.dry_run:
                inserted += 1
            else:
                period_start = create_time.strftime("%Y-%m-%d 00:00:00")
                period_end   = create_time.strftime("%Y-%m-%d 23:59:59")
                created_at   = create_time.strftime("%Y-%m-%d %H:%M:%S")
                cur.execute(INSERT_SQL, (
                    user_id, nick, period_start, period_end,
                    volume, note, created_at,
                ))
                inserted += 1
                if inserted % 100 == 0:
                    sql.commit()

            if scanned % args.batch == 0 or (time.time() - last_print) > 10:
                _print_progress(scanned, inserted, already, skipped_no_user, skipped_no_volume, skipped_not_bet)
                last_print = time.time()

        sql.commit()
    finally:
        cursor.close()

    print()
    print("[backfill] DONE")
    _print_progress(scanned, inserted, already, skipped_no_user, skipped_no_volume, skipped_not_bet)


def _print_progress(scanned, inserted, already, no_user, no_volume, not_bet):
    print(f"  scanned={scanned}  inserted={inserted}  already={already}  "
          f"skip_no_user={no_user} skip_no_volume={no_volume} skip_not_bet={not_bet}",
          flush=True)


if __name__ == "__main__":
    main()

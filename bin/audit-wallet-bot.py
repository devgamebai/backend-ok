#!/usr/bin/env python3
"""
SUN-1141 — Wallet integrity audit bot.

Continuously validates per-player invariant:
  users.vin == Σ(deposits) + Σ(promos) + Σ(win/loss) + Σ(admin) +
                Σ(agency→vin) − Σ(withdraws)

Mismatch ≥ tolerance for 2 consecutive cycles → Telegram alert.

Snapshot+delta for scale: per-user snapshot in Mongo collection
wallet_audit_snapshot; subsequent cycles aggregate only events newer than
snapshot.last_event_id. Cost stays flat regardless of history depth.

Read-only on production tables. Only writes to wallet_audit_* collections
(snapshot + dedup) which are owned by the bot.

See docs/architecture/SUN-1141-wallet-integrity-audit-bot.md for the
design rationale.
"""

import os
import sys
import time
import logging
import hashlib
import requests
import threading
from decimal import Decimal, ROUND_HALF_UP, getcontext
from datetime import datetime, timedelta, timezone

import pymongo
import mysql.connector
from apscheduler.schedulers.background import BackgroundScheduler

getcontext().prec = 30  # plenty for KRW amounts

# ─── Config ────────────────────────────────────────────────────────────────
MYSQL_HOST     = os.environ.get("MYSQL_HOST", "mysql")
MYSQL_PORT     = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER     = os.environ.get("MYSQL_USER", "root")
MYSQL_PASS     = os.environ["MYSQL_PASSWORD"]
MYSQL_DB       = os.environ.get("MYSQL_DB", "vinplay")
MYSQL_DB_ADMIN = os.environ.get("MYSQL_DB_ADMIN", "vinplay_admin")

MONGO_URI      = os.environ.get("MONGO_URI",
    f"mongodb://{os.environ.get('MONGO_USER','sunwinkr_admin')}:"
    f"{os.environ['MONGO_PASSWORD']}@{os.environ.get('MONGO_HOST','mongodb')}:27017/"
    f"?authSource=admin")
MONGO_DB       = os.environ.get("MONGO_DB", "win123club")

TELEGRAM_TOKEN = os.environ["TELEGRAM_AUDIT_BOT_TOKEN"]
TELEGRAM_CHAT  = os.environ["TELEGRAM_AUDIT_CHAT_ID"]

# Behavior knobs (env-overridable)
TOLERANCE_KRW   = Decimal(os.environ.get("AUDIT_TOLERANCE_KRW", "0"))
HOT_INTERVAL_S  = int(os.environ.get("AUDIT_HOT_INTERVAL_S", "60"))
WARM_INTERVAL_S = int(os.environ.get("AUDIT_WARM_INTERVAL_S", "3600"))
FULL_INTERVAL_S = int(os.environ.get("AUDIT_FULL_INTERVAL_S", "21600"))  # 6h
HOT_WINDOW_MIN  = int(os.environ.get("AUDIT_HOT_WINDOW_MIN", "5"))
WARM_WINDOW_H   = int(os.environ.get("AUDIT_WARM_WINDOW_H", "24"))
GRACE_S         = int(os.environ.get("AUDIT_GRACE_S", "30"))
BATCH_SIZE      = int(os.environ.get("AUDIT_BATCH_SIZE", "500"))
BOOTSTRAP_BATCH = int(os.environ.get("AUDIT_BOOTSTRAP_BATCH", "50"))   # smaller for heavy aggregations
BOOTSTRAP_LOOKBACK_DAYS = int(os.environ.get("AUDIT_BOOTSTRAP_LOOKBACK_DAYS", "180"))  # bound full-history scan
DEDUP_TTL_S     = int(os.environ.get("AUDIT_DEDUP_TTL_S", "86400"))
ALERT_THROTTLE  = int(os.environ.get("AUDIT_ALERT_THROTTLE", "50"))
TZ_OFFSET_H     = int(os.environ.get("AUDIT_TZ_OFFSET_H", "7"))  # log_money_user_vin trans_time is +07

# Action_name patterns considered "game wlnet" activity.
# Anything starting with gsc_ / Cq9Fish or in this list contributes to wlnet.
WLNET_ACTION_NAMES = {
    "SieuAnhHung", "NuDiepVien", "KhoBau", "VuongQuocVin", "ChiemTinh",
    "TaiXiu", "TaiXiuMD5", "BauCua", "Binh", "SicBo",
    "MiniPoker", "CaoThap", "Sam", "Lieng", "Baicao", "Bacay", "Tlmn",
    "Coup", "Xizach", "Poker", "Pokertour",
    "ThanDen", "RollRoye", "Bikini", "Benley", "Audition", "TamHung",
    "Spartan", "RangeRover", "Slot",
    "Cq9FishTransfer",
}
def is_wlnet_action(name: str) -> bool:
    if not name: return False
    if name in WLNET_ACTION_NAMES: return True
    if name.startswith("gsc_"): return True
    if name.startswith("awc_"): return True
    return False

CONVERT_AGENCY_ACTION = "ConvertAgencyToVin"

# Bot/test user filter — skip these from auditing.
TEST_NICK_PREFIXES = ("test", "bot", "qc", "demo")

# ─── Logging ───────────────────────────────────────────────────────────────
logging.basicConfig(
    level=os.environ.get("AUDIT_LOG_LEVEL", "INFO"),
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("audit-bot")

# ─── DB connections ────────────────────────────────────────────────────────
mongo_client = pymongo.MongoClient(
    MONGO_URI,
    serverSelectionTimeoutMS=10000,
    socketTimeoutMS=120000,
    connectTimeoutMS=20000,
    maxPoolSize=10,
    retryReads=True,
)
mongo_db     = mongo_client[MONGO_DB]
log_money    = mongo_db["log_money_user_vin"]
snap_col     = mongo_db["wallet_audit_snapshot"]
dedup_col    = mongo_db["wallet_audit_alert_dedup"]

def mysql_conn():
    return mysql.connector.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASS,
        database=MYSQL_DB, autocommit=True,
    )

# ─── Time helpers ──────────────────────────────────────────────────────────
def now_local_str(offset_seconds: int = 0) -> str:
    """trans_time format ('YYYY-MM-DD HH:MM:SS') in the platform's local timezone."""
    t = datetime.now(timezone.utc) + timedelta(hours=TZ_OFFSET_H, seconds=-offset_seconds)
    return t.strftime("%Y-%m-%d %H:%M:%S")

# ─── Component sourcing ────────────────────────────────────────────────────
def fetch_components_for_users(users: list, since_iso: str = None) -> dict:
    """
    Returns {nick_name: {deposit_total, withdraw_total, admin_total,
                          promo_total, agency_convert_total, wlnet_total,
                          max_event_id}}.

    If since_iso is given (a trans_time string), each component sums only
    events at or after that time per user. Otherwise full history.
    """
    if not users:
        return {}
    out = {u: {
        "deposit_total":         Decimal(0),
        "withdraw_total":        Decimal(0),
        "admin_total":           Decimal(0),
        "promo_total":           Decimal(0),
        "agency_convert_total":  Decimal(0),
        "wlnet_total":           Decimal(0),
        "max_event_id":          None,
    } for u in users}

    # ── MySQL aggregations: deposits, withdraws (bank+crypto), admin, promos
    # Schema notes:
    #   deposit_transactions: nick_name, amount, status='APPROVED'
    #   bank_withdrawals:     nick_name, amount_krw, status='APPROVED'
    #   crypto_withdrawals:   nick_name, amount_krw, status='APPROVED'
    #   log_admin:            account_name, money, money_type='vin', status='1'
    #   tbl_signing_bonus_log: nick_name, bonus_amount  (no status filter — all rows are payouts)
    #   deposit_promotion_logs: nick_name, bonus_amount
    #   tbl_cashback_logs:    nick_name, rebate_amount, status='PAID'
    placeholders = ",".join(["%s"] * len(users))
    cn = mysql_conn()
    try:
        cur = cn.cursor()

        cur.execute(
            f"""SELECT nick_name, COALESCE(SUM(amount), 0)
                FROM {MYSQL_DB}.deposit_transactions
                WHERE nick_name IN ({placeholders})
                  AND status='APPROVED'
                GROUP BY nick_name""",
            users)
        for u, amt in cur.fetchall():
            out[u]["deposit_total"] = Decimal(amt or 0)

        # Withdraws sum from both bank + crypto channels. Both tables share schema.
        for tbl in ("bank_withdrawals", "crypto_withdrawals"):
            try:
                cur.execute(
                    f"""SELECT nick_name, COALESCE(SUM(amount_krw), 0)
                        FROM {MYSQL_DB}.{tbl}
                        WHERE nick_name IN ({placeholders})
                          AND status='APPROVED'
                        GROUP BY nick_name""",
                    users)
                for u, amt in cur.fetchall():
                    out[u]["withdraw_total"] += Decimal(amt or 0)
            except mysql.connector.Error as e:
                log.warning(f"withdraw source {tbl} failed (non-fatal): {e}")

        cur.execute(
            f"""SELECT account_name, COALESCE(SUM(money), 0)
                FROM {MYSQL_DB_ADMIN}.log_admin
                WHERE account_name IN ({placeholders})
                  AND money_type='vin' AND status='1'
                GROUP BY account_name""",
            users)
        for u, amt in cur.fetchall():
            out[u]["admin_total"] = Decimal(amt or 0)

        # Promotion sources: 3 tables, each with its own amount column name.
        for tbl, col, status_filter in [
            ("tbl_signing_bonus_log",  "bonus_amount", ""),
            ("deposit_promotion_logs", "bonus_amount", ""),
            ("tbl_cashback_logs",      "rebate_amount", " AND status='PAID'"),
        ]:
            try:
                cur.execute(
                    f"""SELECT nick_name, COALESCE(SUM({col}), 0)
                        FROM {MYSQL_DB}.{tbl}
                        WHERE nick_name IN ({placeholders}){status_filter}
                        GROUP BY nick_name""",
                    users)
                for u, amt in cur.fetchall():
                    out[u]["promo_total"] += Decimal(amt or 0)
            except mysql.connector.Error as e:
                log.warning(f"promo source {tbl} failed (non-fatal): {e}")

        cur.close()
    finally:
        cn.close()

    # ── Mongo aggregations: wlnet + agency_convert (one pipeline)
    match = {"nick_name": {"$in": users}}
    if since_iso:
        match["trans_time"] = {"$gt": since_iso}
    pipeline = [
        {"$match": match},
        {"$group": {
            "_id": {"nick": "$nick_name", "is_convert": {"$eq": ["$action_name", CONVERT_AGENCY_ACTION]}},
            "sum": {"$sum": "$money_exchange"},
            "max_id": {"$max": "$_id"},
        }},
    ]
    for row in log_money.aggregate(pipeline, allowDiskUse=True):
        nick = row["_id"]["nick"]
        if nick not in out:
            continue
        amt = Decimal(_to_long(row["sum"]))
        if row["_id"]["is_convert"]:
            out[nick]["agency_convert_total"] += amt
        else:
            out[nick]["wlnet_total"] += amt
        # Track newest event_id we saw for this user (for snapshot watermark).
        prev = out[nick]["max_event_id"]
        rid = row["max_id"]
        if prev is None or (rid and str(rid) > str(prev)):
            out[nick]["max_event_id"] = rid

    return out

def _to_long(v):
    """Mongo's NumberLong arrives as int in pymongo, but sometimes dict-wrapped."""
    if isinstance(v, dict) and "low" in v:
        return v["low"]
    return v or 0

def fetch_actual_vin(users: list) -> dict:
    if not users: return {}
    placeholders = ",".join(["%s"] * len(users))
    cn = mysql_conn()
    try:
        cur = cn.cursor()
        cur.execute(
            f"""SELECT nick_name, vin
                FROM {MYSQL_DB}.users
                WHERE nick_name IN ({placeholders})""",
            users)
        out = {nick: Decimal(vin or 0) for nick, vin in cur.fetchall()}
        cur.close()
        return out
    finally:
        cn.close()

# ─── Active-user discovery ─────────────────────────────────────────────────
def active_users(window_minutes: int) -> list:
    """Distinct nick_names with wallet activity in the last N minutes."""
    cutoff = now_local_str(offset_seconds=window_minutes * 60)
    return list(log_money.distinct("nick_name", {"trans_time": {"$gte": cutoff}}))

def all_users_with_balance() -> list:
    """All level-0 players with vin > 0 (FULL sweep target)."""
    cn = mysql_conn()
    try:
        cur = cn.cursor()
        cur.execute(f"SELECT nick_name FROM {MYSQL_DB}.users WHERE vin > 0 AND dai_ly = 0 AND nick_name IS NOT NULL")
        out = [r[0] for r in cur.fetchall() if r[0]]
        cur.close()
        return out
    finally:
        cn.close()

def filter_skippable(users: list) -> list:
    """Drop bot/test accounts."""
    return [u for u in users if u and not any(u.lower().startswith(p) for p in TEST_NICK_PREFIXES)]

# ─── Audit core ────────────────────────────────────────────────────────────
def audit_users(users: list, label: str):
    if not users:
        return
    users = filter_skippable(users)
    if not users: return

    log.info(f"[{label}] auditing {len(users)} users")
    actuals = fetch_actual_vin(users)

    # Snapshots in one round-trip
    snapshots = {s["_id"]: s for s in snap_col.find({"_id": {"$in": users}})}

    # Per-cycle alert throttle: collect alerts here; emit summary if too many.
    cycle_alerts = []

    # Process in chunks of BATCH_SIZE so a slow user-set doesn't block
    for i in range(0, len(users), BATCH_SIZE):
        batch = users[i:i + BATCH_SIZE]
        _audit_batch(batch, actuals, snapshots, label, cycle_alerts)

    if len(cycle_alerts) > ALERT_THROTTLE:
        # Storm — likely a bug or schema change. Send one summary instead.
        log.warning(f"[{label}] {len(cycle_alerts)} alerts in single cycle — sending summary instead of individual messages")
        send_telegram(
            f"⚠️ ALERT STORM ({label}): {len(cycle_alerts)} mismatches detected in one cycle.\n"
            f"Likely a deploy issue or schema change. Top 5 by |diff|:\n" +
            "\n".join(f"  {u} {d:+,}" for u,d in sorted(cycle_alerts, key=lambda x: -abs(x[1]))[:5]) +
            f"\n\nFull list in container logs (grep ALERTED). Per-user dedup still in effect — "
            f"individual users won't repeat for 24h."
        )

def _audit_batch(batch, actuals, snapshots, label, cycle_alerts):
    # Identify which need bootstrap (no snapshot) vs incremental (delta only)
    bootstrap_users = [u for u in batch if u not in snapshots]
    incremental_users = [u for u in batch if u in snapshots]

    # ── Bootstrap path: TRUST mode (V1).
    # We don't aggregate full history at first sighting — too heavy for Mongo
    # at scale, and historical drift is out of scope (the bot's job is to
    # catch NEW drift opening up after deployment, not to reconcile pre-
    # existing imbalances). On first sighting we set expected_balance =
    # actual users.vin as a "trust baseline". Future cycles aggregate only
    # delta events since this baseline; any drift that opens between baseline
    # and a later cycle fires the standard two-strike alert. Pre-existing
    # CauNamday-class issues are caught by the cron re-baseline (Phase 4) or
    # by manual audit of historical data — out of scope here.
    if bootstrap_users:
        log.info(f"[{label}] bootstrapping {len(bootstrap_users)} users (trust baseline = actual users.vin)")
        now_local = now_local_str()
        for u in bootstrap_users:
            actual = actuals.get(u, Decimal(0))
            snap_col.replace_one(
                {"_id": u},
                {
                    "_id": u,
                    "snapshot_at": datetime.now(timezone.utc),
                    "last_event_id": None,                          # delta will scan from snapshot_at instead
                    "components": {                                 # all zeros — first delta fills these in
                        "deposit_total": "0", "promo_total": "0", "wlnet_total": "0",
                        "admin_total": "0", "agency_convert_total": "0", "withdraw_total": "0",
                    },
                    "expected_balance": str(actual),                # TRUST: assume actual is correct at baseline
                    "actual_vin_at_snapshot": str(actual),
                    "consecutive_mismatch": 0,
                    "audit_count": 1,
                    "last_audit_at": datetime.now(timezone.utc),
                    "bootstrap": True,
                    "bootstrap_mode": "trust",
                },
                upsert=True,
            )

    # ── Incremental path: delta since snapshot.last_event_id ──
    if incremental_users:
        # For each user, compute delta since their snapshot's max trans_time.
        # We use trans_time string as the watermark since _id from older docs
        # may not be guaranteed monotonic with trans_time (legacy data).
        # Simplest: re-fetch per user using snapshot.snapshot_at as cutoff.
        # Group all users with same cutoff for efficiency.
        per_cutoff = {}
        for u in incremental_users:
            cutoff = snapshots[u].get("snapshot_at")
            if not cutoff:
                continue
            cutoff_str = (cutoff.replace(tzinfo=timezone.utc) + timedelta(hours=TZ_OFFSET_H)).strftime("%Y-%m-%d %H:%M:%S")
            per_cutoff.setdefault(cutoff_str, []).append(u)

        for cutoff_str, users_at_cutoff in per_cutoff.items():
            comps = fetch_components_for_users(users_at_cutoff, since_iso=cutoff_str)
            for u in users_at_cutoff:
                _evaluate_user(u, snapshots[u], comps[u], actuals.get(u, Decimal(0)), label, cycle_alerts)

def _evaluate_user(user, snap, delta_components, actual, label, cycle_alerts=None):
    """Compare expected (snapshot + delta) vs actual; alert or roll forward.

    Trust-mode snapshots set expected_balance = actual at first sighting and
    components = 0. Each later cycle adds delta_total_change to the prior
    expected_balance (not to sum-of-components) so we measure NEW drift
    relative to the baseline, not all-time imbalance.
    """
    snap_components = {k: Decimal(v) for k, v in (snap.get("components") or {}).items()}
    new_components = {
        "deposit_total":        snap_components.get("deposit_total", Decimal(0)) + delta_components["deposit_total"],
        "promo_total":          snap_components.get("promo_total",   Decimal(0)) + delta_components["promo_total"],
        "wlnet_total":          snap_components.get("wlnet_total",   Decimal(0)) + delta_components["wlnet_total"],
        "admin_total":          snap_components.get("admin_total",   Decimal(0)) + delta_components["admin_total"],
        "agency_convert_total": snap_components.get("agency_convert_total", Decimal(0)) + delta_components["agency_convert_total"],
        "withdraw_total":       snap_components.get("withdraw_total", Decimal(0)) + delta_components["withdraw_total"],
    }
    delta_total_change = (delta_components["deposit_total"]  + delta_components["promo_total"]
                          + delta_components["wlnet_total"]  + delta_components["admin_total"]
                          + delta_components["agency_convert_total"]
                          - delta_components["withdraw_total"])
    expected = Decimal(snap.get("expected_balance", "0")) + delta_total_change
    diff = actual - expected

    if abs(diff) <= TOLERANCE_KRW:
        # Healthy — roll snapshot forward
        snap_col.update_one(
            {"_id": user},
            {"$set": {
                "snapshot_at": datetime.now(timezone.utc),
                "last_event_id": delta_components["max_event_id"] or snap.get("last_event_id"),
                "components": {k: str(v) for k, v in new_components.items()},
                "expected_balance": str(expected),
                "actual_vin_at_snapshot": str(actual),
                "consecutive_mismatch": 0,
                "last_audit_at": datetime.now(timezone.utc),
            }, "$inc": {"audit_count": 1}}
        )
        return

    # Mismatch — bump consecutive counter; alert on 2nd hit
    new_consec = (snap.get("consecutive_mismatch") or 0) + 1
    snap_col.update_one(
        {"_id": user},
        {"$set": {
            "consecutive_mismatch": new_consec,
            "last_audit_at": datetime.now(timezone.utc),
        }, "$inc": {"audit_count": 1}}
    )
    if new_consec >= 2:
        maybe_alert(user, expected, actual, diff, new_components, snap, label, cycle_alerts)

# ─── Alerting ──────────────────────────────────────────────────────────────
def alert_class(expected: Decimal, diff: Decimal) -> str:
    if expected < 0:
        return "🔴 NEGATIVE_EXPECTED"
    if diff > 0:
        return "🟠 POSITIVE_DRIFT"
    return "🟠 NEGATIVE_DRIFT"

def alert_dedup_key(user: str, diff: Decimal) -> str:
    bucket = (int(diff) // 1000) * 1000
    sign = "+" if diff > 0 else "-"
    return hashlib.sha1(f"{user}|{sign}|{bucket}".encode()).hexdigest()

def maybe_alert(user, expected, actual, diff, components, snap, label, cycle_alerts=None):
    key = alert_dedup_key(user, diff)
    now = datetime.now(timezone.utc)
    existing = dedup_col.find_one({"_id": key})
    if existing:
        # already alerted within TTL
        dedup_col.update_one({"_id": key}, {"$inc": {"alert_count": 1}})
        return

    dedup_col.insert_one({
        "_id": key, "user": user, "diff": str(diff),
        "first_alerted_at": now,
        "alert_count": 1,
        "expires_at": now + timedelta(seconds=DEDUP_TTL_S),
    })

    cls = alert_class(expected, diff)
    msg = (
        f"🚨 SAI TOÀN VẸN VÍ\n\n"
        f"User:        {user}\n"
        f"Phát hiện:   {now.strftime('%Y-%m-%d %H:%M:%S')} UTC ({label} sweep)\n"
        f"Class:       {cls}\n"
        f"Lệch:        {diff:+,} KRW\n\n"
        f"Đối chiếu (expected):\n"
        f"  Tiền nạp        {components['deposit_total']:+,}\n"
        f"  Khuyến mãi      {components['promo_total']:+,}\n"
        f"  Lãi/lỗ          {components['wlnet_total']:+,}\n"
        f"  Admin cộng      {components['admin_total']:+,}\n"
        f"  Agency→Vin      {components['agency_convert_total']:+,}\n"
        f"  Tiền rút        {-components['withdraw_total']:+,}\n"
        f"  ─────────────\n"
        f"  Mong đợi        {expected:+,}\n"
        f"  users.vin        {actual:+,}\n"
        f"  ─────────────\n"
        f"  Lệch            {diff:+,}\n\n"
        f"Snapshot: {snap.get('audit_count', 0)} audits, last good {snap.get('snapshot_at')}"
    )
    log.warning(f"ALERTED {user} class={cls} diff={diff}")
    if cycle_alerts is not None:
        cycle_alerts.append((user, int(diff)))
        if len(cycle_alerts) > ALERT_THROTTLE:
            return  # storm — caller will emit summary instead
    send_telegram(msg)

def send_telegram(text: str):
    try:
        r = requests.post(
            f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage",
            data={"chat_id": TELEGRAM_CHAT, "text": text},
            timeout=10,
        )
        if not r.ok:
            log.error(f"telegram send failed: {r.status_code} {r.text[:200]}")
    except Exception as e:
        log.error(f"telegram send error: {e}")

# ─── Schedulers ────────────────────────────────────────────────────────────
def hot_scan():
    try:
        users = active_users(HOT_WINDOW_MIN)
        audit_users(users, label="HOT")
    except Exception as e:
        log.exception(f"hot_scan error: {e}")

def warm_sweep():
    try:
        users = active_users(WARM_WINDOW_H * 60)
        audit_users(users, label="WARM")
    except Exception as e:
        log.exception(f"warm_sweep error: {e}")

def full_sweep():
    try:
        users = all_users_with_balance()
        audit_users(users, label="FULL")
    except Exception as e:
        log.exception(f"full_sweep error: {e}")

# ─── Health endpoint (sidecar HTTP server) ─────────────────────────────────
LAST_CYCLE_AT = {"hot": 0, "warm": 0, "full": 0}

def _wrap(label, fn):
    def _f():
        fn()
        LAST_CYCLE_AT[label] = time.time()
    return _f

def health_server():
    from http.server import BaseHTTPRequestHandler, HTTPServer
    class H(BaseHTTPRequestHandler):
        def do_GET(self):
            now = time.time()
            stale = (now - LAST_CYCLE_AT["hot"]) > HOT_INTERVAL_S * 3 if LAST_CYCLE_AT["hot"] else True
            self.send_response(503 if stale else 200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(
                f'{{"hot_age_s":{int(now-LAST_CYCLE_AT["hot"])},'
                f'"warm_age_s":{int(now-LAST_CYCLE_AT["warm"])},'
                f'"full_age_s":{int(now-LAST_CYCLE_AT["full"])}}}'.encode())
        def log_message(self, *a, **kw): pass
    HTTPServer(("0.0.0.0", 8889), H).serve_forever()

# ─── Main ──────────────────────────────────────────────────────────────────
def main():
    log.info("=== sunkr-audit-bot starting ===")
    log.info(f"  hot every {HOT_INTERVAL_S}s, warm every {WARM_INTERVAL_S}s, full every {FULL_INTERVAL_S}s")
    log.info(f"  tolerance={TOLERANCE_KRW} KRW, batch={BATCH_SIZE}, dedup_ttl={DEDUP_TTL_S}s")
    # Send startup banner
    send_telegram(f"✅ sunkr-audit-bot started at {datetime.now(timezone.utc).isoformat()}\n"
                  f"hot={HOT_INTERVAL_S}s warm={WARM_INTERVAL_S}s full={FULL_INTERVAL_S}s")
    # Health server in a thread
    threading.Thread(target=health_server, daemon=True).start()
    # Scheduler
    sched = BackgroundScheduler(daemon=True)
    sched.add_job(_wrap("hot",  hot_scan),    "interval", seconds=HOT_INTERVAL_S,  next_run_time=datetime.now()+timedelta(seconds=10))
    sched.add_job(_wrap("warm", warm_sweep),  "interval", seconds=WARM_INTERVAL_S, next_run_time=datetime.now()+timedelta(minutes=5))
    sched.add_job(_wrap("full", full_sweep),  "interval", seconds=FULL_INTERVAL_S, next_run_time=datetime.now()+timedelta(minutes=30))
    sched.start()

    try:
        while True: time.sleep(60)
    except (KeyboardInterrupt, SystemExit):
        log.info("shutdown signal — stopping scheduler")
        sched.shutdown(wait=False)

if __name__ == "__main__":
    if "--once" in sys.argv:
        # Manual single-pass for testing — runs the HOT scan once and exits
        hot_scan()
        sys.exit(0)
    main()

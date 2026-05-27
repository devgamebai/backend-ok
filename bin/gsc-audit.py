#!/usr/bin/env python3
"""SUN-1120 — GSC wagers audit tool.

Walks GSC `/api/operators/wagers` in 5-minute windows for a date range
and dumps every wager that matches the (optional) member / game_code /
product_code / status filters. Optionally cross-checks against our
local Mongo `log_gsc_bets` collection to flag drift.

This is intentionally a one-shot CLI tool — no scheduling, no alerts,
no writes to production state. Ops runs it on demand when a complaint
arrives.

Usage:
  ./gsc-audit.py --start "2026-04-25 17:00" --end "2026-04-25 17:30"
  ./gsc-audit.py --start "..." --end "..." --member khongduong
  ./gsc-audit.py --start "..." --end "..." --game-code SuperSicBo000001
  ./gsc-audit.py --start "..." --end "..." --member khongduong --compare-mongo

Times are interpreted as UTC.

Reads operator credentials from the running thirdparty container's env.
"""
import argparse
import datetime as dt
import hashlib
import json
import subprocess
import sys
import time
import urllib.parse
import urllib.request


def read_gsc_creds():
    """Pull GSC creds from the running game-thirdparty container."""
    out = subprocess.check_output(
        ["docker", "exec", "sunwinkr-game-thirdparty", "printenv"]
    ).decode("utf-8")
    env = dict(line.split("=", 1) for line in out.strip().splitlines() if "=" in line)
    return {
        "url": env["GSC_OPERATOR_URL"].rstrip("/"),
        "operator_code": env["GSC_OPERATOR_CODE"],
        "secret_key": env["GSC_SECRET_KEY"],
    }


def gsc_sign(action, request_time, secret_key, operator_code):
    """md5(request_time + secret_key + action + operator_code)."""
    raw = f"{request_time}{secret_key}{action}{operator_code}"
    return hashlib.md5(raw.encode("utf-8")).hexdigest()


def fetch_wagers(creds, start_ms, end_ms, offset=0, size=5000):
    """Call GSC /api/operators/wagers for one 5-min-or-less window."""
    rt = int(time.time())
    sign = gsc_sign("getwagers", rt, creds["secret_key"], creds["operator_code"])
    qs = urllib.parse.urlencode({
        "operator_code": creds["operator_code"],
        "sign": sign,
        "request_time": rt,
        "start": start_ms,
        "end": end_ms,
        "offset": offset,
        "size": size,
    })
    url = f"{creds['url']}/api/operators/wagers?{qs}"
    req = urllib.request.Request(url, headers={"User-Agent": "sunwinkr-gsc-audit/1.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        body = resp.read().decode("utf-8")
    data = json.loads(body)
    return data.get("wagers", []), data.get("pagination", {})


def walk_windows(creds, start_dt, end_dt, window_min=5, sleep_ms=100):
    """Yield wagers across the date range, one 5-min window at a time."""
    win_seconds = window_min * 60
    cur = start_dt
    total_calls = 0
    while cur < end_dt:
        next_ = min(cur + dt.timedelta(seconds=win_seconds), end_dt)
        s_ms = int(cur.timestamp() * 1000)
        e_ms = int(next_.timestamp() * 1000)
        offset = 0
        while True:
            wagers, pag = fetch_wagers(creds, s_ms, e_ms, offset=offset, size=5000)
            total_calls += 1
            for w in wagers:
                yield w
            try:
                total = int(pag.get("total", 0))
            except (TypeError, ValueError):
                total = 0
            offset += len(wagers)
            if not wagers or offset >= total:
                break
        cur = next_
        if sleep_ms > 0:
            time.sleep(sleep_ms / 1000.0)
    print(f"# walked {total_calls} GSC API calls", file=sys.stderr)


def fetch_local_mongo(member=None, game_code=None, product_code=None, start_dt=None, end_dt=None):
    """Pull our local Mongo log_gsc_bets in same window for parity comparison.

    Returns list of dicts with the fields we care about.
    """
    # Build the Mongo find() filter as a JS literal directly (json.dumps
    # would quote operators like $regex / $gte and break Mongo's BSON parse).
    parts = []
    if member:
        parts.append(f'user_name:"{member}"')
    if game_code:
        parts.append(f'game_code:/{game_code}/i')
    if product_code is not None:
        parts.append(f'product_code:{int(product_code)}')
    if start_dt and end_dt:
        s = start_dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        e = end_dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        parts.append(f'create_time:{{$gte:new Date("{s}"),$lt:new Date("{e}")}}')
    js_filter = "{" + ",".join(parts) + "}"

    script = f'''
use("win123club");
db.log_gsc_bets.find({js_filter}).forEach(function(d) {{
  print(JSON.stringify({{
    user_name: d.user_name,
    game_code: d.game_code,
    product_code: d.product_code,
    bet_value: d.bet_value && d.bet_value.low !== undefined ? d.bet_value.low : d.bet_value,
    txn_id: d.txn_id,
    wager_code: d.wager_code,
    create_time: d.create_time
  }}));
}});
'''
    # Get creds from backend-api container
    out = subprocess.check_output(
        ["docker", "exec", "sunwinkr-backend-api", "sh", "-c",
         "cat /app/config/mongo.properties"], stderr=subprocess.DEVNULL
    ).decode("utf-8")
    cfg = dict(l.split("=", 1) for l in out.strip().splitlines() if "=" in l and not l.startswith("#"))
    user = cfg["username"].strip()
    pw = cfg["password"].strip()

    out = subprocess.check_output([
        "docker", "run", "--rm", "-i", "--network", "sunwinkr-database", "mongo:7",
        "mongosh", "--quiet", "--host", "sunwinkr-mongodb",
        "-u", user, "-p", pw, "--authenticationDatabase", "admin",
        "--eval", script
    ], stderr=subprocess.DEVNULL).decode("utf-8")
    rows = []
    for line in out.strip().splitlines():
        line = line.strip()
        if not line or not line.startswith("{"):
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            pass
    return rows


def main():
    ap = argparse.ArgumentParser(description="GSC wagers audit (SUN-1120)")
    ap.add_argument("--start", required=True, help="UTC start time, e.g. '2026-04-25 17:00'")
    ap.add_argument("--end", required=True, help="UTC end time, e.g. '2026-04-25 17:30'")
    ap.add_argument("--member", help="filter by member_account (client-side)")
    ap.add_argument("--game-code", help="filter by game_code (client-side, substring)")
    ap.add_argument("--product-code", type=int, help="filter by provider_product_oid (client-side)")
    ap.add_argument("--status", help="filter by status (BET / SETTLED / CANCELLED / ROLLBACK)")
    ap.add_argument("--compare-mongo", action="store_true",
                    help="cross-check against local Mongo log_gsc_bets, print drift")
    ap.add_argument("--window-min", type=int, default=5, help="minutes per GSC call (max 5)")
    args = ap.parse_args()

    start_dt = dt.datetime.strptime(args.start, "%Y-%m-%d %H:%M").replace(tzinfo=dt.timezone.utc)
    end_dt = dt.datetime.strptime(args.end, "%Y-%m-%d %H:%M").replace(tzinfo=dt.timezone.utc)

    creds = read_gsc_creds()
    print(f"# GSC operator: {creds['operator_code']} @ {creds['url']}", file=sys.stderr)
    print(f"# window: {start_dt.isoformat()} → {end_dt.isoformat()} (UTC)", file=sys.stderr)

    gsc_wagers = []
    for w in walk_windows(creds, start_dt, end_dt, window_min=args.window_min):
        if args.member and w.get("member_account") != args.member:
            continue
        if args.game_code and args.game_code.lower() not in (w.get("game_code") or "").lower():
            continue
        if args.product_code is not None and w.get("provider_product_oid") != args.product_code:
            continue
        if args.status and w.get("status") != args.status:
            continue
        gsc_wagers.append(w)

    print(f"# GSC matched {len(gsc_wagers)} wagers", file=sys.stderr)
    print()
    print("=" * 100)
    print("GSC AUTHORITATIVE RECORD")
    print("=" * 100)
    for w in gsc_wagers:
        ct = dt.datetime.fromtimestamp(w["created_at"] / 1000, tz=dt.timezone.utc).strftime("%H:%M:%S")
        st = dt.datetime.fromtimestamp(w["settled_at"] / 1000, tz=dt.timezone.utc).strftime("%H:%M:%S") if w.get("settled_at") else "-"
        print(f"  {ct} settled={st}  member={w['member_account']:15} game={w.get('game_code','?'):28} bet={w.get('bet_amount',0):>8}  status={w.get('status','?'):8}  wager={w.get('code','')[:24]}")

    if not args.compare_mongo:
        return

    print()
    print("=" * 100)
    print("LOCAL Mongo log_gsc_bets")
    print("=" * 100)
    mongo_rows = fetch_local_mongo(
        member=args.member,
        game_code=args.game_code,
        product_code=args.product_code,
        start_dt=start_dt, end_dt=end_dt,
    )
    print(f"# Mongo matched {len(mongo_rows)} rows", file=sys.stderr)

    gsc_codes = {w["code"] for w in gsc_wagers}
    mongo_codes = {r.get("wager_code") for r in mongo_rows if r.get("wager_code")}

    only_in_gsc = gsc_codes - mongo_codes
    only_in_mongo = mongo_codes - gsc_codes

    print()
    print("=" * 100)
    print("DRIFT")
    print("=" * 100)
    print(f"  in GSC but missing from our Mongo: {len(only_in_gsc)}")
    for w in gsc_wagers:
        if w["code"] in only_in_gsc:
            ct = dt.datetime.fromtimestamp(w["created_at"] / 1000, tz=dt.timezone.utc).strftime("%H:%M:%S")
            print(f"    ✗ {ct}  {w['member_account']:15}  bet={w.get('bet_amount',0):>8}  game={w.get('game_code','?')}  wager={w['code']}")
    print(f"  in our Mongo but missing from GSC: {len(only_in_mongo)}  (typically zero)")
    for r in mongo_rows:
        if r.get("wager_code") in only_in_mongo:
            print(f"    ?  user={r.get('user_name')}  bet={r.get('bet_value')}  wager={r.get('wager_code')}")


if __name__ == "__main__":
    main()

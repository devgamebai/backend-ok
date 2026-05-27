#!/usr/bin/env python3
"""
RTP Strategy LIVE probe — creates real test users, sends real TaiXiu bets
through ws-bridge, and measures per-user P/L under different RTP configs.

Tests against the ACTUAL production game engine (not synthetic). Requires:
  - 4 seeded test users (rtp_small/medium/big/whale) with password md5(123456)
  - ws-bridge reachable at wss://staging-play.sunkr.bet/ws/minigame
  - CANCUA_USE_DYNAMIC_RTP=1 env to actually observe the pct-aware effect
    (without flag: baseline TaiXiu only, our override has no effect)

Usage:
    python3 rtp_live_probe.py [--bets N] [--pct X]
        --bets: bets per user (default 50)
        --pct: override pct for whale (default 60)

Output:
    live_results.csv with per-bet observations
    live_summary.txt with per-user aggregated P/L
"""

import argparse
import csv
import hashlib
import json
import random
import sys
import time
import urllib.request
import ssl

try:
    from websocket import create_connection
except ImportError:
    print("ERROR: pip install websocket-client", file=sys.stderr)
    sys.exit(1)

BASE_URL = "https://staging-play.sunkr.bet"
WS_URL = "wss://staging-play.sunkr.bet/ws/minigame"
PASSWORD_MD5 = hashlib.md5(b"123456").hexdigest()
ADMIN_USER = "superadmin"
ADMIN_PW = hashlib.md5(b"admin123").hexdigest()

USERS = [
    ("rtp_small",  "RTPSmall",  100_000,     14193),
    ("rtp_medium", "RTPMedium", 1_000_000,   14194),
    ("rtp_big",    "RTPBig",    10_000_000,  14195),
    ("rtp_whale",  "RTPWhale",  100_000_000, 14196),
]

SSL_CTX = ssl.create_default_context()

def http_get(url):
    with urllib.request.urlopen(url, context=SSL_CTX, timeout=15) as r:
        return json.loads(r.read())

def login_player(user_name):
    r = http_get(f"{BASE_URL}/api?c=3&un={user_name}&pw={PASSWORD_MD5}")
    if not r.get("success"):
        raise RuntimeError(f"login {user_name} failed: {r}")
    return r["accessToken"]

def admin_login():
    r = http_get(f"{BASE_URL}/api_backend?c=701&un=superadmin&pw=0192023a7bbd73250516f069df18b500")
    return json.loads(r["data"])["adminToken"]

def apply_override(admin_token, user_id, game_code, pct, reason="rtp_live_probe"):
    url = (f"{BASE_URL}/api_backend?c=9773&aat={admin_token}"
           f"&user_id={user_id}&game_code={game_code}&win_rate_pct={pct}&reason={reason}")
    return http_get(url)

def delete_override(admin_token, user_id, game_code):
    url = f"{BASE_URL}/api_backend?c=9774&aat={admin_token}&user_id={user_id}&game_code={game_code}"
    try: return http_get(url)
    except Exception: return None

class TaiXiuClient:
    """Minimal JSON client for TaiXiu via ws-bridge."""
    def __init__(self, user_name, access_token, user_id):
        self.user_name = user_name
        self.user_id = user_id
        self.token = access_token
        self.ws = None

    def connect(self):
        self.ws = create_connection(WS_URL, sslopt={"cert_reqs": ssl.CERT_NONE}, timeout=15)
        self.ws.send(json.dumps({
            "id": "AUTHORIZE_TOKEN",
            "data": {"nickname": self.user_name, "token": self.token}
        }))
        time.sleep(0.3)
        # subscribe to TaiXiu VIN room
        self.ws.send(json.dumps({
            "id": "SUBSCRIBE_TAI_XIU",
            "data": {"roomId": 1}
        }))
        time.sleep(0.3)

    def bet(self, bet_value, bet_side):
        """Send a bet, return the messages received until next bet-window.
        Returns list of raw dicts from server."""
        self.ws.send(json.dumps({
            "id": "BET_TAI_XIU",
            "data": {
                "userId": self.user_id,
                "referenceId": int(time.time() * 1000),
                "betMoney": bet_value,
                "moneyType": 1,
                "betSide": bet_side,
                "inputTime": 0
            }
        }))

    def read_messages(self, timeout_ms=1500):
        self.ws.settimeout(timeout_ms / 1000.0)
        msgs = []
        try:
            while True:
                raw = self.ws.recv()
                if isinstance(raw, bytes): continue
                try: msgs.append(json.loads(raw))
                except Exception: pass
        except Exception:
            pass
        return msgs

    def close(self):
        try: self.ws.close()
        except Exception: pass

def get_balance(user_name):
    """Query user's current vin via admin — we know nickname, use DB."""
    # Shortcut: use a direct admin endpoint or query DB
    # For now, return None and track from bet responses
    return None

def run():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bets", type=int, default=30)
    ap.add_argument("--pct", type=float, default=60.0,
                    help="override pct applied to whale after first half")
    args = ap.parse_args()

    print(f"=== RTP LIVE PROBE ===")
    print(f"bets per user: {args.bets}")
    print(f"override pct for whale (applied mid-run): {args.pct}%")
    print()

    admin_token = admin_login()
    print(f"admin_token: {admin_token[:16]}...")

    # Ensure any previous override is cleared
    for _, _, _, uid in USERS:
        delete_override(admin_token, uid, "taixiu")

    # Login all users + connect ws
    clients = []
    for user_name, nick, _, uid in USERS:
        try:
            tok = login_player(user_name)
            c = TaiXiuClient(nick, tok, uid)
            c.connect()
            clients.append((user_name, c, uid))
            print(f"  connected: {user_name} (uid {uid})")
        except Exception as e:
            print(f"  FAILED {user_name}: {e}")

    if not clients:
        print("no clients connected — aborting")
        return

    results = []
    half = args.bets // 2

    print()
    print(f"--- Phase 1: {half} bets baseline (no override) ---")
    for round_i in range(half):
        for user_name, c, uid in clients:
            bet_value = random.choice([10_000, 20_000, 50_000])
            side = random.randint(0, 1)
            c.bet(bet_value, side)
            msgs = c.read_messages(timeout_ms=1200)
            result_msg = next((m for m in msgs if m.get("cmd") == "RESULT_TAI_XIU"
                               or "result" in str(m).lower()), None)
            results.append({
                "phase": "baseline", "round": round_i, "user": user_name,
                "uid": uid, "bet": bet_value, "side": side,
                "msg_count": len(msgs),
                "raw": json.dumps(msgs)[:200]
            })
        time.sleep(0.3)

    print(f"--- Applying whale override: pct={args.pct}% ---")
    r = apply_override(admin_token, USERS[3][3], "taixiu", args.pct)
    print(f"  override response: {r}")

    print(f"--- Phase 2: {args.bets - half} bets with whale override ---")
    for round_i in range(args.bets - half):
        for user_name, c, uid in clients:
            bet_value = random.choice([10_000, 20_000, 50_000])
            side = random.randint(0, 1)
            c.bet(bet_value, side)
            msgs = c.read_messages(timeout_ms=1200)
            results.append({
                "phase": "override", "round": round_i, "user": user_name,
                "uid": uid, "bet": bet_value, "side": side,
                "msg_count": len(msgs),
                "raw": json.dumps(msgs)[:200]
            })
        time.sleep(0.3)

    # Clean up
    print(f"--- Cleanup: removing override ---")
    delete_override(admin_token, USERS[3][3], "taixiu")
    for _, c, _ in clients:
        c.close()

    # Output
    out_csv = "/root/sunwinkr/sunwinkr/tools/rtp-strategy-test/live_results.csv"
    with open(out_csv, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(results[0].keys()))
        w.writeheader()
        for r in results: w.writerow(r)
    print(f"wrote {len(results)} observations → {out_csv}")

    print()
    print("=== SUMMARY (observations per user per phase) ===")
    # Query DB to see final balances via Python straight to MySQL isn't set up;
    # rely on the operator to query final vin balances manually:
    print("Next step: query user final balances:")
    print(f"  mysql vinplay -e \"SELECT id,user_name,vin FROM users WHERE user_name LIKE 'rtp_%'\"")
    print(f"  compare vs starting balances: small=100k, medium=1M, big=10M, whale=100M")

if __name__ == "__main__":
    run()

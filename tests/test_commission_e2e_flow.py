#!/usr/bin/env python3
"""
E2E Commission Flow Test — simulates player bets and verifies full commission chain.

Since we can't easily play games via CLI (WebSocket binary auth), this test:
1. Inserts bet data directly into log_report_user (simulating what vbee consumer does)
2. Runs c=9758 to calculate commission (with per-game rates)
3. Verifies differential split + self-rebate + wallet routing
4. Runs c=9753 to trigger payout
5. Verifies wallet balances (SELF → vin, DOWNLINE → agency_wallet)

This tests everything except game-server → RMQ → vbee path (code fix verified separately).
"""

import json
import subprocess
import urllib.request
import sys

ADMIN_URL = "https://staging-admin.sunkr.bet/api_backend"

def mysql(sql):
    cmd = f"docker exec sunwinkr-mysql mysql -u root -p'-Lo1HgJvrWmb-gSb-cUZV9BGkrDgMa7R' -sN -e \"{sql}\""
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return r.stdout.strip()

def api(params):
    url = f"{ADMIN_URL}?{params}"
    with urllib.request.urlopen(url) as resp:
        return json.loads(resp.read())

def test(name, condition, detail=""):
    status = "✅ PASS" if condition else "❌ FAIL"
    print(f"  {status}: {name}" + (f" — {detail}" if detail else ""))
    return condition

PASS = 0
FAIL = 0

print("=" * 60)
print(" E2E Commission Flow Test (simulated bets)")
print("=" * 60)
print()

# ============================================================
# SETUP: Use bossman01 hierarchy
# TĐL bossman01 (2.0%) → ĐL1 dealer01 (1.5%) → player dealplay1
# ============================================================
print("=== SETUP ===")

BOSS_ID = mysql("SELECT id FROM vinplay_admin.useragent WHERE nickname='bossman01'")
DL1_ID = mysql("SELECT id FROM vinplay_admin.useragent WHERE nickname='dealer01'")
print(f"  TĐL: bossman01 (id={BOSS_ID})")
print(f"  ĐL1: dealer01 (id={DL1_ID})")

# Set per-game rates for dealer01
print("  Setting per-game rates for dealer01...")
r = api("c=9849&nn=dealer01&games=" + urllib.parse.quote(json.dumps([
    {"game_key": "taixiu", "rate": 1.0},
    {"game_key": "xocdia", "rate": 0.5},
    {"game_key": "slot_pokemon", "rate": 0.8}
])))
print(f"  Per-game rates: {r.get('data',{}).get('updated',0)} updated")

# Get player info
PLAYER = "dealplay1"
PLAYER_NICK = mysql(f"SELECT nick_name FROM vinplay.users WHERE user_name='{PLAYER}'")
if not PLAYER_NICK:
    PLAYER_NICK = PLAYER
    mysql(f"UPDATE vinplay.users SET nick_name='{PLAYER}' WHERE user_name='{PLAYER}'")
print(f"  Player: {PLAYER_NICK} (under dealer01)")

# Record balances BEFORE
VIN_BEFORE = int(mysql(f"SELECT COALESCE(vin,0) FROM vinplay.users WHERE nick_name='{PLAYER_NICK}'") or 0)
DL1_WALLET_BEFORE = int(mysql(f"SELECT COALESCE(balance,0) FROM vinplay.agency_wallet WHERE agent_id={DL1_ID}") or 0)
BOSS_WALLET_BEFORE = int(mysql(f"SELECT COALESCE(balance,0) FROM vinplay.agency_wallet WHERE agent_id={BOSS_ID}") or 0)
DL1_VIN_BEFORE = int(mysql(f"SELECT COALESCE(vin,0) FROM vinplay.users WHERE nick_name='dealer01'") or 0)
print(f"  dealer01 VIN before: {DL1_VIN_BEFORE}")
print(f"  dealer01 agency wallet before: {DL1_WALLET_BEFORE}")
print(f"  bossman01 agency wallet before: {BOSS_WALLET_BEFORE}")

print()

# ============================================================
# TEST 1: Simulate player bets via log_report_user
# ============================================================
print("=== TEST 1: Simulate bets in log_report_user ===")

TODAY = mysql("SELECT CURDATE()")
mysql(f"""
INSERT INTO vinplay.log_report_user (nick_name, time_report, taixiu, xocdia, slot_pokemon)
VALUES ('{PLAYER_NICK}', '{TODAY}', 100000, 50000, 200000)
ON DUPLICATE KEY UPDATE taixiu=taixiu+100000, xocdia=xocdia+50000, slot_pokemon=slot_pokemon+200000
""")

row = mysql(f"SELECT taixiu, xocdia, slot_pokemon FROM vinplay.log_report_user WHERE nick_name='{PLAYER_NICK}' AND time_report='{TODAY}'")
r1 = test("Bet data inserted", row != "", f"taixiu/xocdia/slot: {row}")
PASS += r1; FAIL += not r1

print()

# ============================================================
# TEST 2: Dry-run commission calc for player (DOWNLINE)
# ============================================================
print("=== TEST 2: Commission calc — player bets (downline) ===")

# Taixiu: dealer01 gets 1.0%, boss gets (2.0%-1.0%)=1.0%
print("  Taixiu 100K: dealer01=1.0%, bossman01=1.0% differential")
r = api(f"c=9758&nn={PLAYER_NICK}&volume=100000&game=taixiu&dry=1&ps={TODAY}&pe={TODAY}&pt=DAILY")
dists = r.get("distributions", [])
total = r.get("totalPaid", 0)
print(f"  Total: {total}, Distributions: {len(dists)}")
for d in dists:
    print(f"    Agent {d['agentId']}: {d.get('rebateType','?')} diff={d['diffPct']}% amt={d['amount']}")

r2 = test("All downline (no self-rebate for player)",
    all(d.get('rebateType') == 'DOWNLINE' for d in dists),
    f"types: {[d.get('rebateType') for d in dists]}")
PASS += r2; FAIL += not r2

print()

# ============================================================
# TEST 3: Commission calc — agent bets (self-rebate)
# ============================================================
print("=== TEST 3: Commission calc — agent bets (self-rebate) ===")

# Simulate dealer01 betting
mysql(f"""
INSERT INTO vinplay.log_report_user (nick_name, time_report, taixiu)
VALUES ('dealer01', '{TODAY}', 80000)
ON DUPLICATE KEY UPDATE taixiu=taixiu+80000
""")

r = api(f"c=9758&nn=dealer01&volume=80000&game=taixiu&dry=1&ps={TODAY}&pe={TODAY}&pt=DAILY")
dists = r.get("distributions", [])
print(f"  dealer01 bets 80K taixiu")
for d in dists:
    print(f"    Agent {d['agentId']}: {d.get('rebateType','?')} diff={d['diffPct']}% amt={d['amount']}")

has_self = any(d.get('rebateType') == 'SELF' for d in dists)
has_downline = any(d.get('rebateType') == 'DOWNLINE' for d in dists)
r3 = test("Has SELF rebate for dealer01", has_self)
r4 = test("Has DOWNLINE for bossman01", has_downline)
PASS += r3 + r4; FAIL += (not r3) + (not r4)

# Check amounts: dealer01 self=80K*1.0%=800, boss=80K*(2.0%-1.0%)=800
self_amt = next((d['amount'] for d in dists if d.get('rebateType') == 'SELF'), 0)
dl_amt = next((d['amount'] for d in dists if d.get('rebateType') == 'DOWNLINE'), 0)
r5 = test(f"Self amount = 800", self_amt == 800, f"got {self_amt}")
r6 = test(f"Downline amount = 800", dl_amt == 800, f"got {dl_amt}")
PASS += r5 + r6; FAIL += (not r5) + (not r6)

print()

# ============================================================
# TEST 4: Create real PENDING logs + payout
# ============================================================
print("=== TEST 4: Create PENDING logs + payout ===")

# Create non-dry logs for dealer01's self-bet
r = api(f"c=9758&nn=dealer01&volume=80000&game=taixiu&dry=0&ps={TODAY}&pe={TODAY}&pt=DAILY")
dists = r.get("distributions", [])
log_ids = [d.get("logId") for d in dists if d.get("logId")]
print(f"  Created {len(log_ids)} PENDING logs: {log_ids}")

# Check rebate_type in DB
for lid in log_ids:
    rt = mysql(f"SELECT rebate_type FROM vinplay.rebate_logs WHERE id={lid}")
    status = mysql(f"SELECT status FROM vinplay.rebate_logs WHERE id={lid}")
    print(f"    Log {lid}: rebate_type={rt}, status={status}")

r7 = test("Logs created", len(log_ids) >= 2, f"count={len(log_ids)}")
PASS += r7; FAIL += not r7

# Payout each log
for lid in log_ids:
    rt = mysql(f"SELECT rebate_type FROM vinplay.rebate_logs WHERE id={lid}")
    r = api(f"c=9753&log_id={lid}&admin_nickname=superadmin")
    ok = r.get("success", False)
    amt = r.get("payout_amount", 0)
    print(f"    Payout log {lid} ({rt}): success={ok} amount={amt}")

print()

# ============================================================
# TEST 5: Verify wallet balances
# ============================================================
print("=== TEST 5: Verify wallet balances ===")

DL1_VIN_AFTER = int(mysql(f"SELECT COALESCE(vin,0) FROM vinplay.users WHERE nick_name='dealer01'") or 0)
DL1_WALLET_AFTER = int(mysql(f"SELECT COALESCE(balance,0) FROM vinplay.agency_wallet WHERE agent_id={DL1_ID}") or 0)
BOSS_WALLET_AFTER = int(mysql(f"SELECT COALESCE(balance,0) FROM vinplay.agency_wallet WHERE agent_id={BOSS_ID}") or 0)

dl1_vin_diff = DL1_VIN_AFTER - DL1_VIN_BEFORE
dl1_wallet_diff = DL1_WALLET_AFTER - DL1_WALLET_BEFORE
boss_wallet_diff = BOSS_WALLET_AFTER - BOSS_WALLET_BEFORE

print(f"  dealer01 VIN: {DL1_VIN_BEFORE} → {DL1_VIN_AFTER} (diff={dl1_vin_diff})")
print(f"  dealer01 agency wallet: {DL1_WALLET_BEFORE} → {DL1_WALLET_AFTER} (diff={dl1_wallet_diff})")
print(f"  bossman01 agency wallet: {BOSS_WALLET_BEFORE} → {BOSS_WALLET_AFTER} (diff={boss_wallet_diff})")

# Self-rebate (800) should go to VIN
r8 = test("SELF rebate → dealer01 VIN +800", dl1_vin_diff == 800, f"diff={dl1_vin_diff}")
# Downline commission (800) should go to bossman01 agency wallet
r9 = test("DOWNLINE → bossman01 agency wallet +800", boss_wallet_diff == 800, f"diff={boss_wallet_diff}")
# dealer01 agency wallet should NOT get the self-rebate
r10 = test("dealer01 agency wallet unchanged", dl1_wallet_diff == 0, f"diff={dl1_wallet_diff}")

PASS += r8 + r9 + r10; FAIL += (not r8) + (not r9) + (not r10)

print()
print("=" * 60)
print(f"  Results: {PASS} pass, {FAIL} fail")
print("=" * 60)

sys.exit(1 if FAIL > 0 else 0)

#!/usr/bin/env python3
"""
Test real bet via WebSocket → verify log_report_user gets populated.
Uses ws-bridge JSON protocol: {"id":"CMD_NAME","data":{...}}
"""

import asyncio
import json
import subprocess
import urllib.request
import websockets

WS_URL = "wss://wmini.staging-play.sunkr.bet"
PLAY_URL = "https://staging-play.sunkr.bet/api"
TEST_USER = "zuestang"
TEST_PASS = "e3486545c690ee99b976888431dda037"

def mysql_query(sql):
    cmd = f"docker exec sunwinkr-mysql mysql -u root -p'-Lo1HgJvrWmb-gSb-cUZV9BGkrDgMa7R' vinplay -sN -e \"{sql}\""
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return r.stdout.strip()

def get_session_key():
    url = f"{PLAY_URL}?c=3&un={TEST_USER}&pw={TEST_PASS}"
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read())
        return data.get("sessionKey", "")

async def test_bet():
    print("=== Real Bet Test ===\n")

    # Step 1: Login
    print("1. Login...")
    session_key = get_session_key()
    if not session_key:
        print("   FAIL: no sessionKey"); return
    print(f"   OK: sessionKey={session_key[:30]}...")

    nick = mysql_query(f"SELECT nick_name FROM users WHERE user_name='{TEST_USER}'")
    user_id = int(mysql_query(f"SELECT id FROM users WHERE user_name='{TEST_USER}'"))
    print(f"   Player: {nick} (id={user_id})")
    before = mysql_query(f"SELECT COUNT(*) FROM log_report_user WHERE nick_name='{nick}'")
    print(f"   log_report_user rows BEFORE: {before}\n")

    # Step 2: Connect & bet
    print("2. Connecting to WebSocket (Tai Xiu via ws-bridge)...")
    try:
        async with websockets.connect(WS_URL, additional_headers={"Origin": "https://staging-play.sunkr.bet"}) as ws:
            # Auth — bridge format: {"id":"CMD_NAME","data":{...}}
            await ws.send(json.dumps({"id": "AUTHORIZE_TOKEN", "data": {"nickname": nick, "token": session_key}}))
            print("   Sent: AUTHORIZE_TOKEN")

            # Read responses
            for _ in range(3):
                try:
                    resp = await asyncio.wait_for(ws.recv(), timeout=3)
                    t = f"text: {resp[:150]}" if isinstance(resp, str) else f"binary: {len(resp)}B"
                    print(f"   Recv: {t}")
                except asyncio.TimeoutError:
                    break

            # Subscribe to Tai Xiu (gameId=2, roomId=1)
            await ws.send(json.dumps({"id": "SUBSCRIBE_TAI_XIU", "data": {"gameId": 2, "roomId": 1}}))
            print("   Sent: SUBSCRIBE_TAI_XIU")

            # Wait for game state
            for _ in range(5):
                try:
                    resp = await asyncio.wait_for(ws.recv(), timeout=3)
                    t = f"text: {resp[:150]}" if isinstance(resp, str) else f"binary: {len(resp)}B"
                    print(f"   Recv: {t}")
                except asyncio.TimeoutError:
                    break

            # Bet on Tai Xiu
            await ws.send(json.dumps({
                "id": "BET_TAI_XIU",
                "data": {
                    "userId": user_id,
                    "betMoney": 1000,
                    "moneyType": 1,
                    "betSide": 0,  # 0=Tai
                    "inputTime": 0
                }
            }))
            print("   Sent: BET_TAI_XIU (1000 VIN on Tai)")

            # Read bet responses + wait for round to end
            print("   Waiting for responses (up to 30s)...")
            for _ in range(15):
                try:
                    resp = await asyncio.wait_for(ws.recv(), timeout=3)
                    t = f"text: {resp[:150]}" if isinstance(resp, str) else f"binary: {len(resp)}B"
                    print(f"   Recv: {t}")
                except asyncio.TimeoutError:
                    continue

    except Exception as e:
        print(f"   WebSocket error: {e}")

    # Step 3: Wait for vbee processing
    print("\n3. Waiting 15s for vbee to process...")
    await asyncio.sleep(15)

    # Step 4: Check results
    print("\n4. Checking log_report_user AFTER...")
    after = mysql_query(f"SELECT COUNT(*) FROM log_report_user WHERE nick_name='{nick}'")
    print(f"   log_report_user rows AFTER: {after}")

    if int(after) > int(before):
        row = mysql_query(f"SELECT nick_name, time_report, taixiu, taixiu_win FROM log_report_user WHERE nick_name='{nick}' ORDER BY id DESC LIMIT 1")
        print(f"   Data: {row}")
        print("   ✅ PASS: Real bet recorded in log_report_user!")
    else:
        print("   ⚠️  No new data yet")
        logs = subprocess.run("docker logs sunwinkr-vbee --since 30s 2>&1 | grep -i 'LogSumReport\\|602\\|report_user' | tail -5",
                            shell=True, capture_output=True, text=True)
        print(f"   vbee: {logs.stdout or 'no activity'}")
        # Also check if any queue messages came through
        q = subprocess.run("docker exec sunwinkr-rabbitmq rabbitmqctl list_queues name messages 2>&1 | grep log_report",
                          shell=True, capture_output=True, text=True)
        print(f"   queue: {q.stdout or 'empty'}")

if __name__ == "__main__":
    asyncio.run(test_bet())

#!/usr/bin/env python3
"""Test TaiXiu bet with XU (moneyType=0)."""
import websocket, json, time, threading, struct, subprocess

WS_URL = "wss://wmini.staging-play.sunkr.bet"
NICKNAME = "Kwon_DL1"
TOKEN = "40cf4a5e2c9e068534a33155e16f036b"
bet_sent = False

def on_message(ws, message):
    global bet_sent
    if not isinstance(message, bytes) or len(message) < 3:
        return
    cmd = struct.unpack('>H', message[1:3])[0]
    if cmd == 0x083f and not bet_sent:
        time.sleep(3)
        bet = json.dumps({"id": "BET_TAI_XIU", "data": {
            "betValue": 10000, "betSide": 1,
            "moneyType": 0, "inputTime": 5  # moneyType=0 = XU
        }})
        print(f"[SEND] BET 10K XU on TAI")
        ws.send(bet)
        bet_sent = True
    elif cmd == 0x083e:
        code = message[3] if len(message) > 3 else -1
        print(f"[BET RESPONSE] code={code} {'OK' if code==0 else 'FAIL'} | {message.hex()}")
    elif cmd == 0x0843:
        print(f"[DICE] {message.hex()}")
        time.sleep(15)
        ws.close()

def on_open(ws):
    def run():
        ws.send(json.dumps({"id": "AUTHORIZE_TOKEN", "data": {"nickname": NICKNAME, "token": TOKEN}}))
        time.sleep(2)
        ws.send(json.dumps({"id": "SUBSCRIBE_TAI_XIU", "data": {"roomId": 0}}))  # roomId=0 for XU
        print("[OK] Subscribed TaiXiu XU room. Waiting for round...")
        time.sleep(100)
        ws.close()
    threading.Thread(target=run, daemon=True).start()

def on_close(ws, c, m):
    print("[CLOSED]")

if __name__ == "__main__":
    ws = websocket.WebSocketApp(WS_URL, on_open=on_open, on_message=on_message, on_close=on_close, on_error=lambda w,e: print(f"[ERR] {e}"))
    ws.run_forever(ping_interval=30, ping_timeout=10)

    print("\n=== Verify ===")
    r = subprocess.run(['docker', 'exec', 'sunwinkr-mongodb', 'mongosh', '--quiet',
        '-u', 'sunwinkr_admin', '-p', '1LElaF0hcv35gOGVXGKFF5r1PE_-_LsY',
        '--authenticationDatabase', 'admin', 'win123club',
        '--eval', "print(db.log_money_user_vin.countDocuments({nick_name:'Kwon_DL1'}) + ' vin, ' + db.log_money_user_xu.countDocuments({nick_name:'Kwon_DL1'}) + ' xu')"],
        capture_output=True, text=True)
    print(f"MongoDB: {r.stdout.strip()}")

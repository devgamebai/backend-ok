# Live Test Harness — Findings & Limitations

## What was attempted

Build a Python harness that creates real test users, sends actual bets through the live game servers, and measures per-user P/L under different RTP configs.

## What works

1. **Test user seeding** — 4 users created via direct SQL INSERT:
   - `rtp_small` (100k VIN) / `rtp_medium` (1M) / `rtp_big` (10M) / `rtp_whale` (100M)
   - Password: md5("123456") = `e10adc3949ba59abbe56e057f20f883e`
   - User IDs: 14193-14196
   - Login via `c=3` returns valid token

2. **Admin API plumbing** — `c=9773` successfully applies per-user override, `c=9774` removes, `c=9775` audit works (all verified in earlier Phase 1 work).

3. **WebSocket bridge connection** — Python client connects to `wss://staging-play.sunkr.bet/ws/minigame`, authorizes with `{"id":"AUTHORIZE_TOKEN",...}`, subscribes to TaiXiu room. Server streams binary state updates.

4. **JSON command format** — ws-bridge expects `{"id":"CMD_NAME","data":{...}}` (NOT `cmd`). Bridge translates to BitZero binary.

## What doesn't work (yet)

**Bets never reach the real game engine on staging.**

Root cause: **staging runs TaiXiuMD5 module, not regular TaiXiu**.

- ws-bridge `CMD_MAP` only has `BET_TAI_XIU` (cmd 2110, handler 2000 = TaiXiuModule)
- Live staging bets go to **TaiXiuMD5Module** (cmd IDs in 21000-21110 range)
- Subscribe goes to wrong room (gameId=2, roomId=1 — may be empty)
- My bets never trigger `user_core - Request updateMoney` log lines

Evidence from game-minigame debug log during test:
```
thikrauden          → TaiXiuMD5 bets (live players)
hieosikochoidi      → TaiXiuMD5 bets
chungcuty2017       → TaiXiuMD5 bets
rtp_small / 14193   → NOT present (my bets didn't land)
```

## What's needed to make this work end-to-end

**Option A: Add TaiXiuMD5 commands to ws-bridge**
- Find actual cmd IDs from `TaiXiuMD5Module.java` switch cases (currently obfuscated)
- Find `BetTaiXiuMD5Cmd` payload structure
- Add new JSON encoders to `bridge.js` CMD_MAP: `SUBSCRIBE_TAI_XIU_MD5`, `BET_TAI_XIU_MD5`, etc.
- Rebuild ws-bridge image

**Option B: Direct binary BitZero client**
- Implement binary packet builder in Python matching `TaiXiuMD5Module` bytecode
- Talk directly to game-minigame:1641 or via Cloudflare tunnel
- Handle connection framing, ping/pong, cmd routing

**Option C: Hit the MD5 module via known public protocol**
- Check `sunwinkr-client` Cocos Creator source for MD5 command constants
- Reverse-engineer from browser DevTools network tab during real play

**Option D: Use TaiXiu Sicbo room (MGRoomTaiXiuSicbo)**
- My Phase 4b code IS wired there (`tryPctAwareDice` + `sicbo` game_code)
- Has separate module + ws-bridge has `SUBSCRIBE_SICBO`/`GET_MINI_GAME_SICBO_STATE`
- Possibly simpler protocol surface

## Alternative: flag-based room-level observation

Instead of test users, use staging's own traffic:

1. Enable `CANCUA_USE_DYNAMIC_RTP=1` in game-minigame (requires container restart)
2. Capture baseline: `curl c=9780&window=D1` → note taixiu `achieved_rtp_pct`
3. Change game rate: `c=9771` taixiu from 80 → 60
4. Wait 30-60 min for staging traffic to accumulate
5. Capture again → compare RTP delta

This DOESN'T prove per-user targeting (my current TaiXiu code is room-level only). It proves the global pct mechanism affects live traffic.

## Simulator remains the primary strategy validator

See `rtp_strategy_test.py` + `README.md` in this dir. Pure math Monte Carlo with matching formulas. Validated strategy:
- whale_targeted mode: 3.7× house net vs passive
- active mode: 2.7× house net vs passive
- BauCua saturates at ~77% — don't tune below that
- Minimum 1000 bets before trusting per-user signal

## Test user cleanup

```bash
docker exec sunwinkr-mysql mysql -uroot -p"$PASSWORD" vinplay -e "
  UPDATE users SET status=9 WHERE user_name LIKE 'rtp_%';
  -- status=9 = ban/disable (doesn't delete, preserves user history if any)
"
```

Keep the users around for the next live-test iteration once ws-bridge is extended (Option A) or we use Sicbo (Option D).

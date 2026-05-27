# TaiXiu / Sicbo Game Rules Specification

Ground-truth spec for behavior-preserving engine extraction. Sourced from CFR-decompiled JARs in production; all line refs map to compiled JARs.

> Source-of-truth files:
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/TaiXiuModule.java`  (TXM)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/SicboModule.java`  (SBM)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/room/MGRoomTaiXiu.java`  (TXR)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/room/MGRoomSicbo.java`  (SBR)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/entities/MinigameConstant.java`  (MC)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/entities/PotSicbo.java`  (PS)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/entities/PotTaiXiu.java`  (PT)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/entities/BotMinigame.java`  (BM)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/utils/GenerationTaiXiu.java`  (GTX)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/utils/CanCuaRtpBalancer.java`  (CRB)
> - `backend-master/game/Minigame/src/main/java/game/modules/TaiXiu/TaiXiuUtil.java`  (TXU)
> - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/TaiXiuServiceImpl.java`  (DAL-TX)
> - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/TaiXiuSicboServiceImpl.java`  (DAL-SB)

## 1. Round lifecycle

### TaiXiu — round = 68s (TXM.gameLoop, 422-468)

gameLoop ticks @ 1Hz via `BitZeroServer.getTaskScheduler().scheduleAtFixedRate(gameLoopTask, 10, 1, SECONDS)` (TXM:156). `count` resets to 0 at new-round transition.

| count | Phase event | Server-side action |
|------:|-------------|--------------------|
| 0..44 | Betting open | `betTaiXiu` accepts bets; `updateTaiXiuPerSecond` broadcast |
| 45 | **Betting locked** | `roomTXVin.disableBetting()` + `roomTXXu.disableBetting()` — sets `enableBetting=false`, writes `allow_betting_<refId>=0` to Hazelcast |
| 48 | Refund step | `roomTXVin.calculateMoneyReturn()` — guarded by `balanceGate` flag (TXR:665), no-ops when balanceGate=true |
| 50 | Round finish flag | `roomTXVin.finish()` — clears `bettingRound`, removes Hazelcast keys `allow_betting_<refId>`, `force_result_<refId>` |
| 51 | **Dice generation + jackpot calc** | `generateTaiXiuDices()` → `roomTXVin.getResult(refId)`; `calculateJackpot()` |
| 55 | Prize settlement (async) | Schedules `CalculatingTaiXiuPrize` task at +1s → `roomTX.calculatePrize(refId)` |
| 60 | Bot reschedule | `ScheduleBotTask` executor task |
| 68 | **New round** | `balanceTX.startNewRound()`, `++referenceTaiXiuId`, broadcasts `StartNewGameTaiXiuMsg`, resets count=0 |

`MGRoomTaiXiu.getRemainTime()` (TXR:274-310) bettingTime=50s, resultTime=18s. Reveal phase begins at `startTime + 50_000ms`.

### Sicbo — round = 55s (SBM.gameLoop, 417-477)

| count | Phase event | Server-side action |
|------:|-------------|--------------------|
| 0..39 | Betting open | bets + per-second broadcast |
| **40** | **Betting locked** | `disableBetting()` both rooms (SBM:436-439); flips `bettingRound=false` (SBR:320) |
| 43 | Finish flag | `roomTXVin.finish()` — clears `bettingRound`, removes Hazelcast keys |
| 44 | **Dice generation** | `generateTaiXiuDices()` → `roomTXVin.getResult(refId)` |
| 48 | **Prize payout** | `roomTXVin.reward()` (sync, gameLoop thread) + `txService.updateAllTop()` |
| 53 | Bot reschedule | `ScheduleBotTask` executor task |
| **55** | **New round** | `balanceTX.startNewRound()`, `++referenceTaiXiuId`, broadcasts `StartNewGameSicboMsg`, count=0 |

`MGRoomSicbo.getRemainTime()` (SBR:341-392) bettingTime=40s, resultTime=8s.

### Authority

- **Server-authoritative** for every phase transition. Client receives `BroadcastTXTimeMsg`/`UpdateTaiXiuPerSecondMsg`/`UpdateSicboPerSecondMsg` per second; UI is read-only.
- **Bet acceptance gate:** `enableBetting` flag (TXR:406, SBR:312) — checked inline. Sicbo also pre-checks `bettingRound` in module-level `betTaiXiu` (SBM:412) and `MGRoomSicbo.betTaiXiu` (SBR:402).
- **Hazelcast `allow_betting_<refId>`:** advisory, NOT the gate. Source of truth = JVM `enableBetting` boolean.

### referenceId / roundId

- Type: `long`, monotonic per game, persisted to MySQL via `MiniGameService.saveReferenceId(refId, gameId)`.
- TaiXiu loads with gameId=2 (TXM:211); saves with gameId=2 (TXM:221).
- **AMBIGUOUS:** Sicbo loads with `Games.TAI_XIU_SICBO.getId() = 30` (SBM:227) but **saves with hard-coded 31** (SBM:237). Restart re-loads stale stored refId. Likely bug.
- Allocation: incremented at `startNewRoundTX()` BEFORE broadcasting `StartNewGame*Msg` (TXM:342, SBM:357).
- Shared across VIN/XU rooms.

## 2. Bet types — TaiXiu

### betSide encoding (BetTaiXiuCmd.java; MC:31-32)
- `0` = **XIU_SIDE** (Xỉu / Low)
- `1` = **TAI_SIDE** (Tài / High)
- Other values silently fall through to `potXiu` route (TXR:391, 465).

### Bet packet (BetTaiXiuCmd)
```
int userId; long referenceId; long betValue; short moneyType; short betSide; short inputTime
```

### Min / Max
- **MIN_BET_TAI_XIU_VALUE = 100** (MC:30). Enforced TXR:407 (`betValue >= 100L` else error code 4).
- No code-level max. Capped only by user wallet (TXR:408 → error 3).

### Money types
- `moneyType == 1` → VIN room ("vin" wallet)
- `moneyType == 0` → XU room ("xu" wallet)
- Independent pots/funds/jackpots, shared `referenceId`. Dice generated on VIN room, copied to XU via `updateResultDices` (TXM:480).

### Cross-side restriction (TXR:413-414)
User **cannot bet on both Tài and Xỉu in same round** → error code 5.

### Jackpot side bet
- Not separate type. Any winning Tài/Xỉu bet eligible.
- Triggered when `TaiXiuServiceImpl.checkJackpotTaiXiu()` (DAL-TX:577) consumes Hazelcast `jackpottaixiu` (admin-set short = 1=Xỉu, 6=Tài).
- Forced dice = `{checkJackpot, checkJackpot, checkJackpot}` triple (TXR:604).
- Gate (TXR:594-601): `potT.numBet % 5 == 0` (when jp=6/Tài) or `potX.numBet % 5 == 0` (when jp=1/Xỉu). Else suppressed.
- Payout: `tienDuocTinh * jackpotAccumulate / denominator` (TXR:778, 798).
- Pool accumulates +0.6% of losing-pot value per round (TXR:558). Floor 50M VIN (TXR:166-168, 184-186).

### Error codes in BetTaiXiuMsg.Error
`0`=OK, `1`=wallet failure or betting disabled mid-call, `2`=betting closed, `3`=insufficient balance, `4`=below MIN, `5`=cross-side bet.

## 3. Bet types — Sicbo

### Wire format
`BetSicboCmd.betSide` = **String** (PotSicbo enum name); converted via `PotSicbo.getEnumByName(name).getId()` (SBR:406, 411).

### PotSicbo enum (PS:7-58) — full 52-entry table

| id | name | rotation (payout multiplier) |
|--:|------|--:|
| 1 | POINT_4 | 61 |
| 2 | POINT_5 | 31 |
| 3 | POINT_6 | 18 |
| 4 | POINT_7 | 13 |
| 5 | POINT_8 | 9 |
| 6 | POINT_9 | 7 |
| 7 | POINT_10 | 7 |
| 8 | POINT_11 | 7 |
| 9 | POINT_12 | 7 |
| 10 | POINT_13 | 9 |
| 11 | POINT_14 | 13 |
| 12 | POINT_15 | 18 |
| 13 | POINT_16 | 31 |
| 14 | POINT_17 | 61 |
| 15 | ONE_DICE_1 | 1 (see special) |
| 16 | ONE_DICE_2 | 1 |
| 17 | ONE_DICE_3 | 1 |
| 18 | ONE_DICE_4 | 1 |
| 19 | ONE_DICE_5 | 1 |
| 20 | ONE_DICE_6 | 1 |
| 21..41 | DOUBLE_DICES_X_Y (21=1_1, 22=1_2, …, 41=6_6) | 6 |
| 42..47 | TRIPLE_DICES_{1..6} | 31 |
| 48 | TAI | 2 |
| 49 | XIU | 2 |
| 50 | CHAN (Even) | 2 |
| 51 | LE (Odd) | 2 |
| 52 | ANY_TRIPLE_DICES | 31 |

### Special payout — ONE_DICE_*
SBR:1153-1163, 1207:
- 1 occurrence → `bet * 2L`
- 2 occurrences → `bet * 3L`
- 3 occurrences → `bet * 4L`

### Payout formula (other bet types)
```
prize = betValue * rotation
fee   = (tax * prize) / (200.0 - tax)        // sicbo tax = MINIGAME_TAX_TX = 5.0%
```

### Min / Max
- MIN=100 (SBR:545). No code-level max.

### Restrictions
- **AMBIGUOUS:** Sicbo cross-side guard (SBR:551) checks `betSide==0 || betSide==1` which never matches PotSicbo IDs (start at 1=POINT_4). Dead code. Per-user multi-side is effectively allowed.
- Triple suppresses POINT/CHAN/LE/DOUBLE (SBR:1090-1144).
- Total 3 (1+1+1) and 18 (6+6+6) bypass POINT scoring (SBR:1130-1137).

## 4. Result / dice generation

### TaiXiu (TXR:550-621)
1. **Force-result check** — `TaiXiuServiceImpl.suaKetQuaTaiXiu()` (DAL-TX:503) atomic read+remove on Hazelcast `IMap("ketquataixiu").get("ketquataixiu")` → if present, used (single-use).
2. Else: `GenerationTaiXiu.generateResultWithHouseEdge(GAME_ID_TAIXIU, realPotTai, realPotXiu, tax)` (TXR:587).
3. **Jackpot override** — `api.checkJackpotTaiXiu()` (DAL-TX:577) atomic read+remove on Hazelcast `jackpottaixiu`. If 1 or 6 AND %5 gate passed → overrides dice to `{n,n,n}`.
4. **Mapping:** `total > 10 ? TAI : XIU` (TXM:479). total=11=Tài, total=10=Xỉu.
5. Natural triples 3 and 18 NOT excluded from main result; jackpot triggered only by Hazelcast flag.

### Sicbo (SBR:698-730)
1. Force-result via `api.suaKetQuaTaiXiu()`. **AMBIGUOUS:** SBM:296 writes to `ketquataixiusicbo`, but `suaKetQuaTaiXiu()` method name suggests it reads `ketquataixiu` — verify DAL-SB impl body.
2. Else: `generateResultWithHouseEdge()` (SBR:732-776) — bespoke 216-combo search (see RTP balancer below).
3. **Fallback fund protection** (SBR:707-721): if RTP target chosen dice leaves `fundTaiXiu < 0`, re-roll random until `fundTaiXiu + tienloi > 0` — **infinite-loop risk** if fund deeply negative AND bets cover every outcome.
4. Tai/Xiu mapping: `total > 10 ? 1 : 0` (SBM:488).
5. Triples → `ANY_TRIPLE_DICES` + `TRIPLE_DICES_n` only; POINT/CHAN/LE/DOUBLE suppressed (SBR:1102-1144).

### RTP balancer (Sicbo) — SBR.generateResultWithHouseEdge:732-776

```
if (!CanCuaRtpBalancer.isEnabled())         // env CANCUA_USE_DYNAMIC_RTP=1
    return TaiXiuUtil.genarateRandomResult();

winRatePct      = RtpResolver.effectivePct(0L, "sicbo")
targetEdgePct   = (winRatePct >= 92 && effectivePct("sicbo") >= 92) ? 0 : 100 - winRatePct

if (targetEdgePct <= 0 || totalValueBetUser <= 0)   → random
if (totalValueBetUser < 100_000)                    → random   (tiny-pot guard)

targetProfit = totalValueBetUser * (targetEdgePct / 100.0)

// Brute-force all 6*6*6 = 216 ordered triples
for d1 in 1..6, d2 in 1..6, d3 in 1..6:
    totalPayout = sotienphaitra(d1,d2,d3)         // walks listUserBet, applies rotation/special
    profit      = totalValueBetUser - totalPayout
    diff        = |profit - targetProfit|
    track bestCombinations[] for minDiff (ties accumulate)

pick uniformly random from bestCombinations[]
```

- Config: `com.vinplay.vbee.common.rtp.RtpResolver.effectivePct(userId, gameId)`. Default fallback 92% → ignored.
- TaiXiu equivalent: `GenerationTaiXiu.generateResultWithHouseEdge` (GTX:93-166). 2-scenario (force Tài or Xỉu) — pick closest to target. Min imbalance ratio 5% (GTX:54).

### Random source
- TaiXiu: `ThreadLocalRandom` (GTX:177).
- Sicbo direct: `ThreadLocalRandom` (SBR:772).
- `TaiXiuUtil.genarateRandomResult`: uses `GameUtil.randomBetween`.
- Force-result writer: `ThreadLocalRandom` (TXM:270, SBM:287).
- **No SecureRandom anywhere. Not seeded. Non-reproducible.** Tests must mock RNG or `GenerationTaiXiu`/`TaiXiuUtil` static helpers.

### Force-result Hazelcast contract

| game | write path | map name | key | value | consumed by |
|------|------------|----------|-----|-------|-------------|
| TaiXiu | `TaiXiuModule.forceResultTaiXiu` cmd 2003 (TXM:258-286) — superadmin only | `ketquataixiu` | `"ketquataixiu"` | `short[3]` | `TaiXiuServiceImpl.suaKetQuaTaiXiu()` (DAL-TX:503); `map.remove(...)` single-use |
| Sicbo | `SicboModule.forceResultTaiXiu` cmd 28003 (SBM:275-303) — superadmin only | `ketquataixiusicbo` | `"ketquataixiusicbo"` | `short[3]` | AMBIGUOUS — verify DAL-SB |
| TaiXiu jackpot | external admin API | `jackpottaixiu` | `"jackpottaixiu"` | `short` (1=Xỉu, 6=Tài) | `checkJackpotTaiXiu()` (DAL-TX:577); single-use |

- No expiry. Persists until consumed.
- No refId binding. Admin **cannot** force-result a past round; override consumed at NEXT result-gen.

## 5. Prize calculation

### tongTienHopLe ("legal money" cross-pot balance)
`min(potTai.totalValue, potXiu.totalValue)` — matchable amount, both games.

Per-contributor accounting (TXR:559-567, 855-877):
```
for each TransactionTaiXiuDetail tran in pot.contributors:
    tienDuocTinh = tran.betValue
    if (running_pot_total + tran.betValue > tongTienHopLe)
        tienDuocTinh = tongTienHopLe - running_pot_total
    running_pot_total += tienDuocTinh
    tran.refund = tran.betValue - tienDuocTinh
```

Bets matched in insertion order; late bets on heavier side more likely to be partially refunded.

TaiXiu `balanceGate` config (TXR:861) bypasses to pay full bet (`tienDuocTinh = tran.betValue`). Default = `ConfigGame.getIntValue("balance_gate", 0) == 1` → default OFF → balancing IS applied.

### TaiXiu prize formula (TXR.calculatePrize:825-984)
For winning side:
```
prize  = tienDuocTinh * (100 - tax) / 100  +  tienDuocTinh
       = tienDuocTinh * (200 - tax) / 100              // stake + net winnings
refund = betValue - tienDuocTinh
```
`tax = MINIGAME_TAX_TX = 5.0f` (MC:13) for VIN+XU.

Losing side: `prize = 0`, `refund = bet - tienDuocTinh` still credited.

Jackpot allocation (TXR:778, 798):
```
tran.jpAmount = tienDuocTinh * jackpotAccumulate / denominator
denominator = sum(tienDuocTinh) over winning side
```

### Sicbo prize formula (SBR.reward:1170-1255)
```
prize = ONE_DICE_special ? bet * {2,3,4} : bet * rotation
fee   = tax * prize / (200 - tax)                     // VIN tax = MINIGAME_TAX_TX = 5.0%
```
No cross-pot balancing for Sicbo prizes. `calculateMoneyReturn` (SBR:808) exists but **dead code** — not called from gameLoop.

### Tax

| flag | source | when applied |
|------|--------|--------------|
| `MINIGAME_TAX_TX = 5.0f` | MC:13 | on prize at settlement |
| `MINIGAME_TAX_TX_JACKPOT = 1.0f` | MC:14 | declared, **NOT used in prize math reviewed** — likely consumed by RMQ consumer downstream |

### Refund
- Unmatched winning portion → `tran.refund` (TXR:889, 915, 937).
- Same `updateMoney` call but TransType=END_TRANS (TXR:1337).
- TaiXiu advance refund broadcast at count=48 (`TaiXiuRefundMsg`) — informational; money moves at count=55+ in `UpdateMoneyTXTask`. UI may briefly show refund before wallet credit.
- **AMBIGUOUS:** TXR:665 `if (this.balanceGate) return;` — semantics look inverted vs intent. Confirm.

### Settlement order (TaiXiu)
gameLoop ordering:
1. count=51: `getResult` → `calculateJackpot()` (in-memory `jpAmount`).
2. count=56: `calculatePrize` → builds sumTai/sumXiu maps → fires 2 `UpdateMoneyTXTask` threads in parallel. Per user:
   1. JP credit (if `totalJp > 0`) — IN_TRANS
   2. Main prize credit (`totalPrize`) — IN_TRANS if refund>0, else END_TRANS
   3. Refund credit (if `totalRefund > 0`) — END_TRANS

### Settlement order (Sicbo)
- Synchronous in gameLoop thread (SBM:463 → `roomTXVin.reward()`).
- Per bet (SBR:1204-1227): single `updateMoney(prize, fee, refId, END_TRANS)` credit. No jp/main/refund split.

### Multi-bet aggregation
- **TaiXiu** (TXR.updateSumTran:986-1009): same user, same side, multiple bets → sum `betValue + prize + refund + jp` into one `TransactionTaiXiu` row.
- **Sicbo** (SBR.updateSumTran:870-891): sums across all bet types (cross-side allowed).
- **Per-bet txId:** TaiXiu = `referenceId * 1_000_000 + (nanoTime & 0xFFFFFL)` (TXR:429). Sicbo = `referenceId * 1_000_000 + txnSequence.incrementAndGet()` (SBR:568). Required for `money_gateway_log` UK `(tx_id, source, user_id)` — SUN-1290.
- **Settle-time txId** (TXR:1295, 1321, 1337): single `referenceId` reused across jp/prize/refund credits. TransType disambiguates.

## 6. Wallet integration

### Class
- `com.vinplay.usercore.service.impl.UserServiceImpl.updateMoney(...)` — synchronous (TXR:432, 1301/1321/1337, SBR:570, 1210).
- Bots: Sicbo unconditional debit since 2026-05-08 (SBR:570). TaiXiu still has `if (!isBot || isLivestream)` gate at TXR:431. **AMBIGUOUS — inconsistent.**

### Signature
```
MoneyResponse updateMoney(
    String nickname,
    long amount,            // negative for debit, positive for credit
    String moneyType,       // "vin" | "xu"
    String source,          // "TaiXiu" | "TaiXiuHoanTien" | "SicBo" | "TaiXiuSicbo"
    String gameId,          // "2" or "30"
    String description,
    long fee,
    Long txId,              // per-bet unique
    TransType transType     // START_TRANS|IN_TRANS|END_TRANS
)
```

### Error path
- Insufficient balance checked BEFORE updateMoney at TXR:408 / SBR:546 — `result=3`, no wallet call.
- `MoneyResponse.isSuccess()=false` from backend → `result=1`. No row written.
- Mid-call disabled-betting (TXR:437): auto-refund via `TaiXiuHoanTien` source + END_TRANS.

### TransType (TransType.java)
- `START_TRANS (1)` = bet debit (opens logical txn)
- `IN_TRANS (2)` = intermediate credit during settle
- `END_TRANS (3)` = final credit closing txn
- `VIPPOINT (4)` / `NO_VIPPOINT (5)` = unused

### Retry
- **No application-level retry** for `updateMoney` failures. Returns `result=1`.
- Mongo writes wrapped in `MongoRetryHelper.run(..., "taixiu.clearUserBetToDb")` (TXR:370, SBR:483).
- Hazelcast `allow_betting_<refId>` write wrapped in try/catch (TXM:362-373) since SUN-1xxx 2026-05-11.

### RMQ persistence (`queue_taixiu`)

| message | type | source | destination table |
|---------|-----:|--------|--------------------|
| `TransactionTaiXiuMessage` | 100 | `saveTransactionTaiXiu()` DAL-TX:523 | `transaction_tai_xiu` (or `transaction_tai_xiu_sicbo`) |
| `ResultTaiXiuMessage` | 101 | `saveResultTaiXiu()` DAL-TX:544 | `result_tai_xiu` |
| `TransactionTaiXiuDetailMessage` | 102 | `saveTransactionTaiXiuDetail()` DAL-TX:573 | `transaction_tai_xiu_detail` |

### Mongo writes (direct from room)

| collection | written by | keys |
|------------|-----------|------|
| `user_bet_tai_xiu` | TXR:324 | referentId, nick_name, inputTime, betSide, betValue, balance, money_type |
| `user_bet_tai_xiu_sicbo` | SBR:435 | referentId, nick_name, inputTime, betSide, betValue, money_type |
| `user_jackpot_tai_xiu` | TXR:352 | referentId, result, time, countBet, moneyJackpotAll, data |
| `user_jackpot_tai_xiu_details` | TXR:338 | referentId, result, time, countBet, moneyJackpotAll, nickName, money |
| `user_jackpot_tai_xiu_sicbo` | SBR:462 | (same shape) |
| `user_jackpot_tai_xiu_sicbo_details` | SBR:448 | (same shape) |
| `jackpot_tx` | TXR.updateJpValue:1234 | `{ jackpotTX: <string-num> }` — singleton |

## 7. Bots

### Loading
- TaiXiu `scheduleBot` (TXM:376) → `BotMinigame.getBotTaiXiu("vin")` + `getVipBotTaiXiu()`. Lists merged into `botsVin`.
- Sicbo `scheduleBot` (SBM:377) → `BotMinigame.getBotSicbo("vin")` + `getVipBotSicbo()`.

### Bet placement (in-process)
- TaiXiu (TXM.botBet:390-412): `roomVin.betTaiXiu(nickname, 0, betValue, timeBetting, (short)1, (short)botBetSide, isBot=true)`. `userId=0` for bots.
- Sicbo (SBM.botBet:394-407): `roomVin.updateInfoBotBet(...)` + `roomVin.betTaiXiu(...)` with `isBot=true`. `BetSicboBotMsg` broadcast room-wide.

### Detection
- `isBot(username)` (TXR:1191, SBR:1053) — `UserCacheModel.isBot()` via `UserServiceImpl.getUser`. Null-safe since SUN-1xxx.
- `isUserBot(nickname)` (TXR:382, SBR:495) — Hazelcast `usersSetWin` boolean. Distinct: "livestream"/forcibly-routed bots.

### Wallet treatment
- **AMBIGUOUS / inconsistent:** Sicbo bots always debit real wallet (SBR:570). TaiXiu bots SKIP debit unless livestream (TXR:431).
- Bot balance top-up via `BotMinigame.pushMoneyToBot` (BM:139). Floor 10M VIN.

### Bet distribution

**TaiXiu (block-split, BM:292-406):**
- numBetTai/numBetXiu drawn independently in `[tx_min_bot_betting_vin, tx_max_bot_betting_vin]`.
- Collision → jitter `numBetXiu ± 1`.
- First `numBetTai` bots → `betSide=1`, rest → `betSide=0` (BM:386-393).
- Time-of-day modulation via `ratioTXInNight` (BM:659-681): 30-100% scaling.

**Sicbo (random-per-bet, BM:408-491):**
- numBetTai/numBetXiu drawn but **ignored** for betSide selection.
- Each bot's `betSide = random.nextInt(52) + 1` (BM:475) → uniform across all 52 PotSicbo IDs.

### Pot balancing
- TaiXiu (TXM.botBet:398-407): when remainTime ≤ 15, queries `getJackPotApi()`. If jackpot side set, bots SKIP betting opposite side when (`potT.numBet % 5 == 0` or `potX.numBet % 5 == 0`). Otherwise alternates `botBetSide = 1 - botBetSide`.
- Sicbo: no pot-balancing logic visible.

### Virtual-player pad (SUN-807)
- TXR.refreshPadIfNeeded:513, SBR.refreshPadIfNeeded:665. Each side draws independently in `[tx_fake_player_min=30, tx_fake_player_max=60]`. Cleared outside betting phase.

## 8. Edge cases / known quirks

| case | behavior |
|------|----------|
| Empty round (no bets) | Dice still generated; empty iteration in calculatePrize/reward; refId still increments |
| Only bots in round (TaiXiu) | isBot exclusion in `calculatePrize` skips totalTai/Xiu stats (TXR:956-959). Dice saved. Bots skip jackpot notification (TXR:813, 818) |
| Insufficient balance on debit | `result=3`, no rows, no pot mutation |
| Wallet failure on credit during settle | TaiXiu `UpdateMoneyTXTask` **swallows** in catch (TXR:1350-1352) — **money lost in transit. No retry. Bug risk.** |
| Engine crash mid-round | No recovery state. `referenceId` persisted at startNewRoundTX. On restart, loadData resumes. **In-flight bets in current round lost** (Mongo `user_bet_tai_xiu` cleared at next round's `clearUserBetToDb`) |
| Hazelcast force-result write race with random gen | Atomic via Hazelcast IMap. No race. |
| Admin force-result for past round | Not possible — no refId binding. Consumed on NEXT result-gen. |
| Multi-table | TaiXiu VIN+XU and Sicbo VIN+XU independent rooms. Within game, VIN+XU share refId; cross-game no coupling. |
| **Sicbo Mongo `clearUserBetToDb` race** | Drops at `startNewGame`. If `reward()` callback runs after `startNewGame` (clock-skew), data lost. Mitigated by 5s gap but not formally guaranteed. |
| **Sicbo `listUserBet` accumulation** | Reassigned `new ArrayList()` in startNewGame (SBR:220). SUN-EXPLOIT-GUARD at SBR:1179 references historical bug (quochuy98). Currently safe. |
| **Sicbo force-result map name mismatch** | SBM:296 writes `ketquataixiusicbo`; `suaKetQuaTaiXiu()` name suggests reads `ketquataixiu`. Verify DAL-SB. |
| **Sicbo refId persistence mismatch** | Loads gameId=30, saves gameId=31 (SBM:227 vs 237). JVM restart loses ref. |
| **TaiXiu `genarateResult` recursive no-op** | TXU:33 recursive call on triple total but discards return. Likely buggy. Active path uses `GenerationTaiXiu.generateDices`. |
| **Sicbo `tax = 0.0f` initial** | SBR:126 init 0.0f; set in ctor. If `reward()` runs before ctor completes (impossible normally) → tax=0. |
| **Sicbo cross-side guard broken** | SBR:551 checks `betSide==0 || betSide==1` — PotSicbo IDs never match. Dead code. |
| **Sicbo `fundTaiXiu` fallback infinite loop** | SBR:715 `while` can spin if random never produces positive `tienloi` against bet distribution. Edge: large negative fund + heavy bets on every outcome → server freeze. |

## 9. Invariants (JUnit assertion targets)

- **INV-1 (refId monotonicity):** `referenceId` strictly increasing per game. `roomTX.referenceId == module.referenceTaiXiuId` after `startNewRoundTX()`.
- **INV-2 (dice single-gen):** `getResult(refId)` called exactly once per round per game (VIN room generates, XU receives).
- **INV-3 (force-result single-use):** `IMap("ketquataixiu").remove("ketquataixiu")` atomic consume.
- **INV-4 (cross-side block, TaiXiu):** `pot_tai.getUsernameTotalBet(u) > 0 ⊕ pot_xiu.getUsernameTotalBet(u) > 0`.
- **INV-5 (tongTienHopLe matching):** `sum(tienDuocTinh)` over BOTH sides = `2 * min(pot_tai_total, pot_xiu_total)`.
- **INV-6 (refund completeness):** every TaiXiu contributor → `tran.refund + tienDuocTinh == tran.betValue`.
- **INV-7 (prize formula, TaiXiu):** winning contributor → `tran.prize == tienDuocTinh * (200 - tax) / 100`.
- **INV-8 (prize formula, Sicbo non-special):** winning contributor with `betSide ∉ [15..20]` → `tran.prize == tran.betValue * PotSicbo.getById(betSide).rotation`.
- **INV-9 (prize formula, Sicbo ONE_DICE_*):** `betSide ∈ [15..20]` → `tran.prize ∈ {bet*2, bet*3, bet*4}` matching occurrence count of `(betSide-14)`.
- **INV-10 (jackpot pool conservation):** `jp_next = max(50M, jp_prev + 0.006 * losing_pot)` unless `resetJp==true` → `jp_next = 50M`.
- **INV-11 (jackpot distribution sum):** `sum(tran.jpAmount) over winners == jp_at_trigger` (mod integer truncation drift ≤ winner_count).
- **INV-12 (txId uniqueness):** every `updateMoney` call gets unique `txId` per round. TaiXiu: `refId * 1e6 + (nanoTime & 0xFFFFF)`. Sicbo: `refId * 1e6 + atomic_seq`.
- **INV-13 (bet min):** every accepted bet → `bet.betValue >= 100`.
- **INV-14 (Sicbo TAI/XIU mapping):** `result = (d1+d2+d3 > 10) ? 1 : 0`; `listResult.contains("TAI")` iff sum ∈ [11..17] AND no triple.
- **INV-15 (Sicbo triple suppression):** any value × 3 → listResult contains NEITHER TAI/XIU/CHAN/LE/DOUBLE_DICES_*/POINT_* except 3 and 18.
- **INV-16 (pot reset):** after `startNewGame(refId)`, `potTai.contributors.isEmpty() && potXiu.contributors.isEmpty()`.
- **INV-17 (gameLoop tick monotonicity):** `count` reaches 68 (TaiXiu) / 55 (Sicbo) exactly once per round before reset.
- **INV-18 (RTP balancer feature gate):** `System.getenv("CANCUA_USE_DYNAMIC_RTP") == null` → pure random.
- **INV-19 (Sicbo fund decrement bound):** after `reward()`, `fundTaiXiu_next = fundTaiXiu_prev + (totalValueBetUser - totalPayout)`.
- **INV-20 (bet history mongo):** every successful real-player bet → exactly one `user_bet_tai_xiu` / `_sicbo` document.
- **INV-21 (txn code uniqueness):** `transactionCode = referenceId + "-" + betIndex` unique per bet per round (Sicbo).
- **INV-22 (livestream isolation):** `isUserBot()` Hazelcast bots DO debit real wallet; `UserCacheModel.isBot` TaiXiu bots do NOT (Sicbo all do).

## 10. References

| topic | path | lines |
|-------|------|-------|
| TaiXiu gameLoop | TaiXiuModule.java | 422-468 |
| TaiXiu round init | TaiXiuModule.java | 339-374 |
| TaiXiu force-result handler | TaiXiuModule.java | 258-286 |
| TaiXiu bet validation/debit | MGRoomTaiXiu.java | 388-503 |
| TaiXiu getRemainTime | MGRoomTaiXiu.java | 274-310 |
| TaiXiu cross-pot balance | MGRoomTaiXiu.java | 550-621, 825-984 |
| TaiXiu jackpot calc | MGRoomTaiXiu.java | 727-823 |
| TaiXiu settlement wallet credit | MGRoomTaiXiu.java | 1265-1373 |
| Sicbo gameLoop | SicboModule.java | 417-477 |
| Sicbo round init | SicboModule.java | 354-375 |
| Sicbo force-result handler | SicboModule.java | 275-303 |
| Sicbo bet validation/debit | MGRoomSicbo.java | 526-638 |
| Sicbo getRemainTime | MGRoomSicbo.java | 341-392 |
| Sicbo RTP balancer | MGRoomSicbo.java | 732-776 |
| Sicbo winning-status calc | MGRoomSicbo.java | 1090-1144 |
| Sicbo prize formula | MGRoomSicbo.java | 1170-1255 |
| Sicbo exploit guard | MGRoomSicbo.java | 1179-1200 |
| PotSicbo enum | PotSicbo.java | 7-58 |
| PotTaiXiu accumulator | PotTaiXiu.java | 32-112 |
| MinigameConstant | MinigameConstant.java | 12-56 |
| GenerationTaiXiu RTP balancer | GenerationTaiXiu.java | 93-166 |
| CanCuaRtpBalancer feature flag | CanCuaRtpBalancer.java | 35-73 |
| TaiXiuUtil dice helpers | TaiXiuUtil.java | 13-44 |
| DAL force-result read | TaiXiuServiceImpl.java | 503-510 |
| DAL jackpot Hazelcast read | TaiXiuServiceImpl.java | 577-595 |
| DAL save messages (RMQ) | TaiXiuServiceImpl.java | 512-575 |
| TransType enum | TransType.java | 6-26 |
| BetTaiXiuCmd | BetTaiXiuCmd.java | 14-37 |
| BetSicboCmd | BetSicboCmd.java | 23-46 |
| ForceResultTaiXiuCmd | ForceResultTaiXiuCmd.java | 14-27 |
| BotMinigame distribution | BotMinigame.java | 292-491 |
| BotMinigame night ratio | BotMinigame.java | 653-681 |

## AMBIGUOUS items (require decision)

1. Sicbo refId persistence mismatch (gameId 30 load / 31 save) — bug.
2. Sicbo force-result Hazelcast map name (`ketquataixiusicbo` write vs `ketquataixiu` read?) — verify DAL-SB.
3. Sicbo cross-side guard dead code (PotSicbo IDs never match 0/1).
4. Sicbo `fundTaiXiu < 0` infinite-loop risk.
5. `MINIGAME_TAX_TX_JACKPOT` unused in prize math — RMQ downstream or remove.
6. TaiXiu bots skip wallet debit; Sicbo bots always debit — inconsistent.
7. Settle-time wallet failure swallowed without retry (TXR:1350) — money loss.
8. `TaiXiuUtil.genarateResult` recursive no-op (TXU:33).
9. Missing referenced doc `docs/TAIXIU-SICBO-GAME-ARCHITECTURE.md`.
10. `balanceGate` flag semantics look inverted at TXR:665.
11. Refund-msg-vs-money desync between count=48 (msg) and count=55+ (money).
12. `MGRoomSicbo.calculateMoneyReturn` dead code.

**Extraction stance:** behavior-preserving. Replicate all 12 quirks exactly. Mark each with `// TODO(SUN-xxxx): legacy quirk preserved — fix in follow-up`. Fix in separate hardening MRs after extraction lands.

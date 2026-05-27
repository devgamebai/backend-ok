# Lottery Rules Specification (Lô Đề + XSMB)

Ground-truth spec for behavior-preserving extraction of the lottery surface. Sourced from JVM (CFR-decompiled) and .NET source in production.

> Source-of-truth files:
> - `api-xsmb-today-main/app.js`, `controllers/mainController.js`, `routes/v1/index.js`  (SCRAPE)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/LotteryModule.java`  (JLM)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/model/LotteryMode.java`  (JLMode)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/model/LotteryResult.java`  (JLRes)
> - `backend-master/game/Minigame/src/main/java/game/modules/minigame/cmd/rev/LotteryCmd.java`  (JLCmd)
> - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/LoDeServiceImpl.java`  (JLDalSvc)
> - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/dao/impl/LoDeDaoImpl.java`  (JLDao)
> - `banca/Core/Libs/Loto/LotoGame.cs`  (CLG)
> - `banca/Core/Libs/Loto/ScanXskt.cs`  (CSCAN)
> - `banca/Core/Libs/Database/LotoSql.cs`  (CLSQL)
> - `banca/Core/Libs/WebService/LotoService.cs`  (CLSVC)
> - `banca/ScanLoto/Program.cs`  (CSCAN2)
> - `install/config/mysql/db/full_backup.sql`  (SQL)
> - `docs/LOTTERY_LODE.md`, `docs/architecture/LOTTERY_SPLIT_PLAN.md`

## 1. Inventory — all lottery products

| # | Product | Stack | Surface | Handler/cmd | Draw cadence | Result source |
|---|---------|-------|---------|-------------|--------------|---------------|
| 1 | **XSMB result scraper** | Node 20 | HTTP GET `/api/v1`, port 49111 | `mainController.getAll` | On-demand pull | Scrapes `${LOTTERY_SCRAPE_URL:-az24.vn/xsmb-sxmb-xo-so-mien-bac.html}` per call (SCRAPE: mainController.js:9-130) |
| 2 | **Java Lô Đề** | Java / BitZero | BitZero TCP, handler 30000 (cmd 30001 = buyTicket) | JLM:188-203 | Daily settle at 18:35, period 86400000ms | OkHttp GET `${LOTTERY_API_URL:-http://lottery-api:49111/api/v1}` (JLM:105-108) → product #1 |
| 3 | **C# Lô Đề** | .NET, banca container, port 2083 | MessagePack routes `LOTO1..LOTO10` + Nancy HTTP `/lotoapi/*` | CLG:94-353, CLSVC | Scrape every 300s; settle every 300s (CLMAIN:56-111) | Scrapes `xskt.com.vn/xsmb/ngay-{D-M-YYYY}` (CSCAN:55-156). Window: 18:40-20:00 only |

**Not present:** Keno, Mega6/45, Power6/55, Max3D, XSMT/XSMN, instant-lottery, scratch-card. C# `LotoChannel` enum has 36 entries but only `MienBac=1` is wired. C# has 24 game modes but only Bắc modes (10 wired) generate any payout because Trung/Nam result feeds don't exist.

**MEGA_* envs in `CacheConfigName.java:235-253`** are Mega-Card payment gateway (telco/scratchcard top-up), unrelated to Mega-6/45 lottery.

## 2. Round lifecycle

### 2.1 Scraper (product #1)
Stateless. Each GET triggers fresh axios HTTP fetch. No cache, no schedule, no persistence. Response: `{ countNumbers, time, results: { ĐB:[], G1..G7:[] } }`. Healthcheck: `wget --spider http://localhost:49111/api/v1`.

### 2.2 Java Lô Đề lifecycle (product #2)
No round abstraction — bets accumulate all day, settle once.

| Phase | Window (intended VN local) | Behavior |
|-------|---------------------------|----------|
| Bet open | 00:00 → 17:00 | `buyTicket` accepts (JLM:192-202) |
| Bet locked | 17:00 → 19:00 | Handler short-circuits with log, **no error to client** (JLM:194-200) |
| Draw + settle | 18:35 daily | `scheduler.scheduleAtFixedRate(getResultLottery, …, 86400000L)` (JLM:95-97) |
| Bet reopen | 19:00 → 24:00 | `buyTicket` accepts (next-day session) |

**TZ AMBIGUITY:** `LocalTime.now()` uses JVM default TZ. Production `.env:62` = `TZ=Asia/Seoul`. Hanoi = `Asia/Ho_Chi_Minh`. **2hr gap.** Lockout fires at wrong wall hours. Settlement scrape runs at 16:35 Hanoi — before public draw. See anti-cheat audit.

17:00-19:00 reject path **does not refund and does not respond** — silent drop.

`init()` always calls `getResultLottery()` at boot (JLM:97) — settle runs at boot even if mid-day.

### 2.3 C# Lô Đề lifecycle (product #3)
Different window — 18:10 lock and 19:00 hard cutoff:

| Phase | Window | Behavior |
|-------|--------|----------|
| Bet open | 00:00 → 18:10 (`H==18 && M<=10`) | `LOTO1` accepts (CLG:99-104) |
| Bet locked | 18:11 → 18:59 | Code `301` "Time request from 0h to 18h05 VN Time" (comment lies). Hour ≥ 19 also rejected |
| Scrape attempt | 18:40 → 19:59 | `ScanXskt.Run` only runs in window; outside returns "Kết quả được lấy từ 18h40 -> 20h" (CSCAN:19-28) |
| Settle | 18:40+ | `CalculateResult` runs every 5min indefinitely; rows where `Status != 1` reprocessed (CLSQL:880-1013) |

`AddPlayRequest` rejects late session writes (CLSQL:739-740): `today > session` → returns 0. Client can't backdate.

`AddResult` rejects past-day results (CLSQL:762-763): only current-day session matches `today`.

**Server-authoritative** both stacks. Bet-window check wholly server-side.

## 3. Bet types + payouts

### 3.1 Java (`LotteryMode` enum, JLMode:14-24) — 10 modes

Constructor: `(id, vietnameseName, description, rate, prizeMultiplier)`. `rate` = bet-cost multiplier; `prizeMultiplier` = per-match payout; settle formula `prize = matches * userBet * prizeMul / rate` (where `userBet` pre-multiplied at purchase, so `/rate` cancels — SUN-1295 fix JLM:278-283).

Snapshot fields `bet_unit`, `rate_at_purchase`, `prize_multiplier` stamped on `lode` row at purchase (JLDao:41-53). Settle reads from row if present; falls back to live enum for legacy rows (JLM:252-263). **SUN-1295 anti-TOCTOU fix.**

| id | enum | Bet | Rate | Prize mult | Settle rule |
|----|------|-----|-----:|-----------:|-------------|
| 1 | LO_2_SO | single 2-digit | 22 | 80 | per-match across 27 prize lines (JLM:284-302) |
| 2 | LO_3_SO | single 3-digit | 23 | 600 | per-match across 24 prize lines (excl G7) |
| 3 | LO_XIEN_2 | 2 nums csv | 1 | 12 | flat if **2/2** match in rs27 |
| 4 | LO_XIEN_3 | 3 nums csv | 1 | 48 | flat if **3/3** match |
| 5 | LO_XIEN_4 | 4 nums csv | 1 | 160 | **win iff ≥3/4 match** (JLM:330-336). **Code differs from spec — flagged in docs/LOTTERY_LODE.md:33** |
| 6 | DAU | 1-digit | 1 | 8 | first digit of `de` (ĐB suffix 2-digit) |
| 7 | DUOI | 1-digit | 1 | 8 | last digit of `de` |
| 8 | DE_DAU | 2-digit | 1 | 80 | win iff any ĐB line ends with `num`. `prize = betValue * 80 / DUOI.getRate() = 80/1 = 80` (legacy divisor, JLM:348-352) |
| 9 | DE | 2-digit | 1 | 85 | win iff `de == num`. Flat `userBet * 85`. SUN-1295 lowered 95→85 |
| 11 | BA_CANG | 3-digit | 1 | 450 | win iff any ĐB line ends with `num` (3-digit). SUN-1295 lowered 900→450 |

Modes 0 and 10 don't exist in Java. Mode IDs stored in `lode.mode BIGINT`.

**Min/max bet (Java):** No explicit clamp. Only `currentMoney > finalBetValue` (JLM:225). `TextUtils.isEmpty(num)` is only invalid-input gate (JLM:208).

### 3.2 C# (`LotoGameMode` enum, CLSQL:66-94) — 24 modes (full catalog, only XSMB Bắc channel wired)

Region-aware PayRate (cost mult) and WinRate (payout mult) per mode. Bắc (channel `MienBac=1`) defaults below from CLSQL:188-441 (PayRate) and CLSQL:471-734 (WinRate). All overridable via Redis `loto_pay_rate_<mode*100+channel>` and `loto_win_rate_<mode*100+channel>` (CLSQL:166-185, 450-469). SUN-1295 lowered several Bắc rates.

| id | enum | Bắc PayRate | Bắc WinRate | Trung WinRate | Nam WinRate | Settle rule |
|----|------|------------:|------------:|--------------:|------------:|-------------|
| 0 | None | — | — | — | — | no-op |
| 1 | BaoLo2So | **22** | **80** | 80 | 80 | per-match `winTime * pay * 80` over 27 lines |
| 2 | BaoLo3So | 23 | 600 | 810 | 810 | per-match over G1..G6 (24 lines) |
| 3 | LoXien2 | 1 | 12 | 28 | 28 | flat if both match in 27 lines |
| 4 | LoXien3 | 1 | 48 | 150 | 150 | flat if all 3 match |
| 5 | LoXien4 | 1 | 160 | 710 | 710 | flat if **all 4** match. **Differs from Java mode 5 (3/4)** |
| 6 | Dau | 1 | 9.5 | 9 | 9 | per-match against ĐB 2nd char |
| 7 | Duoi | 1 | 9.5 | 9 | 9 | per-match against ĐB last char |
| 8 | DeDau | 4 | 95 | 82 | 83 | per-match against Result8 fallback Result7 last-2 |
| 9 | DeDacBiet | 1 | 85 | 83 | 83 | per-match against ĐB last-2 (SUN-1295: 85 not 95) |
| 10 | DanhDauDuoi | 2 | 85 | 85 | 85 | combo 8+9 |
| 11 | BaCang | 1 | 450 | 710 | 710 | per-match against ĐB last-3 (SUN-1295 900→450) |
| 12 | BaCangDau | 1 | 879 | 879 | 879 | per-match against Result7 last-3 |
| 13 | BaCangDuoi | 1 | 879 | 879 | 879 | per-match against ĐB last-3 |
| 14 | BaCangDauDuoi | 2 | 710 | 710 | 710 | combo 12+13 |
| 15 | LoTruotXien4 | 1 | 2.5 | 2 | 2 | win iff **none** of 4 match (CLSQL:1718-1771) |
| 16 | LoTruotXien8 | 1 | 80 | 3.5 | 3.5 | win iff none of 8 match |
| 17 | LoTruotXien10 | 10 | 110 | 4.5 | 4.5 | win iff none of 10 match |
| 18 | XiuChuDau | 1 | 710 | 710 | 710 | same as BaCangDau |
| 19 | XiuChuDuoi | 1 | 710 | 710 | 710 | same as BaCangDuoi |
| 20 | XiuChuDauDuoi | 2 | 710 | 710 | 710 | same as BaCangDauDuoi |
| 21 | Da2 | 1 | 28 | 28 | 28 | same as LoXien2 |
| 22 | Da3 | 1 | 150 | 150 | 150 | same as LoXien3 |
| 23 | Da4 | 1 | 710 | 710 | 710 | same as LoXien4 |
| 24 | LoTruotXien14 | 10 | 800 | 4.5 | 4.5 | win iff none of 14 match (CLSQL:1930-2032) |
| 25 | LoTruotXien12 | 10 | 400 | 4.5 | 4.5 | win iff none of 12 match. **Buggy — only checks 10/12 indices**, see AMBIGUOUS #6 |

**Input validation (CLSQL:1016-1156)** strictly checks count + digit length per mode. `LoTruotXien8`/`LoTruotXien10` use `break` instead of `return false` when count mismatches → silently pass when bad. AMBIGUOUS #4.

**Min/max bet (C#):** No explicit clamp. Wallet shortage → code `303`. Empty/wrong-shape → `301`/`302`.

### 3.3 XSMB result structure (both stacks)

ĐB = 5-digit jackpot. Suffix-based win checks:
- `de = last 2 digits of ĐB`
- `27 results pool` = ĐB ∪ G1 ∪ G2 ∪ G3 ∪ G4 ∪ G5 ∪ G6 ∪ G7 (JLRes:112-139)
- `24 results pool` = same minus G7 (JLRes:141-165)

C# stores into columns `ResultSp` (= ĐB), `Result1..Result7` (= G1..G7) on `loto_result` (CSCAN:126-138, CLSQL:765-778).

## 4. Result generation / scraping

### 4.1 Draw ingest

**Java path:**
- Scheduler 18:35 calls `getResultLottery()` (JLM:95-97)
- HTTP GET to `lottery-api:49111` → parse JSON → `LotteryResult` (JLM:105-119)
- De-dupe via `loDeService.getLatestResult(time)` (JLM:112-118)
- Persist new: `saveLotteryResult(jsonData, parsedDate)` (JLM:115)
- Iterate pending `getRecordsWithNullPrizeBefore1830Today(date)` (JLDao:91-127) → compute prize → credit wallet → `updatePrize(id, prize)` (JLM:121-132)

**C# path:**
- `RunScan` Task in `BanCaApplication.Main` polls every 300s
- `ScanXskt.Run` scrapes `xskt.com.vn/xsmb/ngay-{D-M-YYYY}` (CSCAN:55-56)
- `LotoSql.AddResult(session, MienBac=1, results, time)` inserts to `loto_result` (CLSQL:759-779)
- `RunHandleWinLoss` Task polls `CalculateResult` every 300s — settles `loto_request` rows where `Status != 1` (CLSQL:892-1014)

### 4.2 Force-result + admin overrides

- **No admin "force this draw" hook** for either stack. No analog of TaiXiu's `force_result_<refId>` Hazelcast.
- **Rate overrides (admin):**
  - C# HTTP POST `/lotoapi/setpayrate/{mode}/{channel}/{rate}` and `/setwinrate/...` (CLSVC:136-173) → Redis + in-memory cache
  - Java: NO runtime override — `LotteryMode` enum is source. `setRate`/`setPrizeMultiplier` exist (JLMode:44-77) but **never called from production paths**.
- **C# settle re-trigger:** HTTP POST `/lotoapi/calculateresultbysession/{session}` (CLSVC:100-112). Pushes `OnCalculatedLoto` to all WS clients.
- **Cache clear:** C# POST `/lotoapi/clearcache` (CLSVC:114-121). Does NOT clear `payRateCache`/`winRateCache`.
- **Allow modes/channels:** C# POST `/lotoapi/setallows` (CLSVC:244-260, CLG:383-410). **Dead code** — `LOTO1` does NOT check `allowChannels`/`allowModes` (CLG:96-204). AMBIGUOUS #5.

### 4.3 Persistence — tables

| Table | DB / pool | Owner | Purpose | Cols |
|-------|-----------|-------|---------|------|
| `lode` | `vinplay_minigame` / `mysqlpool_minigame` | Java | Per-ticket bet + settle | `id, user_id, nick_name, bet_value, mode, ticket, prize, created_date, updated_date, bet_unit, rate_at_purchase, prize_multiplier` (SUN-1295 snapshots) |
| `result_lottery` | `vinplay_minigame` | Java | Per-day XSMB JSON snapshot | `id, result TEXT, created_date DATETIME` |
| `loto_request` | `cgame` | C# | Per-ticket bet + settle | `Id, AppId, Username, Session, GameMode, Number, Channel, Pay, PayRate, Win, Status, TimePlay, TimeUpdate, WinNumber` |
| `loto_result` | `cgame` | C# | Per-session draw | `Id, Session VARCHAR(8), Channel, ResultSp, Result1..Result8, TimeResult` |
| `loto_gamemode` | `cgame` | C# | Lookup for `GetGameModes()` help payload | `Id, Name, Help, GroupName, Group, Location, GameMode` (empty in backup) |

**No Mongo collections** for lottery.

Distinct DBs: Java writes `vinplay_minigame.lode`; C# writes `cgame.loto_*`. **Two parallel histories** — no shared schema, no shared settle.

## 5. Wallet integration

### 5.1 Java (`LotteryModule.buyTicket`, JLM:206-236)
- **Debit:** `userService.updateMoney(user.getName(), -finalBetValue, "vin", "LoDe", "Lô Đề", "Cược <num> \n <modeName>", 0L, now, TransType.START_TRANS)` (JLM:227)
- **Credit on win:** `prvUserService.updateMoney(nickName, prize, "vin", "LoDe", "Lô Đề", "Thắng tiền cược", 0L, now, TransType.START_TRANS)` (JLM:129)
- Class: `UserServiceImpl` (same as minigames)
- Money type: hard-coded `"vin"`. No XU support.
- Game key: `"LoDe"`. Games enum entry: `LODE(67, "LoDe", "Lô Đề")`
- **TransType:** `TransType.START_TRANS` for BOTH debit and credit. No dedicated lottery type.
- Tables: shared `transaction_money` ledger + `lode` per-ticket row

### 5.2 C# (`LotoGame.LOTO1`, CLG:146-202)
Two paths gated on `MoneyGatewayClient.IsEnabled()` (Phase 5c flag):

**Unified-wallet (preferred):**
- Debit: `MoneyGatewayClient.SettleAsync(userId, -cost, "bc-loto-bet-<uid>-<mode>-<ts>", "WAGER_DEBIT_BANCA")` (CLG:151-158)
- Credit: `SettleAsync(uid, win, "bc-loto-win-<uid>-<requestId>", "WAGER_CREDIT_BANCA")` (CLSQL:957-967)

**Legacy Redis:**
- Debit: `RedisManager.IncEpicCash(userId, -cost, platform, "lotopay:<mode>", TransType.LOTO_PAY)` (CLG:161-163)
- Credit: `IncEpicCash(uid, win, "server", "loto_win:<mode>", TransType.LOTO_WIN)` (CLSQL:970-976)
- Then `MySqlProcess.Genneral.MySqlUser.SaveCashToDb(uid, currentCash)` flushes balance

**TransType enum** (RedisManager.cs:91-97):
```
XXENG_CHANGE_CASH = 50
LOTO_PAY          = 51
LOTO_WIN          = 52
```

Game keys: `"lotopay:<gameMode>"` (debit), `"loto_win:<gameMode>"` (credit). Subgame: `APP_ID = "xxeng"`.

## 6. Bots / virtual players
- **No betting bots** for Lô Đề.
- **Chat bots:** C# `LotoGame.sendMessBot` (CLG:419-432) auto-spams chat from `_listChat` + `_listNickName` every ≤3s. Cosmetic only.

## 7. House edge / RTP / kill-switches

| Flag | Used by | Effect |
|------|---------|--------|
| `LOTTERY_SCRAPE_URL` env | Node scraper | Source HTML URL |
| `LOTTERY_API_URL` env | Java | Where JVM polls scraper (JLM:105) |
| `URL` env in scraper | Node mainController | Same as LOTTERY_SCRAPE_URL after compose injection |
| Redis `loto_pay_rate_<mode*100+channel>` | C# | Per-mode-per-channel bet cost override |
| Redis `loto_win_rate_<mode*100+channel>` | C# | Per-mode-per-channel payout override |
| `MoneyGatewayClient.IsEnabled()` | C# | Unified-wallet REST vs legacy Redis |
| No `CANCUA_USE_DYNAMIC_RTP` analog exists for lottery | — | Payouts deterministic per draw + bet — no RTP balancer |

**No "lottery off" kill-switch.** Workarounds: stop `lottery-api` container, revoke MySQL, set bogus `LOTTERY_API_URL`, or use C# `/lotoapi/setallows` (but it's dead code — `LOTO1` ignores it).

## 8. Edge cases / known quirks

1. **Mode 5 Java settles 3-of-4 (JLM:330-336); C# settles 4-of-4 (CLSQL:1331-1384).** Disagreement. Doc `LOTTERY_LODE.md:33` flags this.
2. **C# `LoTruotXien12` (mode 25) only iterates 10/12 indices** (CLSQL:1798-1928). Player wins with 12 nums against draw hitting nums [10] or [11]. Confirmed bug.
3. **C# settle has no idempotency.** Status remains 0 on crash mid-credit → next cycle re-credits. AMBIGUOUS.
4. **Java settle window `(yesterday 18:59, today 18:10)`** skips bets placed at 18:10-18:59 of prior day → permanent stuck rows (JLDao:99-103).
5. **Both stacks settle independently, both debit same user.** Cocos via wbanca writes `loto_request`; BitZero cmd 30001 writes `lode`. No data leak, but double-debit possible if both clients run.
6. **C# raw SQL with `MySqlHelper.EscapeString`** (CLSQL:743-746, 2193-2195). Number field as JSON-stringified array unescaped. Java DAO uses prepared statements.
7. **C# `AddPlayRequest` returns `Math.Round(rate*pay)`** — C# default `MidpointRounding.ToEven`: `rate=2.5, pay=1 → 2 not 3`. Penny edge.
8. **Java `LotteryResult.Results` field name `ĐB`** uses literal Unicode. Reflection-based renaming breaks it.
9. **C# `ScanXskt` parses fixed `data.Count != 12`** — any source HTML change at xskt.com.vn instantly breaks scraping (CSCAN:83-89).
10. **Node `getById` unimplemented** (`mainController.js:128`) — silent 200 with no body.
11. **C# AppId hardcoded `"xxeng"`** (CLG:16) — no per-tenant routing.
12. **`LobbyService` Lobby + `NetworkServer` captured from FIRST request** (CLG:92-93). Dev clone would never update ref.

## 9. Invariants (testable assertions)

1. **INV-LOTTERY-01:** 18:35 scheduler is the ONLY way a row enters `result_lottery`. (JLM:95-97, JLDao:168-180)
2. **INV-LOTTERY-02:** Twice-daily `getResultLottery()` must not duplicate `result_lottery` row. (JLM:112-118)
3. **INV-LOTTERY-03:** Between 17:00-19:00 local, cmd 30001 must NOT debit any wallet. (JLM:192-200)
4. **INV-LOTTERY-04:** Between 18:11-23:59 local, `LOTO1` returns code 301 + no `IncEpicCash` call. (CLG:100-104)
5. **INV-LOTTERY-05:** Mutating `LotteryMode.rate` AFTER bet placed must NOT change settled prize. SUN-1295. (JLM:215-232, JLDao:41-53)
6. **INV-LOTTERY-06:** Mode 1 settle: `prize = matches * userBet * 80 / 22`. (JLTest:26-44, JLM:284-302)
7. **INV-LOTTERY-07:** Mode 5 settle rule: pick 3/4 OR 4/4. Currently Java=3/4, C#=4/4. **MUST reconcile before extraction.**
8. **INV-LOTTERY-08:** Each `loto_request` row → exactly one wallet debit. (CLG:148-202)
9. **INV-LOTTERY-09:** No `loto_request` Status=1 reverts to 0. (CLSQL:892-1013)
10. **INV-LOTTERY-10:** BaoLo2So rejects array, LoXien2 rejects scalar. (CLSQL:1020-1067, CLG:113-132)
11. **INV-LOTTERY-11:** Setting `loto_pay_rate_122` in Redis to 999 → `GetPayRate(BaoLo2So, MienBac)` returns 999. (CLSQL:166-185)
12. **INV-LOTTERY-12:** C# mode 5: exhaustively test 2^4=16 match patterns; only 1111 wins. (CLSQL:1331-1384)
13. **INV-LOTTERY-13:** Modes 15-17, 24-25: any match → 0 prize. (CLSQL:1718-2033)
14. **INV-LOTTERY-14:** Cross-stack debit isolation: `users.cash` debited exactly twice (not 4×) for one Java + one C# bet same minute. (JLM:227 vs CLG:148-163)
15. **INV-LOTTERY-15:** `ScanXskt.Run` outside 18:40-19:59 returns early without writing `loto_result`. (CSCAN:19-28)
16. **INV-LOTTERY-16:** Scraper failure → no `loto_request` Status transitions to 1 → all bets remain pending. No auto-refund. Specify expected (refund? manual?). (JLM:121-132)

## 10. AMBIGUOUS items (12)

1. **Mode 5 Java vs C# (3/4 vs 4/4).** Ops must declare canonical rule.
2. **No `referenceId` aggregate for lottery.** C# uses `Session = YYYYMMDD`. Decide if extraction introduces explicit `LotteryRound`.
3. **17:00-19:00 Java hard-coded** (JLM:194-195). Parameterise? C# 18:10 cutoff disagrees by 70min.
4. **C# `break` vs `return false`** in `LoTruotXien8/10` validation (CLSQL:1134-1135, 1144-1145).
5. **`allowModes/allowChannels`** dead code in C# (CLG:65-66, 359-410).
6. **C# `LoTruotXien12` only loops 10/12 indices** (CLSQL:1798-1928). Bug or "trượt 10 of 12" variant?
7. **Java settle window skips 18:10-18:59 of prior day** (JLDao:100-101). Intentional?
8. **No refund hook on scraper failure.** Today: tickets stay Status=0 forever. Specify intent.
9. **Java mode 8 divides by `DUOI.getRate()` = 1** (JLM:351). Legacy no-op. Remove?
10. **Java money type fixed `"vin"`** (JLM:93, 129, 227). No XU lottery exists. Confirm intentional.
11. **Two parallel lottery stacks coexist, both can debit same `User.cash`.** Intentional or collapse?
12. **C# `LotoChannel` 36 entries, only `MienBac=1` used.** Drop Trung/Nam until result feeds exist?

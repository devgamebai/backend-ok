# Lottery Anti-Cheat Audit — Pre-Reveal Leak + Bet-After-Result + REST/WS-Chat Refactor

**Audit date:** 2026-05-14
**Scope:** XSMB (Xổ Số Miền Bắc) — the only lottery product. No Keno/XSMT/XSMN.
**Methodology:** Same as `taixiu-sicbo-anticheat-audit.md`. Classifications: **LEAK** / **INTERNAL** / **DEFERRED**.

## 🚨 LIVE EXPLOIT — TZ + Window misalignment

**Operator policy clarification (2026-05-14):** global container TZ is `Asia/Seoul` BY DESIGN — TaiXiu/Sicbo/all other realtime games run on Korea time. **Lottery is the ONLY exception** — XSMB is a Vietnamese product and must follow Hanoi time.

Production `.env:62 TZ=Asia/Seoul`. Game-minigame container clock = Seoul (+2hr vs Hanoi). `LotteryModule.handleClientRequest` uses `LocalTime.now()` with no explicit ZoneId — picks up JVM default = Seoul. **Bug: lottery code inherited container TZ instead of pinning Hanoi.**

Real wall-clock effects:
- Scheduler `18:35` fires at **16:35 Hanoi** — BEFORE public draw at 18:15 Hanoi → scrape pulls yesterday's data
- Lockout `17:00-19:00 Seoul` = **15:00-17:00 Hanoi** — locks before draw, opens at 17:00 Hanoi (1h15min before draw)
- Bets accepted from 17:00 Hanoi onwards, including AFTER public draw at 18:15
- Settlement window `(yesterday 18:59, today 18:10)` filter is also TZ-shifted

**Status:** Live functional bug in lottery only. Other games intentionally on Seoul TZ and unaffected.

**Required fix (surgical, lottery-only):**
Replace every `LocalTime.now()` / `LocalDate.now()` / scheduler trigger in `LotteryModule` and `LoDeDaoImpl` with explicit `ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))`. Do NOT touch global `.env TZ` — Korea is intentional for the rest of the stack.

## 1. Current reveal/draw timing

Lottery model differs from TaiXiu/Sicbo: **no in-server RNG, no realtime tick.** XSMB drawn externally at ~18:15 Hanoi on TV. Server pulls via HTTP scrape every 24h + on startup.

### XSMB — `LotteryModule.init()` + `getResultLottery()` (JLM:89-142)

| Phase | Action | File:Line |
|-------|--------|-----------|
| Server boot | `init()` runs `getResultLottery()` once, schedules at 18:35 local with 24h period | :94-97 |
| 18:35 daily | `getResultLottery()` fires on ScheduledExecutorService | :96 |
| GET | `OkHttpClient.newCall(GET http://lottery-api:49111/api/v1).execute()` blocks until scraper returns | :107-109 |
| Dedupe | `loDeService.getLatestResult(time)` → if row exists for `time` key, use stored, ignore scraped | :112-118 |
| Persist | If new: `saveLotteryResult(jsonData, parsedDate)` writes `result_lottery` row | :115 |
| Settle | `getRecordsWithNullPrizeBefore1830Today(date)` returns pending bets. Loop: compute prize, credit, write `prize` via `updatePrize` | :121-132 |
| Player-visible | NONE direct. REST poll of `HistoryLotteryResultProcessor` after persist commits, OR `HistoryLotteryProcessor` (sees `prize` column populated) | indirect |

### Where result becomes visible

- Server JVM: moment OkHttp body parsed (:111)
- DB: after `saveLotteryResult` commits (:115)
- Player REST: any time after DB commit
- Player external (TV/web): ~18:15 Hanoi from public draw
- Server official "publish": **no event.** Settlement silent. Player polls.

### Gap inverted

Risk is **NOT** "server learns then publishes." Reality: **player learns public XSMB result via TV/web BEFORE server's polling cycle pulls and settles it.** Bets accepted in that window = bet-after-result attacks. See §3.

## 2. Pre-reveal leak audit

### 2.1 Audit matrix

| # | Path | File:Line | Class | Notes |
|---|------|-----------|-------|-------|
| 1 | `getResultLottery` `System.out.println(lotteryResult)` | JLM:120 | INTERNAL→**LEAK-LIKE** | Prints entire result struct (ĐB, G1-G7) to stdout BEFORE settlement loop. Container logs. Ops/log-pipeline sees result before settle. MED if forwarded to SaaS. |
| 2 | `getResultLottery` `saveLotteryResult` write | JLM:115 → JLDao:168-180 | DEFERRED→**LEAK** | Writes raw JSON to `result_lottery` BEFORE settlement loop touches user bets. `HistoryLotteryResultProcessor` (:21) returns `getListOfResultsByDateRange()` — player polls REST immediately after 18:35 scrape commit, sees today's result. **Bet-after-result surface.** |
| 3 | `buyTicket` `TelegramAlert.sendMessage` | JLM:224 | INTERNAL | Sends `nickname/bet/mode/num/timestamp` to Telegram ops surveillance. Pre-purchase. Privacy concern not anti-cheat. |
| 4 | `handleClientRequest` `System.out.println` | JLM:197, 233 | INTERNAL | Server stdout only. |
| 5 | `getRecordsWithNullPrizeBefore1830Today` SELECT | JLDao:91-127 | INTERNAL | Server-side query. |
| 6 | `getLotteryTicketByUserName` SELECT | JLDao:133-162 | DEFERRED | Returns `prize` column. Non-null only AFTER `updatePrize` commits. Safe by construction. |
| 7 | `HistoryLotteryResultProcessor.execute` | :25-34 | **LEAK** | See L-1. Returns raw 6-day JSON dump including TODAY'S result as soon as `result_lottery` row exists. No time gate. |
| 8 | `ListLotteryTransactionProcessor.execute` | :19-43 | **LEAK** | Backend admin endpoint. Concat-string SQL on `nickname/ticket/model/timeStart/timeEnd` (JLDao:243-245). **SQL injection** + result-exposure. |
| 9 | `LotteryMode` mutable setters | JLMode:44, 76 | INTERNAL | Public mutability on static enum. SUN-1295 snapshots mitigate for FUTURE bets. Legacy non-snapshot rows still vulnerable. Grep: zero callers. |
| 10 | ws-bridge lottery mapping | bridge.js:1-651 | N/A | grep `lottery|Lottery|30000|30001|lode` = zero hits. **Lottery has no ws-bridge presence.** Binary minigame socket handler 30000 direct. |
| 11 | `HistoryLotteryProcessor` returns `LotteryMessage` list | :25-34 | DEFERRED-SAFE | Returns only `prize` field — populated post-settle. Authn: `un` query param no validation. |
| 12 | Cmd 30001 betting time-window check | JLM:193-201 | **CRITICAL LEAK ENABLER** | Rejects only if `LocalTime.now() ∈ (17:00, 19:00)`. Outside window — including 19:00:01+ after public result known — bets accepted. See §3. |

### 2.2 LEAK findings (strict)

**L-1 — HistoryLotteryResultProcessor exposes today's result before settlement.**
- File:line: `HistoryLotteryResultProcessor.java:25-34`, `LoDeDaoImpl.java:208-224`
- **Severity: HIGH**
- Detail: As soon as `saveLotteryResult` commits, REST endpoint visible with no time gate. Settlement loop follows but takes O(N). No flag distinguishes "row inserted, settle pending" from "settle complete." Player polls, sees result is in, files new bet (subject to broken 17-19 lockout).

**L-2 — Settlement-order partial visibility.**
- `LoDeDaoImpl.java:74-85` (`updatePrize`)
- **Severity: LOW**
- Each `updatePrize` own transaction. During ~seconds-long settle loop, user A's bet shown settled while B's not. Operational concern only.

**L-3 — Scrape stdout dump.**
- `LotteryModule.java:120` (`System.out.println(lotteryResult)`)
- **Severity: LOW**
- Docker JSON logs with rotation. If forwarded to ELK/Datadog with broad read access, ops sees results 1-2s before settle. Not player-reachable.

### 2.3 Safe paths

- `getResultLottery` OkHttp call (:106-109) — server-side network egress
- Scrape JSON `Gson.fromJson` (:111) — JVM heap
- `loDeService.getLatestResult` (:112) — server-side SELECT
- `saveTransactionLode` (:229-232) — pre-result, bet metadata only
- Per-bet rate/prize snapshot columns (SUN-1295) — clean

### 2.4 Missing invariant

TaiXiu has `resultTX == null` until tick=51 enforced server-side. **Lottery has NO equivalent invariant.** Result visible to clients via REST as soon as DB write commits, no settle-complete gate. **Primary anti-cheat finding.**

## 3. Bet-after-result attack vector — CRITICAL

Real, currently live in production.

### 3.1 Threat model
1. Public XSMB draw concludes ~18:15 Hanoi
2. Result on TV, Twitter, every aggregator within ~5min
3. Server polling cycle 18:35 daily (JLM:95) — fixed 20-min lag by design
4. Attacker knows public result at 18:16, places bet at 18:17 if server accepts

### 3.2 Server cutoff actuality

`LotteryModule.handleClientRequest` case 30001 (:192-202):
```java
LocalTime currentTime = LocalTime.now();
LocalTime startTime = LocalTime.of(17, 0);
LocalTime endTime   = LocalTime.of(19, 0);
if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
    System.out.println("Operation rejected due to time restriction.");
    break;
}
this.buyTicket(user, dataCmd);
```

| Issue | Detail | Severity |
|-------|--------|----------|
| **TZ ambiguity** | `LocalTime.now()` = JVM default TZ. Production `.env:62 TZ=Asia/Seoul`. Real Hanoi cutoff fires 2hr off | **CRITICAL** (live) |
| **Cutoff too late** | Window is 17:00-19:00. Public XSMB results from 18:15. 45min "locked" coincides w/ result already public — blocks honest players. 19:00:01-23:59 wide open with result potentially known via L-1 | HIGH |
| **One window only** | Same 17-19 daily. No round-based logic | HIGH |
| **Boundary semantics** | `isAfter / isBefore` strict — 17:00:00 and 19:00:00 both unlocked. Effective open interval `(17:00, 19:00)` | LOW |
| **No NTP guard** | Server clock drift shifts lockout | LOW |
| **Bet at 19:00:01 w/ result in DB** | If scrape ran at 18:35 successfully, `result_lottery` has today. L-1 REST reveals it. Bet at 19:00:01 enters `lode` `prize IS NULL`, `created_date = 19:00:01`. Settlement next day `WHERE created_date < D 18:10` — catches this bet → settles against today's result. **Free money.** | **CRITICAL** |

### 3.3 Attack sequence (concrete, w/ TZ broken)

Assuming TZ=Seoul (current production):
1. T = 18:15 Hanoi (= 20:15 Seoul) — Public draw on TV. Attacker reads ĐB on Twitter.
2. T = 16:35 Hanoi next day (= 18:35 Seoul) — Scheduled scrape runs (wrong wall hour). Pulls "today" which is yesterday's draw to `az24.vn` (might be cached today's). Commits `result_lottery`.
3. T = 17:00 Hanoi (= 19:00 Seoul) — Lockout expires (boundary). Attacker POSTs `LotteryCmd` mode=9 DE with last 2 digits of ĐB.
4. T = 17:00:01 Hanoi — `buyTicket` accepts. Debits, inserts `lode` row.
5. T = 16:35 Hanoi next day — Settlement loop reads pending rows with `created_date < D 18:10` (Seoul wall clock = D 16:10 Hanoi). Attacker's bet at D 17:00 Hanoi NOT in window (it's > D 16:10 Hanoi). Slipped.

OK actually with TZ broken, the timing math gets WEIRD. Different bug class: settlement may apply WRONG date's results, bets may stuck forever. Not all clean exploits, but the lottery is BROKEN.

If TZ were correct (Hanoi):
1. T = 18:15 — TV draw
2. T = 18:35 — Scrape, commit `result_lottery`
3. T = 19:00:01 — Lockout expires. Attacker bets DE.
4. T = D+1 18:35 — Settlement loop. Window `WHERE created_date < D+1 18:10 AND > D 18:59`. D 19:00:01 IS in window. Settled against D+1's stored result (might also be D's via dedup) → wrong-date but still potentially profitable.

Settled at 50-min gap between settlement window end (18:10) and lockout end (19:00). Critical bug regardless of TZ.

### 3.4 Authz on REST

- `HistoryLotteryProcessor.execute` reads `un` query param no validation (:28). Container auth assumed.
- `HistoryLotteryResultProcessor.execute` no auth gate visible (:25-34).
- `ListLotteryTransactionProcessor` backend-tier, admin-fenced at gateway. Concat-string SQL is **SQL injection** regardless of auth tier.

## 4. F12 / packet inspection

### Player-visible surface

No realtime WebSocket frames for lottery. ws-bridge handles zero lottery cmds. Surface:

| Channel | Pre-reveal | Post-reveal |
|---------|-----------|-------------|
| Binary WS cmd 30001 (BUY) | Request side only | Request only |
| REST `HistoryLotteryResultProcessor` | empty (before 18:35 scrape) | full JSON, 6-day window |
| REST `HistoryLotteryProcessor` | own pending w/ prize=null | own with prize populated |
| REST `ListLotteryTransactionProcessor` (admin) | n/a | full table dump |
| Container stdout (not player) | scrape-time println | — |

### Must never expose pre-publish (recommended invariant)
- `result_lottery` row for today until `settle_complete=true` flag flips
- Detail-level result fields (ĐB, G1-G7) in any REST body

### Currently inappropriate
- L-1: `HistoryLotteryResultProcessor` exposes JSON moment scrape DB write commits, BEFORE settle completes

### JS-heap attack
No Cocos object holds lottery state realtime. Re-poll REST only. Attack at HTTP layer.

## 5. Proposed hardening

### 5.1 Server-authoritative lock window — replace LocalTime check

```java
public final class LotteryClock {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalTime LOCK_TIME   = LocalTime.of(18, 10);
    public static final LocalTime SCRAPE_TIME = LocalTime.of(18, 35);
    public static final Duration  POST_LOCK_HOLD = Duration.ofMinutes(45); // until 18:55

    public static boolean isBettingOpen(Clock clock, boolean todaySettleComplete) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(VN));
        LocalTime t = now.toLocalTime();
        if (t.isAfter(LOCK_TIME) && t.isBefore(LOCK_TIME.plus(POST_LOCK_HOLD))) return false;
        if (t.isAfter(LOCK_TIME) && !todaySettleComplete) return false;
        return true;
    }
}
```

3 changes:
1. Explicit `ZoneId.of("Asia/Ho_Chi_Minh")`. Never trust JVM default
2. Lock at 18:10 (5 min before draw), not 17:00
3. Hold until `settle_complete = true` for today, regardless of wall clock

### 5.2 settle_complete flag (snapshot censoring, lottery-flavor)

Add `settled_at TIMESTAMP NULL` to `result_lottery`. Set non-null only at end of settlement loop.

```java
private static void getResultLottery() {
    LotteryResult rs = scrapeAndPersist();  // commits row, settled_at=NULL
    settleAllPending(rs);                    // O(N) loop
    loDeService.markSettled(rs.getTime());   // UPDATE result_lottery SET settled_at=NOW()
}
```

`HistoryLotteryResultProcessor.getListOfResultsByDateRange()` returns only rows where `settled_at IS NOT NULL`. Player never sees today's result via REST until settle finishes.

### 5.3 Settlement critical section

Acquire row-level lock on `lottery_lock` table or Redis `SETNX lottery:scraping`. Any `buyTicket` during critical section returns `result=2`.

### 5.4 Cutoff alignment fix

`getRecordsWithNullPrizeBefore1830Today` (JLDao:100) upper bound must MATCH lockout time. Today: settle says `< 18:10`, lockout says `< 17:00 OR > 19:00`. 19:00-23:59 zone is the bug. Settle should be `< previous_day 18:10`, lockout `> 18:10` until `next_day 18:55`. **One source of truth: `LotteryClock.LOCK_TIME`.**

### 5.5 stdout println suppression
Replace `System.out.println(lotteryResult)` (JLM:120) with debug log gated by phase check.

### 5.6 Telegram alert sanitization
`buyTicket` `TelegramAlert.sendMessage` (:224) ships `nickname`. Restrict channel or hash.

### 5.7 SQL injection fix
`ListLotteryTransactionProcessor` → `LoDeDaoImpl.search/count` (:227-294). Use `PreparedStatement` placeholders, not string concat.

### 5.8 Force-result + admin overrides
No analog of TaiXiu's `forceResultTaiXiu` — XSMB external. Reject ANY admin write to `result_lottery` outside scheduled scraper. Audit log all writes. If ops needs to fix botched scrape: dedicated endpoint behind explicit `Role.SUPERADMIN` + dual-control.

### 5.9 Reveal phase machine
```
DRAW_PENDING → DRAW_LOCKED → SCRAPING → SETTLING → SETTLED → DRAW_PENDING (next day)
```

| Phase | Trigger | Bet accept? | Result visible? |
|-------|---------|-------------|-----------------|
| DRAW_PENDING | 00:00 - 18:10 Hanoi | YES | Yesterday's settled only |
| DRAW_LOCKED | 18:10 wall clock | NO | Yesterday's only |
| SCRAPING | 18:35 scheduler | NO | Yesterday's only |
| SETTLING | scrape row committed | NO | Yesterday's only (today censored — `settled_at IS NULL`) |
| SETTLED | settle finishes, `settled_at=NOW()` | YES (re-opens) | Today's settled visible |

## 6. Refactor — REST + WS-chat-only

### 6.1 Engine extraction

```
lottery-engine/  (pure Java, no Bitzero, no Netty)
  src/main/java/io/sunwin/lottery/
    engine/
      LotteryClock.java          // lock/unlock, ZoneId.of("Asia/Ho_Chi_Minh")
      LotteryPhase.java          // enum DRAW_PENDING|LOCKED|SCRAPING|SETTLING|SETTLED
      DrawIngest.java            // OkHttp pull, JSON→LotteryResult, idempotent dedupe
      PrizeCalculator.java       // computePrize() pure function
      BetValidator.java          // mode validation, num format, bet limits, lock check
      SettleService.java         // O(N) settle loop with transaction
      RevealGuard.java           // throws if result requested in non-SETTLED phase
    model/
      LotteryMode.java           // SUN-1295 enum, getters only — remove setters
      LotteryResult.java         // immutable
      LotteryTicket.java         // immutable, snapshot fields
```

### 6.2 Spring REST surface

| Method | Path |
|--------|------|
| GET | `/api/v2/lottery/products` |
| GET | `/api/v2/lottery/xsmb/state` |
| GET | `/api/v2/lottery/xsmb/result/{date}` (404 if `settled_at IS NULL`) |
| GET | `/api/v2/lottery/xsmb/results?from=&to=` (filter `settled_at NOT NULL`) |
| POST | `/api/v2/lottery/xsmb/bet` |
| GET | `/api/v2/lottery/xsmb/history?userId=` (auth: caller==userId OR admin) |
| POST | `/api/v2/lottery/xsmb/admin/settle` (admin, idempotent via `settled_at`) |
| GET | `/api/v2/lottery/xsmb/admin/transactions` (PreparedStatement fix) |

### 6.3 WS-chat only

| Topic | Payload | Purpose |
|-------|---------|---------|
| `/topic/lottery/xsmb/chat` | `{user, msg, ts}` | Room chat. **No game state.** |
| `/topic/lottery/xsmb/announce` | `{type:"settled", date, settledAt}` | One-shot push on settle complete. **No result fields.** Triggers REST GET |
| `/topic/lottery/xsmb/lock` | `{phase, lockTime}` | One-shot on phase transition. No result |

Result data **never travels over WS.** WS = notification trigger only. Clients always GET via REST; authn + `settled_at NOT NULL` gate exposure.

Matches user requirement: "refactor the same use rest api only keep the chat for websocket."

### 6.4 Required JUnit

```java
@Test void betRejectedDuringScrapeWindow() {
    Clock c = Clock.fixed(Instant.parse("2026-05-14T18:30:00+07:00"), VN);
    assertFalse(LotteryClock.isBettingOpen(c, false));
}

@Test void betRejectedAfterScrapeUntilSettled() {
    Clock c = Clock.fixed(Instant.parse("2026-05-14T19:00:01+07:00"), VN);
    assertFalse(LotteryClock.isBettingOpen(c, false));
}

@Test void betOpenedAfterSettleComplete() {
    Clock c = Clock.fixed(Instant.parse("2026-05-14T19:00:01+07:00"), VN);
    assertTrue(LotteryClock.isBettingOpen(c, true));
}

@Test void resultEndpointReturns404PreSettle() {
    settler.recordScrape(today, json);   // settled_at = null
    assertThrows(ResultNotPublishedException.class, () -> controller.result(today));
}

@Test void timezoneAlwaysHanoi() {
    System.setProperty("user.timezone", "UTC");
    Instant lockMoment = LocalDate.of(2026,5,14).atTime(18,10).atZone(VN).toInstant();
    assertFalse(LotteryClock.isBettingOpen(Clock.fixed(lockMoment, ZoneOffset.UTC), false));
}

@Test void sqlInjectionRejectedInSearch() {
    assertThrows(IllegalArgumentException.class,
        () -> dao.search("' OR 1=1 --", null, null, null, null, 1, 10));
}
```

### 6.5 Migration

- Add `lode.settled_at TIMESTAMP NULL`; backfill from `prize IS NOT NULL`
- Add `result_lottery.settled_at TIMESTAMP NULL`; backfill = `created_date + 5min`
- Switch `result_lottery` query to `WHERE settled_at IS NOT NULL`
- **Lottery code pins `ZoneId.of("Asia/Ho_Chi_Minh")` everywhere.** Do NOT change global container `TZ` env — Korea TZ is intentional for TaiXiu/Sicbo/other realtime games. Vietnam TZ is lottery-only and enforced at code level.

## 7. Findings summary

### Severity table

| Severity | LEAK | Adjacent |
|----------|------|----------|
| **CRITICAL** | **1** (bet-after-result via L-1 + cutoff misalignment §3) | **1** (TZ default `LocalTime.now()` — live on production) |
| HIGH | 1 (L-1 pre-settle result REST) | 1 (SQL injection `LoDeDaoImpl.search`) |
| MEDIUM | 0 | 1 (Telegram nickname egress) |
| LOW | 2 (L-2, L-3) | 2 (LotteryMode setters; no NTP guard) |

**Total LEAK: 3.** Meta-finding: lock window + settlement window misalignment enables CRITICAL bet-after-result.

### Top 5 priority fixes

1. **🚨 CRITICAL — Replace `LocalTime.now()` with `ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))`** in lottery code only. Do NOT change global container `TZ` (Korea is intentional for other games). Apply to: `LotteryModule.handleClientRequest` (JLM:193-201), scheduler trigger (JLM:95-97), `LoDeDaoImpl.getRecordsWithNullPrizeBefore1830Today` (JLDao:91-127), `addNewLode` (JLDao:41-53), and any other lottery `LocalDate.now()`/`LocalTime.now()` callsite.

2. **CRITICAL — Tie lockout end to `settle_complete` flag, not wall clock 19:00.** Lock at 18:10 Hanoi. Hold until `result_lottery.settled_at IS NOT NULL` for today's date.

3. **CRITICAL — Add `settled_at` flag + REST censoring** (JLM:115, JLDao:208-224, `HistoryLotteryResultProcessor.java:25-34`). Result row not visible via REST until settle loop finishes.

4. **HIGH — Realign settlement window with lockout** (JLDao:100-101). Both reference `LotteryClock.LOCK_TIME`.

5. **HIGH — Fix SQL injection** `LoDeDaoImpl.search/count` (:227-294). Use PreparedStatement placeholders for nickname/ticket/model/timeStart/timeEnd.

### Open AMBIGUOUS

- **A1** — Container TZ: confirmed `.env:62 TZ=Asia/Seoul`. **Live bug.**
- **A2** — Authn on `HistoryLotteryProcessor`: confirm `un` parameter bound to session user upstream
- **A3** — Multi-instance scraper: if N gameservers each schedule `getResultLottery`, concurrent scrape→settle possible. DB-level lock needed
- **A4** — Cocos client lottery path: confirmed binary WS to cmd 30001. ws-bridge translates zero lottery cmds

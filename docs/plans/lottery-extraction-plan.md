# Lottery Engine Extraction Plan (XSMB / Lô Đề)

**Status:** Plan-only. Behavior-preserving extraction from BitZero `LotteryModule` into `lottery-engine` (pure Java) + `lottery-api` (Spring Boot 2.7.18). REST-only data plane; WS-STOMP for chat + announce only.

**Source-of-truth specs:**
- `/root/sunwinkr/sunwinkr/docs/specs/lottery-rules-spec.md` (16 invariants, 12 AMBIGUOUS, 3 stacks)
- `/root/sunwinkr/sunwinkr/docs/specs/lottery-anticheat-audit.md` (3 LEAK findings, CRITICAL TZ bug)
- Analog: `/root/sunwinkr/sunwinkr/docs/plans/taixiu-extraction-plan.md`

## Decisions (binding for Phase 1)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | **Option B: extract Java side only, retire BitZero handler 30000; leave C# `LotoGame` untouched** | Java is cmd 30001 entry; C# `wbanca` asymmetric (Cocos web), stable. Smaller blast radius. Unify (Option C) ~6 months out |
| D2 | **Mode 5 canonical = 4/4 (Xiên 4 strict)** | C# does this; Java 3/4 is documented bug per `docs/LOTTERY_LODE.md:33`. Feature flag `LOTTERY_MODE5_LEGACY_3OF4=1` for 1-release rollback |
| D3 | **Pin `ZoneId.of("Asia/Ho_Chi_Minh")` at code level — do NOT touch global `.env:62 TZ=Asia/Seoul`** | Korea TZ intentional for TaiXiu/Sicbo/realtime games. Vietnam TZ lottery-only |
| D4 | **REST-only data plane; WS-STOMP for chat + one-shot announce only** | Per explicit user requirement |
| D5 | **`settled_at TIMESTAMP NULL` flag on both `lode` and `result_lottery`** | Closes L-1 (pre-settle result reveal) |
| D6 | **No `LotteryRound` aggregate. Session key = `YYYY-MM-DD` Vietnam** | Daily cycle, no realtime tick |

## Section 1: Gradle module setup

### 1.1 `settings.gradle`
```groovy
include 'game:lottery-engine'
include 'game:lottery-api'
project(':game:lottery-engine').projectDir = file('game/lottery-engine')
project(':game:lottery-api').projectDir    = file('game/lottery-api')
```

### 1.2 `lottery-engine/build.gradle` (pure Java)
```groovy
plugins { id 'java-library' }
group = 'com.sunwinkr'; version = '0.1.0-SNAPSHOT'
sourceCompatibility = '1.8'; targetCompatibility = '1.8'
dependencies {
    implementation 'org.slf4j:slf4j-api:1.7.36'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.assertj:assertj-core:3.24.2'
    testImplementation 'net.jqwik:jqwik:1.8.4'
}
test { useJUnitPlatform { includeEngines 'junit-jupiter', 'jqwik' } }
```

### 1.3 `lottery-api/build.gradle` (Spring Boot 2.7.18)
```groovy
plugins { id 'org.springframework.boot' version '2.7.18'; id 'java' }
apply plugin: 'io.spring.dependency-management'
sourceCompatibility = '1.8'; targetCompatibility = '1.8'
dependencies {
    implementation project(':game:lottery-engine')
    implementation project(':VinPlayDAL')
    implementation project(':VinPlayUserCore')
    implementation project(':VbeeCommon')
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
bootJar { enabled = true; mainClass = 'com.sunwinkr.lottery.api.LotteryApiApplication' }
```

### 1.4 Wire into `game-minigame` container
Spring app launched from BitZero `ServerReadyTask` on bg thread, feature-flag `LOTTERY_API_ENABLED=1`.

### 1.5 Directory layout
```
game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/
  clock/             LotteryClock (VN ZoneId), LotteryPhase
  ingest/            DrawIngest, DrawIngestResult, ScrapeClient (iface)
  bet/               BetValidator, BetAcceptResult, BetRequest, TicketNumberParser
  prize/             PrizeCalculator (10 modes), PrizeBreakdown
  settle/            LotterySettleService, SettleSummary
  port/              WalletPort, ResultStore, BetStore, ScrapeClient,
                     SettledFlagStore, TelegramAlertPort
  model/             LotteryMode, LotteryDraw, LotteryTicket, LotteryResult27/24

game/lottery-api/src/main/java/com/sunwinkr/lottery/api/
  LotteryApiApplication, config/, controller/, security/, push/,
  adapter/, scheduler/, dto/, wire/
```

## Section 2: Engine extraction — method-by-method

### 2.1 Round / day lifecycle

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| L1 | `LotteryModule.init` JLM:89-98 | `clock.LotteryClock` + `scheduler.DrawScheduler` | Spring `@Scheduled(cron="0 35 18 * * *", zone="Asia/Ho_Chi_Minh")`. Drop boot-time `getResultLottery()` (was unconditional re-scrape) — idempotent via `settled_at` | `DrawScheduleTest.cronIsVietnamTZ` | M |
| L2 | `LotteryModule.calculateInitialDelay` JLM:155-165 | DELETE | Replaced by cron | — | S |
| L3 | `LotteryModule.getResultLottery` JLM:100-142 | `ingest.DrawIngest#runOnce(LocalDate vnDate)` + `settle.LotterySettleService#settleAll(LotteryDraw)` | Split scrape, persist, settle. `System.out.println` (JLM:120) → SLF4J `debug` gated. LAST step `settledFlagStore.markSettled(vnDate)` | `IngestIdempotenceTest`, `SettleOrderTest` | L |
| L4 | `LotteryModule.handleClientRequest` cmd 30001 JLM:187-204 | `wire.LotteryModuleBridge#bet(User, DataCmd)` ~30 lines | `LotteryClock.isBettingOpen(clock, isSettleComplete)`. **No silent drop** — `ackBetRejected(reason)` returns explicit response | `BridgeRejectsDuringLockTest` | S |
| L5 | `handleServerEvent` JLM:173-185 | KEEP in shim | TaiXiu room property cleanup | — | S |
| L6 | `generateRandomString` JLM:144-153 | DELETE | Dead code | — | S |

### 2.2 Scrape / draw ingest

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| I1 | OkHttp call JLM:106-109 | `port.ScrapeClient` iface + `adapter.OkHttpScrapeClient` | URL env `LOTTERY_API_URL`. Connect/read 10s. Retry once on timeout | `ScrapeClientTimeoutTest` | S |
| I2 | Gson parse JLM:110-111 | `ingest.DrawJsonParser` | Field `ĐB` literal Unicode preserved (quirk #8) | `JsonParseTest.preservesDBUnicode` | S |
| I3 | `getLatestResult` dedupe JLM:112-118 | `port.ResultStore#findByDate(LocalDate vn)` | Parameterised `DATE(created_date) = ?`. Idempotent return | `DedupeOnSameDateTest` | S |
| I4 | `saveLotteryResult` JLM:115 → JLDao:168-180 | `port.ResultStore#save(jsonData, vnDate)` | Sets `settled_at = NULL`. `created_date` in VN TZ | `SaveResultSetsSettledNullTest` | S |
| I5 | NEW | `port.SettledFlagStore#markSettled(vnDate)` | UPDATE `result_lottery SET settled_at = NOW()`. Only AFTER settle loop completes | `MarkSettledOrderTest` | S |
| I6 | NEW | `ingest.DrawIngest#runOnce` orchestration | Order: scrape → dedupe → persist (`settled_at=NULL`) → settleAll → markSettled. Crash-resumable | `CrashMidSettleResumesTest` | M |

### 2.3 Bet acceptance

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| B1 | `LotteryModule.buyTicket` JLM:206-236 | `bet.BetAcceptor#accept(BetRequest)` | Error codes: `0000` OK, `0001` wallet, `0002` locked, `0003` insufficient, `0004` unknown_mode, `0005` invalid_num. **No silent drop** | `BetAcceptErrorOrderingTest` | M |
| B2 | `TextUtils.isEmpty(num)` JLM:208 + `findLotteryModeById` JLM:215 | `bet.BetValidator#validate` | Mode {1..9,11}, mode-specific shape. Per-mode in `TicketNumberParser` | `BetValidatorTest` per-mode | M |
| B3 | SUN-1295 snapshot JLM:215-220 | `bet.BetSnapshot.of(LotteryMode)` | Preserve `userBet * rate` math. Engine NEVER reads live enum during settle | `Sun1295SnapshotInvariantTest` | S |
| B4 | Wallet check JLM:222-227 | `port.WalletPort#debit(...)` | `"vin"` hard-coded per AMBIGUOUS #10 — preserve. Currency abstraction reserved for future | `WalletDebitTest` | S |
| B5 | `saveTransactionLode` JLM:229-232 | `port.BetStore#insert(LotteryTicket)` | Prepared-statement (DAL clean). Emits `BetAccepted` event | `BetStoreSnapshotTest` | S |
| B6 | `TelegramAlert.sendMessage` JLM:224 | `port.TelegramAlertPort#notifyBetPlaced(ticket)` | Ship hash(nickname) not raw (audit §5.6) | `TelegramRedactionTest` | S |
| B7 | LOCK CHECK JLM:193-196 (bug site) | `clock.LotteryClock#isBettingOpen(clock, todaySettleComplete)` | Lock 18:10 VN. Hold until `result_lottery.settled_at IS NOT NULL` for today | `LockWindowTest` (×5) | M |

### 2.4 Prize calculation

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| P1 | `LotteryModule.computePrize` JLM:284-369 | `prize.PrizeCalculator#calculate(LotteryDraw, LotteryTicket)` | Pure function. result27 / result24 / DB / mode / num / betValue / rate / prizeMul | `PrizeCalcUnitTest` (×10 modes) | L |
| P2 | Mode 1 JLM:293-297 | `#mode1` | `matches * betValue * prizeMul / rate` closed-form | `Mode1ClosedFormTest` | S |
| P3 | Mode 2 JLM:299-303 | `#mode2` | rs24 (excl G7) | `Mode2RS24Test` | S |
| P4 | Mode 3 JLM:305-314 | `#mode3` | Flat if 2/2 | `Mode3FlatPayoutTest` | S |
| P5 | Mode 4 JLM:316-325 | `#mode4` | Flat if 3/3 | `Mode4Test` | S |
| P6 | Mode 5 JLM:327-336 | `#mode5` | **CANONICAL = 4/4**. Replace `<3` → `<4`. Flag `LOTTERY_MODE5_LEGACY_3OF4` for rollback. TODO(SUN-MODE5-RECONCILE) | `Mode5_4of4_StrictTest` + `LegacyFlag3of4Test` | M |
| P7 | Modes 6/7 JLM:338-346 | `#mode6/7` | First/last char of `de` | `DauDuoiTest` | S |
| P8 | Mode 8 JLM:348-352 | `#mode8` | Legacy `/DUOI.getRate()=1` no-op. `// AMBIGUOUS #9` | `Mode8LegacyDivisorTest` | S |
| P9 | Mode 9 JLM:354-357 | `#mode9` | SUN-1295: 85 (not 95) | `Mode9Sun1295Test` | S |
| P10 | Mode 11 JLM:359-363 | `#mode11` | SUN-1295: 450 (not 900) | `Mode11Sun1295Test` | S |

### 2.5 Settle service

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| S1 | Settle loop JLM:121-132 | `settle.LotterySettleService#settleAll(LotteryDraw)` | Per-row try/catch. One failure doesn't halt loop. Emit `SettleFailureEvent` | `SettleAllTransactionTest` | M |
| S2 | `getRecordsWithNullPrizeBefore1830Today` JLDao:91-127 | `port.BetStore#findPendingForDate(vnDate, LotteryClock.LOCK_TIME)` | **TZ FIX**: replace `ZoneId.systemDefault()` (JLDao:100-101) with `LotteryClock.VN`. Window: `(yesterday 18:10, today 18:10)` — eliminates AMBIGUOUS #7 | `SettleWindowVietnamTZTest` | M |
| S3 | `updatePrize` JLDao:74-85 | `port.BetStore#markSettled(ticketId, prize)` | UPDATE adds `settled_at = NOW()` in same statement | `UpdatePrizeSetsSettledAtTest` | S |
| S4 | Wallet credit JLM:128-130 | `port.WalletPort#credit(...)` | Preserve `TransType.START_TRANS`. TODO(SUN-LOTTERY-TRANSTYPE): add LODE_WIN | `CreditTransTypeTest` | S |
| S5 | NEW | `settle.LotterySettleService#markDaySettled(vnDate)` | LAST call. `result_lottery.settled_at = NOW()` only after all bets processed | `MarkDaySettledAtomicTest` | S |

### 2.6 History / read APIs

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| H1 | `getRowsByNickname` JLDao:133-162 | `port.BetStore#findByUser(user, paging)` | Wraps existing DAL | `FindByUserTest` | S |
| H2 | `getListOfResultsByDateRange` JLDao:208-224 | `port.ResultStore#listSettled(from, to)` | **`WHERE settled_at IS NOT NULL`** — closes L-1 | `ListSettledOnlyTest` | S |
| H3 | `search/count` JLDao:227-294 | `port.BetStore#search(filter)` + `count(filter)` | **CRITICAL: replace string concat with PreparedStatement** for nickName/ticket/model/timeStart/timeEnd. Closes SQL injection | `SqlInjectionRejectedTest` (×5 fields) | M |
| H4 | `getLotteryTicket` | `port.BetStore#findPendingByDate(vnDate)` | Wraps after TZ fix | — | S |

## Section 3: TZ pinning (CRITICAL)

Every Vietnam-anchored decision routed through `LotteryClock.VN = ZoneId.of("Asia/Ho_Chi_Minh")`. **Global `.env:62 TZ=Asia/Seoul` MUST NOT change.**

| File:Line | Current | Replacement |
|-----------|---------|-------------|
| `LotteryModule.java:95-96` | `Calendar.getInstance() + scheduleAtFixedRate(18,35,…)` | Spring `@Scheduled(cron="0 35 18 * * *", zone="Asia/Ho_Chi_Minh")` |
| `LotteryModule.java:97` | `getResultLottery()` boot scrape | Conditional bootstrap: `if (resultStore.findByDate(LocalDate.now(VN)) == null) drawIngest.runOnce(...)` |
| `LotteryModule.java:121` | `LocalDateTime.now()` Telegram | `LocalDateTime.now(LotteryClock.VN)` |
| `LotteryModule.java:155-165` `calculateInitialDelay` | `Calendar.getInstance()` JVM default | DELETED |
| `LotteryModule.java:167-171` `getCurrentDateMillis` | `System.currentTimeMillis()` UTC | DELETED — `LotteryClock.VN.todayStart()` |
| `LotteryModule.java:193-195` | `LocalTime.now()` + (17:00, 19:00) | `LotteryClock.isBettingOpen(systemClock, todaySettleComplete)` |
| `LoDeDaoImpl.java:100-101` | `date.toInstant().atZone(ZoneId.systemDefault())` + (18,10)/(yesterday 18,59) | `date.toInstant().atZone(LotteryClock.VN)` + `LotteryClock.LOCK_TIME` |
| `LoDeDaoImpl.java:197-201` | MariaDB `DATEDIFF` server-side | Parameterised `DATE(created_date) = ?` VN-anchored |
| `LoDeDaoImpl.java:208-220` | `DATE_SUB(CURDATE(),…)` server TZ | `WHERE created_date >= ? AND settled_at IS NOT NULL` VN-anchored |

`LotteryClock` skeleton:
```java
public final class LotteryClock {
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalTime LOCK_TIME = LocalTime.of(18, 10);
    public static final Duration  POST_LOCK_HOLD = Duration.ofMinutes(45);

    public static boolean isBettingOpen(Clock clock, boolean todaySettleComplete) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(VN));
        LocalTime t = now.toLocalTime();
        if (!t.isBefore(LOCK_TIME) && t.isBefore(LOCK_TIME.plus(POST_LOCK_HOLD))) return false;
        if (!t.isBefore(LOCK_TIME) && !todaySettleComplete) return false;
        return true;
    }
}
```

## Section 4: `settled_at` flag

### 4.1 Migration SQL
```sql
ALTER TABLE vinplay_minigame.lode
    ADD COLUMN settled_at TIMESTAMP NULL AFTER prize;
ALTER TABLE vinplay_minigame.result_lottery
    ADD COLUMN settled_at TIMESTAMP NULL AFTER created_date;

UPDATE vinplay_minigame.lode
   SET settled_at = updated_date
 WHERE prize IS NOT NULL AND settled_at IS NULL;

UPDATE vinplay_minigame.result_lottery
   SET settled_at = DATE_ADD(created_date, INTERVAL 5 MINUTE)
 WHERE settled_at IS NULL;

CREATE INDEX idx_result_lottery_settled ON result_lottery (settled_at);
CREATE INDEX idx_lode_settled ON lode (settled_at);
```

### 4.2 Engine wiring
- `LotterySettleService.settleAll(draw)` final line: `settledFlagStore.markDraySettled(draw.date())`
- `LotteryClock.isBettingOpen` reads `settledFlagStore.isSettled(today)`
- REST `XsmbController.result(date)` → 404 if `settled_at IS NULL`
- REST `XsmbController.results(from,to)` → `WHERE settled_at IS NOT NULL`

### 4.3 Tests
- `betRejectedAfterScrapeUntilSettled`
- `resultEndpointReturns404PreSettle`
- `betOpenedAfterSettleComplete`
- `crashMidSettleResumesAndCompletes`
- `markDraySettledIsLastWrite`

## Section 5: Spring REST + STOMP

### 5.1 REST endpoints

| Method | Path | Auth | Body | Response |
|--------|------|------|------|----------|
| POST | `/api/v2/lottery/xsmb/bet` | bearer | `BetRequestDto` | `BetResponseDto` |
| GET | `/api/v2/lottery/xsmb/state` | bearer | — | `StateDto{phase, lockTime, todaySettled, lastSettledDate}` |
| GET | `/api/v2/lottery/xsmb/result/{date}` | bearer | — | 404 if `settled_at IS NULL` else `ResultDto` |
| GET | `/api/v2/lottery/xsmb/results?from=&to=` | bearer | — | `List<ResultDto>` filtered |
| GET | `/api/v2/lottery/xsmb/history?userId=` | bearer (caller==userId OR admin) | — | `HistoryDto` |
| GET | `/api/v2/lottery/products` | none | — | `[{code:"xsmb"}]` |
| POST | `/api/v2/lottery/admin/xsmb/settle` | role=LOTTERY_ADMIN | `{date}` | `{settledCount}` |
| POST | `/api/v2/lottery/admin/xsmb/rescrape` | role=LOTTERY_ADMIN | `{date}` | dual-control wrapped |
| GET | `/api/v2/lottery/admin/transactions` | role=LOTTERY_ADMIN | — | `SearchResult` (PreparedStatement) |

### 5.2 DTOs
```java
public record BetRequestDto(@Min(1)@Max(11) int mode,
                            @NotBlank @Pattern(regexp="[0-9,]{1,20}") String num,
                            @Min(1000) long betValue,
                            @NotBlank String clientNonce) {}

public record BetResponseDto(boolean success, String errorCode,
                             long currentMoney, Long ticketId, String message) {}
```

### 5.3 `AccessTokenFilter` — reuse Hazelcast `cacheToken` (same as TaiXiu plan)

### 5.4 STOMP — chat + announce only

| Topic | Payload | Trigger |
|-------|---------|---------|
| `/topic/lottery/xsmb/chat` | `{user, msg, ts}` | client SEND |
| `/topic/lottery/xsmb/announce` | `{type:"settled", date}` — **NO result fields** | `SettleAnnouncePublisher` on `markDraySettled` |
| `/topic/lottery/xsmb/lock` | `{type:"locked", lockTime}` | `PhaseLockPublisher` at 18:10 VN |

**Result fields NEVER traverse WS.** Clients react to `announce` by REST GET.

### 5.5 Idempotency
`clientNonce` → Hazelcast `lottery:bet:nonce:<user>` 10min TTL. Repeat → cached.

## Section 6: BitZero adapter

### 6.1 Shim shape (~30 lines)
```java
public class LotteryModule extends BaseClientRequestHandler {
    private LotteryModuleBridge bridge;

    public void init() {
        if (!"1".equals(System.getenv("LOTTERY_ENGINE_ENABLED"))) {
            LegacyLotteryModule.installInto(this);
            return;
        }
        this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
        bridge = LotteryApiApplication.contextHolder().getBean(LotteryModuleBridge.class);
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 30000: bridge.snapshot(user); break;
            case 30001: bridge.bet(user, new LotteryCmd(dataCmd)); break;
        }
    }
}
```

### 6.2 Wire-compat preserved
Cmd 30001 layout unchanged. Cmd 30000 (formerly empty break) returns snapshot.

## Section 7: Shadow + cutover

### Phase 1 — Build (W1-2)
- Engine + API compile. Legacy `LotteryModule` UNCHANGED.
- Ghost mode: Spring scheduler runs at 18:36 VN (1min after legacy), writes shadow tables `lode_shadow` / `result_lottery_shadow`. Flag `LOTTERY_ENGINE_GHOST_MODE=1`.

### Phase 2 — Diff (W3)
- `GhostDiffRunner` daily 03:00 KST. Per-bet prize compare; per-draw result compare.
- Tolerances: 0. Diff → Slack + `lottery_shadow_diff` row.
- Gate: **0 diffs for 14 consecutive days**.

### Phase 3 — Cutover (W4-5)
- `LOTTERY_ENGINE_ENABLED=1` flips cmd 30001.
- Legacy `getResultLottery` scheduler NoOp.
- Engine `DrawScheduler` authoritative.

### Phase 4 (future, ~6 months) — Option C unification
- Retire C# `LotoGame` + `ScanXskt`. Schema merge `loto_request` → `lode`. Out of scope.

### Rollback
- `LOTTERY_ENGINE_ENABLED=0` → legacy `LotteryModule.buyTicket`. No data migration; `settled_at` columns read-tolerant.

## Section 8: Test plan

### 8.1 Invariant JUnit (16 tests)

| Test class | Invariants |
|------------|-----------|
| `IngestSingletonInvariantTest` | INV-01, INV-02 |
| `BetLockInvariantTest` | INV-03 (CRITICAL: combined TZ + settle flag) |
| `Sun1295RateSnapshotTest` | INV-05 |
| `Mode1PayoutFormulaTest` | INV-06 |
| `Mode5StrictXien4Test` | INV-07 canonical 4/4 |
| `JavaWalletIsolationFromCSharpTest` | INV-14 cross-stack |
| `ScrapeFailureLeavesPendingTest` | INV-16 |

C# invariants (INV-04, 08, 09, 10-13, 15) deferred to Phase 4.

### 8.2 jqwik property tests
```java
@Property void mode1ClosedForm(@ForAll long userBet, @ForAll int matches) {
    long stored = userBet * 22L;
    long prize  = PrizeCalculator.applyMode1(stored, matches, 22, 80);
    assertThat(prize).isEqualTo(matches * userBet * 80L);
}

@Property void mode5Strict4of4OnlyWins(@ForAll List<String> n4, @ForAll List<String> rs27) {
    long prize = PrizeCalculator.mode5(rs27, n4, 1_000_000L, 1, 160);
    int matches = countMatches(n4, rs27);
    if (matches == 4) assertThat(prize).isEqualTo(160_000_000L);
    else              assertThat(prize).isZero();
}
```

### 8.3 Spring `@SpringBootTest`
- `BetEndpointMvcTest` — 200/`0000`, 400/`0004`, 401
- `ResultEndpointMvcTest` — 404 pre-settle, 200 post
- `HistoryEndpointMvcTest` — auth scope
- `AdminSettleMvcTest` — 403 non-admin, idempotent
- `StompChatTest` — chat round-trip; announce has NO result fields

### 8.4 Critical TZ tests
```java
@Test void schedulerCronAlwaysVietnam_evenIfJvmIsKorea() {
    System.setProperty("user.timezone", "Asia/Seoul");
    DrawScheduler s = new DrawScheduler(LotteryClock.VN, ingest);
    assertThat(s.nextFireZone()).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
    assertThat(s.nextFireWallClock()).isEqualTo("18:35");
}
```

### 8.5 Shadow-replay harness
- Last 1000 `lode` rows replayed through engine `PrizeCalculator`
- Byte-exact match required. CI nightly.

## Section 9: Risks + AMBIGUOUS handling

### 9.1 12 AMBIGUOUS items

| # | Item | Plan |
|---|------|------|
| 1 | Mode 5 Java 3/4 vs C# 4/4 | **RESOLVED → 4/4 canonical** (D2). Flag for rollback |
| 2 | No `referenceId` aggregate | **RESOLVED → flat session=YYYY-MM-DD VN** |
| 3 | 17-19 Java vs 18:10 C# | **RESOLVED → 18:10 lock + `settled_at` gate** |
| 4 | C# `break` vs `return false` | C#-only, out of scope |
| 5 | `allowModes/allowChannels` dead code | C#-only, out of scope |
| 6 | C# `LoTruotXien12` 10/12 bug | C#-only, out of scope |
| 7 | Java settle window 18:10-18:59 gap | **RESOLVED via TZ pin + `LotteryClock.LOCK_TIME`** |
| 8 | No refund on scrape failure | **PRESERVE.** Tickets stay `prize IS NULL`. Manual ops endpoint deferred |
| 9 | Mode 8 `/DUOI.getRate()=1` no-op | **PRESERVE** for byte-exact compat |
| 10 | Java money type `"vin"` fixed | **PRESERVE** but `WalletPort` accepts param |
| 11 | Two parallel stacks both debit | **PRESERVE Phase 1**. Engine extracts Java only |
| 12 | C# 36 channels, only Bắc used | C#-only, out of scope |

### 9.2 Anti-cheat findings disposition

| Finding | Severity | Disposition |
|---------|----------|-------------|
| TZ default `LocalTime.now()` | CRITICAL | **Fixed §3** — pin Vietnam at code |
| L-1: pre-settle result REST | HIGH | **Fixed §4** — `settled_at` flag + REST filter |
| L-2: settlement partial visibility | LOW | Operational; document |
| L-3: stdout println leak | LOW | **Fixed §2.1 L3** — SLF4J debug gated |
| SQL injection `search/count` | HIGH | **Fixed §2.6 H3** — PreparedStatement |
| Telegram nickname egress | MED | **Fixed §2.3 B6** — hash before send |
| `LotteryMode` setters | LOW | **Fixed §2.4** — Engine reads only snapshots |

### 9.3 Implementation risks
- HZ 3.12 lock-in (don't pull `starter-hazelcast`)
- JDK 8 target (Boot 2.7.18 supports)
- Dual scheduler during ghost phase (non-overlapping times)
- `result_lottery` row uniqueness — add `UNIQUE KEY uk_result_lottery_date (DATE(created_date))` after dedup verification

## Section 10: Timeline + PR plan

| Section | Engineer-days |
|---------|--------------|
| §1 Gradle + container | 1 |
| §2.1 Lifecycle | 2 |
| §2.2 Scrape ingest | 2 |
| §2.3 Bet acceptor | 3 |
| §2.4 PrizeCalculator (10 modes) | 3 |
| §2.5 Settle service | 2 |
| §2.6 History + SQL fix | 2 |
| §3 TZ pinning (8 sites) | 1 |
| §4 `settled_at` migration | 1 |
| §5 REST + STOMP | 3 |
| §6 BitZero bridge | 1 |
| §7 Ghost mode + diff | 2 |
| §8 Tests | 3 |
| **Subtotal** | **26d** |
| Buffer 30% | 8 |
| **Total** | **~34d ≈ 3.5 wk × 2 engineers** |

### PR sequencing

| PR | Scope | Days |
|----|-------|------|
| **PR-1** | Gradle `lottery-engine` + `lottery-api` skeletons; `LotteryClock` (VN ZoneId pinned); `LotteryPhase` enum; `LotteryMode` ported | 4 |
| **PR-2** | `BetValidator` + `PrizeCalculator` (10 modes, Mode 5 4/4 + legacy flag) + `DrawIngest` + `LotterySettleService`; `settled_at` migration; PreparedStatement SQL fix; 16 invariant tests | 14 |
| **PR-3** | Spring controllers + DTOs + `AccessTokenFilter`; STOMP `/chat` + `/announce` + `/lock`; `LotteryModuleBridge`; ghost-mode runner + diff cron; cutover flag; Spring MVC + STOMP tests | 16 |

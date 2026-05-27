# TaiXiu Engine Extraction Plan

**Status:** Plan-only. Behavior-preserving extraction from BitZero `TaiXiuModule` + `MGRoomTaiXiu` into two new Gradle subprojects: `minigame-engine` (pure Java) and `minigame-api` (Spring Boot 2.7.18).

**Source-of-truth specs:**
- `/root/sunwinkr/sunwinkr/docs/specs/taixiu-sicbo-rules-spec.md` (22 invariants, 12 AMBIGUOUS)
- `/root/sunwinkr/sunwinkr/docs/specs/taixiu-sicbo-anticheat-audit.md` (reveal hardening)

## Section 1: Gradle module setup

### 1.1 `settings.gradle` additions
```groovy
include 'game:minigame-engine'
include 'game:minigame-api'

project(':game:minigame-engine').projectDir = file('game/minigame-engine')
project(':game:minigame-api').projectDir    = file('game/minigame-api')
```

### 1.2 `game/minigame-engine/build.gradle`
```groovy
plugins { id 'java-library' }
group = 'com.sunwinkr'; version = '0.1.0-SNAPSHOT'
sourceCompatibility = '1.8'; targetCompatibility = '1.8'

repositories { mavenCentral(); mavenLocal() }

dependencies {
    // ZERO Spring, ZERO BitZero. Engine stays pure.
    implementation 'org.slf4j:slf4j-api:1.7.36'
    compileOnly  'com.google.code.findbugs:jsr305:3.0.2'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.assertj:assertj-core:3.24.2'
    testImplementation 'net.jqwik:jqwik:1.8.4'
}

test { useJUnitPlatform { includeEngines 'junit-jupiter', 'jqwik' } }
```

### 1.3 `game/minigame-api/build.gradle`
```groovy
plugins { id 'org.springframework.boot' version '2.7.18'; id 'java' }
apply plugin: 'io.spring.dependency-management'
group = 'com.sunwinkr'; version = '0.1.0-SNAPSHOT'
sourceCompatibility = '1.8'; targetCompatibility = '1.8'

repositories { mavenCentral(); mavenLocal() }

dependencies {
    implementation project(':game:minigame-engine')
    implementation project(':VinPlayDAL')
    implementation project(':VinPlayUserCore')
    implementation project(':vbee-common')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    // do NOT pull spring-boot-starter-hazelcast — repo pins HZ 3.12.

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

bootJar { enabled = true; mainClass = 'com.sunwinkr.minigame.api.MinigameApiApplication' }
```

### 1.4 Wire into existing `game-minigame` container
```groovy
// backend-master/game/Minigame/build.gradle
dependencies {
    implementation project(':game:minigame-engine')
    implementation project(':game:minigame-api')
}
```

`game-minigame` container boots BitZero as today; Spring app launched from BitZero's `ServerReadyTask` via `SpringApplication.run(MinigameApiApplication.class, args)` on background thread. Shared JVM + Hazelcast client. Feature flag `MINIGAME_API_ENABLED=1`.

### 1.5 Target directory layout

```
game/minigame-engine/src/main/java/com/sunwinkr/minigame/engine/
  core/                 TaiXiuRound, RevealPhase, RevealClock, RevealGuard
  bet/                  BetLedger, BetAcceptResult, BetRequest
  dice/                 DiceGenerator (iface), RandomDiceGenerator,
                        HouseEdgeDiceGenerator, ForcedDiceGenerator
  prize/                PrizeCalculator, SettleResult, RoundSnapshot
  jackpot/              JackpotPool, JackpotTriggerPolicy
  bot/                  BotPlanner, BotPlan, BotTaiXiuBet
  port/                 WalletPort, ForceResultStore, JackpotForcePort,
                        HistoryPublisher, BetRecorder, ConfigPort
  snapshot/             TaiXiuSnapshot, SnapshotBuilder
  rtp/                  RtpBalancer, RtpConfig

game/minigame-api/src/main/java/com/sunwinkr/minigame/api/
  MinigameApiApplication.java
  config/               StompConfig, SecurityConfig, EngineConfig
  controller/           TaiXiuController, AdminController
  security/             AccessTokenFilter, RoleResolver
  push/                 TickPublisher, RoundEventListener
  adapter/              JdbcWalletPort, HazelcastForceResultStore,
                        MongoBetRecorder, RmqHistoryPublisher
  dto/                  BetRequestDto, BetResponseDto, StateDto, HistoryDto,
                        ErrorResponseDto, ForceResultRequest
  wire/                 TaiXiuModuleBridge (BitZero ↔ engine adapter)
```

## Section 2: Engine extraction — method-by-method

### 2.1 Round lifecycle

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| L1 | `TaiXiuModule.gameLoop` 422-468 | `core.TaiXiuRound#tick()` + `RevealClock#advance()` | 1Hz tick. count→phase mapping: 0..44=OPEN, 45=LOCKED, 48=REFUND_CALC, 50=FINISH_FLAG, 51=GENERATING, 52=REVEALED (hardening split), 56=SETTLE, 60=BOT_PLAN, 68=NEW_ROUND | `RoundLifecycleTest.advancesThroughAllPhases` | M |
| L2 | `TaiXiuModule.startNewRoundTX` 339-374 | `core.TaiXiuRound#startNewRound(long newRefId)` | refId++ BEFORE broadcast (INV-1). HZ `allow_betting_*` write in try/catch (SUN-1xxx) | `RoundStartTest.refIdMonotonic` | S |
| L3 | `MGRoomTaiXiu.startNewGame` 171-206 | `core.TaiXiuRound#resetForNewRound(long)` | resultTX=null earliest (SUN-1246). `resetJp → jp=50M`. Pots renew. | `INV-16 potReset` | S |
| L4 | `MGRoomTaiXiu.disableBetting` 251-254 | `core.TaiXiuRound#lockBetting()` | phase=LOCKED, enableBetting=false, HZ `allow_betting_<ref>=0` | `BetLockTest.lockedRejectsBets` | S |
| L5 | `MGRoomTaiXiu.finish` 208-225 | `core.TaiXiuRound#finishRound()` | bettingRound=false; remove HZ keys. **Do NOT reset startTime** (SUN-1245) | `RoundFinishTest.startTimeUnchanged` | S |
| L6 | `MGRoomTaiXiu.getRemainTime` 274-310 | `core.RevealClock#remainTime()` | Time-based 50/18 split. `revealStart = startTime + 50_000ms`. **TODO(SUN-RH-LOCK)** — 6s lock window §3 | `RemainTimeTest.monotonicDecrease, noStuckAt33, noJumpBack` | M |
| L7 | HZ writes TXM:351-373 | `port.CachePort#setAllowBetting(refId, true)` | Per-call try/catch (SUN-1xxx) | `CacheWriteFailsBenignTest` | S |
| L8 | `TaiXiuModule.loadData` 208-217 | `core.TaiXiuBootstrap#load(MiniGamePort)` | refId default=1L; gameId=2; lichSuPhienTX cap 120 | `BootstrapTest.loadsRefAndHistory` | S |
| L9 | `TaiXiuModule.GameLoopWatchdog` 584-613 | `core.RoundWatchdog` | 90s threshold. API module: `@Scheduled(fixedRate=30000)` | `WatchdogTest.resurrectsAfterFreeze` | S |

### 2.2 Bet acceptance

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| B1 | `MGRoomTaiXiu.betTaiXiu` 388-503 | `bet.BetAcceptor#accept(BetRequest)` returns `BetAcceptResult{errorCode, currentMoney, perBetTxId, txDetail}` | All 5 error codes 1-5. Order: 4 → 3 → 5 → wallet → race re-check | `errorCodeOrderingTest`, `INV-4, INV-13` | L |
| B2 | TXR:429 perBetTxId | `bet.TxIdGenerator#nextBetTxId(refId)` | `refId * 1_000_000L + (nanoTime() & 0xFFFFFL)` (SUN-1290) | `INV-12, TxIdCollisionTest` (1M iters) | S |
| B3 | TXR:413 cross-side | `bet.BetAcceptor#checkCrossSide` | XOR: `potTai.userTotal(u)>0 ⊕ potXiu.userTotal(u)>0` | `INV-4` | S |
| B4 | TXR:431-435 bot wallet skip | `bet.BetAcceptor#shouldDebitWallet` | `!isBot \|\| isLivestream`. **TODO(SUN-1xxx): inconsistent w/ Sicbo — preserve** | `INV-22 livestreamIsolation` | S |
| B5 | TXR:437 mid-call disable refund | `bet.BetAcceptor#raceRecheck` | After successful debit, re-read enableBetting; if false → auto-refund via WalletPort.credit(..., "TaiXiuHoanTien", END_TRANS) with SAME perBetTxId | `BetRaceTest.midDebitDisableRefunds` | M |
| B6 | TXR:448-463 risk bias recorder | `bet.RiskBiasRecorder#record(bet, isBlack, isWhite)` | `n<percent` gate, moneyType==1, non-bot. Accumulates blackList*/whiteList* | `RiskBiasTest.percentageGate` | M |
| B7 | TXR:392-401 realPot | `bet.BetLedger#addReal(side, value, user)` | Non-bot path increments realPot*/realNumBet* first-bet | `RealPotTest.firstBetIncrementsCount` | S |
| B8 | `PotTaiXiu.bet` 32-50 | `bet.PotState#addContributor(TxDetail, isBot)` | Synchronized append; user dedup; bot/totalBot tracking | `PotStateTest.threadSafeAdd` | S |
| B9 | `MGRoomTaiXiu.insertUserBetToDb` 324-336 | `port.BetRecorder#recordBet(BetRecord)` | Mongo `user_bet_tai_xiu`. **No silent swallow** | `BetEventTest.eventFiredOnSuccess` | S |
| B10 | `refreshPadIfNeeded` 513-527 | `bet.FakePlayerPad#countsFor(refId)` | Per-round random `[30..60]`, jitter ±1. Cached by refId | `FakePlayerPadTest.cachedPerRoundCollisionsJittered` | S |

### 2.3 Dice generation + force-result

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| D1 | TXR:550-621 `getResult` | `dice.ResultPipeline#generate(RoundContext)` | Order: force-result → RTP balancer → jackpot override. Returns `short[3]`. **Stores into pendingDice, NOT broadcast** | `INV-2, INV-3, D1_OrderTest` | M |
| D2 | TXR:579 `suaKetQuaTaiXiu` | `port.ForceResultStore#peekAndConsume()` | Atomic `IMap.remove` on `ketquataixiu` | `ForceResultStoreTest.consumeOnce` | S |
| D3 | `GenerationTaiXiu.generateResultWithHouseEdge` 93-166 | `dice.HouseEdgeDiceGenerator#generate(potTai, potXiu, taxPct, userId)` | RtpResolver branch. Default-92-no-config → random. 5% imbalance floor. Both-negative → random | `HouseEdgeTest` (all 4 branches), `INV-18` | M |
| D4 | `TaiXiuUtil.generateResult` 71-81 / `generateDices` 175-184 | `dice.RandomDiceGenerator#generate()` + `ForcedDiceGenerator#forceUntilSide(side)` | `ThreadLocalRandom.nextInt(6)+1`. Force loop until total>10 matches forceSide. **No SecureRandom — preserve** | `RandomDiceTest.distribution`, `ForcedDiceTest.terminates` | S |
| D5 | TXR:594-615 jackpot override | `jackpot.JackpotTriggerPolicy#apply(dice, potTai, potXiu)` | `checkJackpot==6 && potTai.numBet%5==0` → triple Tài; `==1 && potXiu.numBet%5==0` → triple Xỉu | `INV-10, JackpotTriggerTest.gatedBy5Modulo` | M |
| D6 | TXM:476-491 `generateTaiXiuDices` | `core.TaiXiuRound#revealDices()` | total>10 ? TAI : XIU. VIN-room generates, XU receives via `setResult(dice, side)` | `TwoRoomDiceTest.sharedResult` | S |
| D7 | TXM:281 force-result stdout | `core.RevealGuard.traceDice(phase, dice, msg)` | Throws if phase ∉ {REVEALED, SETTLED}. Replaces TXM:487, TXR:618, TXM:281-282 | `RevealGuardTest.throwsBeforeReveal` | S |
| D8 | TXM:258-286 superadmin substring auth | `api.controller.AdminController` + `security.RoleResolver` | **Replace** `name.contains("superadmin")` with explicit role lookup. Engine knows nothing about auth | `AdminControllerTest.rejectsImposters` | M |

### 2.4 Prize calc + cross-pot balance

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| P1 | TXR:553 tongTienHopLe | `prize.CrossPotBalancer#legalAmount(potTai, potXiu)` | `min(potTai.total, potXiu.total)` | `INV-5` | S |
| P2 | TXR:559-567 contributor allocation | `prize.CrossPotBalancer#allocate(pot, hopLe)` | Insertion order. Running sum; last contributor often partial | `INV-6, AllocationOrderTest` | M |
| P3 | TXR:825-984 `calculatePrize` switch on result | `prize.PrizeCalculator#calculate(roundSnap)` | Two branches (result=0/1). Winning formula: `prize = (tienDuocTinh*(100-tax)/100) + tienDuocTinh`. **balanceGate quirk:** true → full bet | `INV-7, BalanceGateTest` | L |
| P4 | TXR:861/885/909/933 balanceGate | `prize.PrizeCalculator#applyBalanceGate` | **TODO(SUN-BAL-INV): inverted at TXR:665 — preserve** | `BalanceGateBypassTest` | S |
| P5 | TXR:986-1009 updateSumTran | `prize.UserAggregator#aggregate(detail)` | Multi-bet same-side merge | `MultiBetAggregationTest` | S |
| P6 | TXR:956-959 bot exclusion | `prize.SettleResult#summary()` | `rs.totalTai = pot.totalValue - pot.totalBotBet` | `BotExcludedFromTotalsTest` | S |
| P7 | TXR:961-969 parallel UpdateMoneyTXTask | `port.SettleExecutor#submitParallel(sumTai, sumXiu)` | Engine builds SettleResult; adapter dispatches to 2 threads | adapter test only | S |

### 2.5 Jackpot

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| J1 | TXR:558 +0.6% accumulator | `jackpot.JackpotPool#accumulate(losingPotTotal)` | `jp = jp + (long)(losingPot * 0.006)`. Floor 50M VIN | `INV-10, Jp006Test` | S |
| J2 | TXR:727-823 calculateJackpot | `jackpot.JackpotDistributor#distribute(round)` | `jp_share = tienDuocTinh / sum(tienDuocTinh) × jp` | `INV-11, integer-truncation-drift` | M |
| J3 | TXR:813-820 sendNotifyJp bot-skip | `jackpot.JackpotDistributor#notifyWinners` | Skip when isBot | `JpNotifyBotSkipTest` | S |
| J4 | TXR:1234-1259 jp_tx Mongo singleton | `port.JackpotStatePort#read/write` | Adapter wraps `jackpot_tx.jackpotTX` | adapter test | S |

### 2.6 Settlement

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| S1 | TXR:1265-1373 UpdateMoneyTXTask | `adapter.JdbcWalletPort#applySettlement(SettleResult)` | I/O in adapter. Per user: jp IN_TRANS, main IN-or-END, refund END_TRANS. Single refId reused | `SettleOrderTest` | M |
| S2 | TXR:1295 all-lost branch | `WalletPort#noopCloseTxn(user, refId)` | `updateMoney(0, ..., END_TRANS)` | `AllLostClosesTxnTest` | S |
| S3 | TXR:1318 fee | `prize.FeeCalc#fee(totalPrize, tax)` | `(long)(tax * totalPrize / (200 - tax))` | `FeeFormulaTest` | S |
| S4 | TXR:1350 swallowed wallet failure | `port.WalletPort` contract | **TODO(SUN-WAL-FAIL):** preserve swallow; emit `SettleFailureEvent` for future retry | `WalletFailureEventTest` | S |
| S5 | TXR:1307/1333 broadcast | `port.BroadcastPort#notifyWin(user, amount, gameId)` | Wraps `BroadcastMessageServiceImpl.putMessage`. MIN_MONEY threshold | adapter | S |

### 2.7 Bots

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| BT1 | `BotMinigame.getBotTaiXiu` 292-406 | `bot.BotPlanner#planTaiXiu(moneyType, hour)` | numBetTai/Xiu independent. Collision jitter ±1. **Block-split**: first numBetTai bots → side=1 | `BotPlanTaiXiuTest.blockSplit` | M |
| BT2 | `ratioTXInNight` 659-681 | `bot.NightRatioPolicy#ratioPct(hour)` | 02:00-08:00 30-100%. Default 100 | `NightRatioTest.tableLookup` | S |
| BT3 | `randomBettingTime` 549-557 | `bot.BotPlanner#pickBettingTime` | `phanTramVaoSom = {60,70,80,85,90}` random | `BettingTimeDistTest` | S |
| BT4 | TXM:390-412 botBet 15s window | `bot.BotDispatcher#tick(currentRemainTime, plan)` | At remainTime≤15: jackpot side mod-5 skip. Alternate side | `BotDispatchTest` | M |

### 2.8 History + RTP balancer hook

| # | Source | Target | Quirks | Tests | Cx |
|---|--------|--------|--------|-------|----|
| H1 | TXM:488-491 lichSuPhienTX | `core.TaiXiuHistory#push(ResultTaiXiu)` | Cap 120, oldest evicted | `HistoryCapTest` | S |
| H2 | TXM:494-499 getLichSuPhienTX | `core.TaiXiuHistory#last(int n)` | Last n entries | `HistoryLastNTest` | S |
| H3 | TXR:1011-1106 calculateBalanceTX | `rtp.RtpBalancerHook#shouldForce(type)` | All 4 type modes. **Dead code today — preserve TODO** | `RtpBalancerHookTest` | M |

## Section 3: Reveal hardening

### 3.1 RevealPhase enum
```java
public enum RevealPhase {
    OPEN, LOCKED, GENERATING, REVEALED, SETTLED, CLEANUP;

    public boolean diceVisible() { return this == REVEALED || this == SETTLED; }
    public boolean acceptsBets() { return this == OPEN; }
}
```

Legal transitions (asserted in `RevealClock`):
```
OPEN→LOCKED, LOCKED→GENERATING, GENERATING→REVEALED,
REVEALED→SETTLED, SETTLED→CLEANUP, CLEANUP→OPEN
```

### 3.2 Tick → phase mapping (6s lock window)

```
count: 0..44 OPEN
       45     → LOCKED      (disableBetting; 6s lock window starts)
       48     → refund-calc (still LOCKED)
       50     → finish-flag (still LOCKED)
       51     → GENERATING  (generate dice → pendingDice; NOT broadcast)
       52     → REVEALED    (broadcast dice; +1s gap)
       56     → SETTLED     (calculatePrize)
       60     → CLEANUP_BOTS
       68     → OPEN
```

Lock window = 6 ticks @ 1Hz = 6s ✓. 51→52 adds explicit 1-tick gap. Round still 68s total.

### 3.3 `pendingDice` + snapshot censoring
```java
class TaiXiuRound {
    private volatile RevealPhase phase = RevealPhase.OPEN;
    private volatile short[] pendingDice;
    private volatile short[] revealedDice;

    void generateDicesLocked() {
        require(phase == RevealPhase.GENERATING);
        pendingDice = resultPipeline.generate(this);
    }

    void revealDices() {
        require(phase == RevealPhase.REVEALED);
        if (pendingDice == null) throw new IllegalStateException("reveal without dice");
        revealedDice = pendingDice;
        history.push(buildResultTaiXiu(revealedDice));
    }

    public TaiXiuSnapshot snapshotForClient(String username) {
        TaiXiuSnapshot s = new TaiXiuSnapshot();
        s.remainTime  = clock.remainTime();
        s.bettingState = phase.acceptsBets();
        s.potTai      = potTai.totalValue();
        s.potXiu      = potXiu.totalValue();
        s.myBetTai    = potTai.totalByUser(username);
        s.myBetXiu    = potXiu.totalByUser(username);
        s.jpTai = s.jpXiu = jackpotPool.value();
        if (phase.diceVisible() && revealedDice != null) {
            s.dice1 = revealedDice[0]; s.dice2 = revealedDice[1]; s.dice3 = revealedDice[2];
            s.result = (short)(revealedDice[0]+revealedDice[1]+revealedDice[2] > 10 ? 1 : 0);
        } else {
            s.dice1 = s.dice2 = s.dice3 = 0;
            s.result = (short) -1;
        }
        return s;
    }
}
```

### 3.4 RevealGuard wrap sites
| Site | File:Line | Replacement |
|---|---|---|
| 1 | `TaiXiuModule.java:487` | `RevealGuard.traceDice(round.phase(), dice, "GENERATE RESULT")` |
| 2 | `MGRoomTaiXiu.java:618` | `RevealGuard.traceDice(round.phase(), result, "Result End")` |
| 3 | `TaiXiuModule.java:281-282` `System.out.println("ForceResultTaiXiu...")` | `RevealGuard.adminTrace(role, dice, "ForceResult")` |

### 3.5 Property test for snapshot
```java
@Property
void noDicePreReveal(@ForAll RevealPhase phase, @ForAll @StringLength(min=1,max=30) String u) {
    Assume.that(phase != RevealPhase.REVEALED && phase != RevealPhase.SETTLED);
    TaiXiuRound r = newRoundAt(phase);
    r.generateDicesIfPossible();
    TaiXiuSnapshot s = r.snapshotForClient(u);
    assertThat(s.dice1).isZero();
    assertThat(s.dice2).isZero();
    assertThat(s.dice3).isZero();
    assertThat(s.result).isEqualTo((short) -1);
}
```

### 3.6 Force-result auth hardening
- BitZero command 2003 player-socket handler → `return;` (deprecated)
- Force-result → `AdminController.POST /api/v2/admin/taixiu/force-result`
- `AccessTokenFilter` validates; `SecurityConfig` requires role `MINIGAME_ADMIN` (from `UserCacheModel.role`, NOT name substring)
- Engine port `ForceResultStore.set(short[3])` — no auth in engine

## Section 4: Adapters

### 4.1 `JdbcWalletPort` — wraps `UserServiceImpl.updateMoney`
```java
@Component
public class JdbcWalletPort implements WalletPort {
    private final UserService userService;

    @Override public MoneyResult debit(String user, long amount, String moneyType,
                                       String source, long gameId, String desc,
                                       long fee, long txId, TransKind kind) {
        MoneyResponse res = userService.updateMoney(user, -amount, moneyType, source,
            String.valueOf(gameId), desc, fee, txId, mapTransType(kind));
        return new MoneyResult(res.isSuccess(), res.getCurrentMoney(), res.getErrorCode());
    }
    // credit() mirrors with positive amount
}
```

### 4.2 `HazelcastForceResultStore`
```java
@Component
public class HazelcastForceResultStore implements ForceResultStore {
    @Override public Optional<short[]> peekAndConsume() {
        IMap<String, short[]> map = HazelcastClientFactory.getInstance().getMap("ketquataixiu");
        return Optional.ofNullable((short[]) map.remove("ketquataixiu"));
    }
    @Override public void set(short[] dice) {
        HazelcastClientFactory.getInstance().getMap("ketquataixiu").put("ketquataixiu", dice);
    }
}
```

### 4.3 `MongoBetRecorder`
```java
@Component
public class MongoBetRecorder implements BetRecorder {
    @Override public void recordBet(BetRecord r) {
        Document doc = new Document()
            .append("referentId", r.refId).append("nick_name", r.nickname)
            .append("inputTime", r.inputTime).append("betSide", r.betSide)
            .append("betValue", r.betValue).append("balance", r.balance)
            .append("money_type", r.moneyType == 1 ? 1 : 2);
        MongoDBConnectionFactory.getDB().getCollection("user_bet_tai_xiu").insertOne(doc);
    }
    @Override public void clearForNewRound() {
        MongoRetryHelper.run(() ->
            MongoDBConnectionFactory.getDB().getCollection("user_bet_tai_xiu").drop(),
            "taixiu.clearUserBetToDb");
    }
}
```

### 4.4 `RmqHistoryPublisher`
```java
@Component
public class RmqHistoryPublisher implements HistoryPublisher {
    @Override public void publishTransaction(TransactionTaiXiuMessage msg)
        { MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", msg, 100); }
    @Override public void publishResult(ResultTaiXiuMessage msg)
        { MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", msg, 101); }
    @Override public void publishTransactionDetail(TransactionTaiXiuDetailMessage msg)
        { MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", msg, 102); }
}
```

### 4.5 `BotPlanner`
Move `getBotTaiXiu`, `getVipBotTaiXiu`, `ratioTXInNight`, `randomBettingTime` from `BotMinigame` into engine `bot/`. Adapter `BotRegistryPort` for `ConfigGame.getIntValue("tx_min_bot_betting_vin", ...)` and bot-nickname source.

## Section 5: Spring REST + STOMP API

### 5.1 Endpoints

| Method | Path | Auth | Body | Response |
|--------|------|------|------|----------|
| POST | `/api/v2/taixiu/join` | bearer | `{moneyType}` | StateDto |
| POST | `/api/v2/taixiu/leave` | bearer | `{moneyType}` | `{success}` |
| POST | `/api/v2/taixiu/bet` | bearer | BetRequestDto | BetResponseDto |
| GET | `/api/v2/taixiu/state?moneyType=1` | bearer | — | StateDto |
| GET | `/api/v2/taixiu/history?moneyType=1&n=100` | bearer | — | HistoryDto |
| POST | `/api/v2/admin/taixiu/force-result` | role=ADMIN | `{side:0\|1}` | `{success}` |
| POST | `/api/v2/admin/taixiu/kill-switch` | role=ADMIN | `{enabled}` | `{success}` |
| GET | `/api/v2/admin/taixiu/round-state` | role=ADMIN | — | diagnostics |

### 5.2 DTOs
```java
public record BetRequestDto(short moneyType, long betValue, short betSide, String clientNonce) {}
public record BetResponseDto(boolean success, String errorCode, long currentMoney,
                             long perBetTxId, String message) {}
public record StateDto(long referenceId, short remainTime, boolean bettingState,
                       long potTai, long potXiu, long myBetTai, long myBetXiu,
                       long jpTai, long jpXiu,
                       short dice1, short dice2, short dice3, short result,
                       short numBetTai, short numBetXiu,
                       short realNumBetTai, short realNumBetXiu) {}
```

### 5.3 `AccessTokenFilter`
```java
@Component
public class AccessTokenFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain ch) {
        String at = headerOrQuery(req, "Authorization", "Bearer ", "at");
        if (at == null) { ch.doFilter(req, res); return; }
        IMap<String, TokenInfo> cache = HazelcastClientFactory.getInstance().getMap("cacheToken");
        TokenInfo info = cache.get(at);
        if (info == null) { res.setStatus(401); return; }
        Authentication auth = new UsernamePasswordAuthenticationToken(
            info.nickname, null, resolveAuthorities(info));
        SecurityContextHolder.getContext().setAuthentication(auth);
        ch.doFilter(req, res);
    }
}
```

### 5.4 STOMP broker
```java
@Configuration @EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic");
        r.setApplicationDestinationPrefixes("/app");
    }
    public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws/minigame").setAllowedOriginPatterns("*");
    }
}
```

Topics:
- `/topic/taixiu/{moneyType}/tick` — per-second pots/remainTime (no dice)
- `/topic/taixiu/{moneyType}/reveal` — dice payload on REVEALED transition
- `/topic/taixiu/{moneyType}/pot` — pot deltas on bet
- `/topic/taixiu/{moneyType}/round-start` — new round event

### 5.5 Error response shape
```json
{"success": false, "errorCode": "0004", "message": "Bet below minimum", "perBetTxId": null}
```

| code | meaning |
|------|---------|
| 0000 | OK |
| 0001 | Wallet failure / race-disabled |
| 0002 | Betting closed |
| 0003 | Insufficient balance |
| 0004 | Below MIN(100) |
| 0005 | Cross-side bet not allowed |
| 0401 | Unauthorized |
| 0403 | Forbidden (role) |
| 0429 | Rate limit |

### 5.6 Idempotency
`clientNonce` → Hazelcast `taixiu:bet:nonce:<user>` 5min TTL. Repeat → return cached BetResponseDto.

## Section 6: BitZero adapter

### 6.1 Bridge shape (~50 lines)
```java
public class TaiXiuModule extends BaseClientRequestHandler {
    private TaiXiuModuleBridge bridge;

    public void init() {
        bridge = new TaiXiuModuleBridge(getRoom(...), engine.getTaiXiuFacade());
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 2000: bridge.subscribe(user, new SubcribeMinigameCmd(dataCmd)); break;
            case 2001: bridge.unsubscribe(user, new UnsubscribeMiniGameCmd(dataCmd)); break;
            case 2002: bridge.changeRoom(user, new ChangeRoomMinigameCmd(dataCmd)); break;
            case 2110: bridge.bet(user, new BetTaiXiuCmd(dataCmd)); break;
            case 2116: bridge.history(user); break;
            case 2003: /* deprecated: force-result removed from player socket */ break;
        }
    }
}
```

### 6.2 Bridge bet flow
```java
public void bet(User user, BetTaiXiuCmd cmd) {
    BetRequest req = BetRequest.of(user.getName(), cmd.userId, cmd.betValue,
        cmd.inputTime, cmd.moneyType, cmd.betSide, false);
    BetAcceptResult r = engineFacade.acceptBet(req);
    BetTaiXiuMsg msg = new BetTaiXiuMsg();
    msg.Error = (byte) r.errorCode();
    msg.currentMoney = r.currentMoney();
    sendMessageToUser(msg, user);
}
```

### 6.3 Wire protocol unchanged
All `BaseMsg` subclasses (`UpdateResultDicesMsg`, `UpdateTaiXiuPerSecondMsg`, `BetTaiXiuMsg`, `TaiXiuInfoMsg`, `TaiXiuRefundMsg`, `UpdatePrizeTaiXiuMsg`, `BroadcastTXTimeMsg`, `LichSuPhienMsg`, `StartNewGameTaiXiuMsg`, `TaiXiuJackpotMsg`) keep field layouts. Bridge builds from engine snapshots.

### 6.4 Event subscription
Engine emits typed events (`BetAccepted`, `RoundLocked`, `DiceRevealed`, `SettleComplete`, `RefundComputed`, `JackpotTriggered`). Bridge registers listener → translates each → BitZero `BaseMsg` → broadcasts.

## Section 7: Shadow + cutover

### Phase 1 — Build (W1-2)
- Engine + adapters compile. BitZero `TaiXiuModule` UNCHANGED (still authoritative).
- Engine ghost mode: hooked into BitZero event stream, computes own SettleResult.
- Outputs → Mongo `shadow_user_bet_tai_xiu` + `shadow_settle_results`.
- Flag: `MINIGAME_ENGINE_GHOST_MODE=1` (default 0).

### Phase 2 — Diff (W3)
- Spring `@Scheduled` daily 03:00 KST cron `shadow-diff`:
  - `result_tai_xiu` vs `shadow_settle_results.totals`
  - `transaction_tai_xiu_detail` vs `shadow_settle_results.details`
  - prize/refund/jp per user
- Any diff → Slack + `shadow_diff_report` row.
- Cutover gate: **0 diffs for 14 days**.

### Phase 3 — Cutover (W4-5)
- `MINIGAME_ENGINE_ENABLED=1` flips ownership.
- BitZero `TaiXiuModule` becomes 50-line bridge.
- Spring `TaiXiuRevealClock` authoritative; BitZero `gameLoopTask` no-op (kept 1 release for rollback).

### Rollback
- `MINIGAME_ENGINE_ENABLED=0` → legacy gameLoop resumes. No data migration.
- Watchdog auto-disables engine if `lastTickMs > 90s` AND flag true.

## Section 8: Test plan

### 8.1 Invariant JUnit (15 tests for TaiXiu vertical)
| Test class | Invariants |
|------------|-----------|
| `RoundLifecycleInvariantTest` | INV-1, INV-2, INV-17 |
| `ForceResultInvariantTest` | INV-3 |
| `BetAcceptanceInvariantTest` | INV-4, INV-13, INV-22 |
| `CrossPotBalanceInvariantTest` | INV-5, INV-6 |
| `PrizeFormulaInvariantTest` | INV-7 |
| `JackpotInvariantTest` | INV-10, INV-11 |
| `TxIdInvariantTest` | INV-12 |
| `PotResetInvariantTest` | INV-16 |
| `RtpFeatureGateTest` | INV-18 |
| `BetHistoryInvariantTest` | INV-20 |

Plus reveal hardening:
- `RevealPhaseTransitionTest` — illegal transitions throw
- `NoDiceInSnapshotPreRevealTest` (jqwik property)
- `RevealGuardTest` — pre-reveal log throws
- `LockWindow6sTest` — bet at count=44 accepted, count=45 rejected

### 8.2 jqwik property tests
```java
@Property void hopLeMatchesMinPot(@ForAll List<Long> taiBets, @ForAll List<Long> xiuBets) {
    long hopLe = CrossPotBalancer.legalAmount(potOf(taiBets), potOf(xiuBets));
    assertThat(hopLe).isEqualTo(Math.min(sum(taiBets), sum(xiuBets)));
}

@Property void prizeFormulaTaxConserved(@ForAll long bet, @ForAll @FloatRange(min=0, max=10) float tax) {
    long prize = PrizeCalculator.winningPrize(bet, tax);
    assertThat(prize).isEqualTo((long)(bet * (100 - tax) / 100) + bet);
}
```

### 8.3 Spring `@SpringBootTest`
| Test | Endpoint | Asserts |
|------|----------|---------|
| `BetEndpointMvcTest` | POST `/bet` | 200+0000; 400+0004; 401 missing token |
| `StateEndpointMvcTest` | GET `/state` | snapshot consistency; dice=0 pre-reveal |
| `HistoryEndpointMvcTest` | GET `/history` | last n; cap 120 |
| `AdminForceResultMvcTest` | POST `/admin/force-result` | 403 non-admin; 200 admin; HZ map populated |
| `StompTickTest` | WS subscribe | tick frames 1Hz; no dice pre-reveal |

### 8.4 Shadow-replay harness
- Reads N rounds from `result_tai_xiu` + `transaction_tai_xiu_detail`.
- Replays bets through engine in timing order.
- Compares engine `SettleResult` vs legacy stored.
- Pass: byte-exact match on prize/refund/jp per user per round.
- CI nightly: 1000 most recent rounds.

## Section 9: Risks + AMBIGUOUS handling

### 9.1 12 AMBIGUOUS items
| # | Item | Plan |
|---|------|------|
| 1 | Sicbo refId gameId 30/31 | Sicbo-only |
| 2 | Sicbo force-result map name | **RESOLVED — not a bug** (`ketquataixiusicbo` on both sides) |
| 3 | Sicbo cross-side dead code | Sicbo-only |
| 4 | Sicbo fundTaiXiu inf-loop | Sicbo-only |
| 5 | `MINIGAME_TAX_TX_JACKPOT` unused | Preserve unused. TODO(SUN-TAX-JP). |
| 6 | TaiXiu bots skip / Sicbo bots debit | Preserve. `BetAcceptor.shouldDebitWallet` returns `!isBot \|\| isLivestream` for TaiXiu |
| 7 | Settle wallet failure swallow | Preserve swallow + emit `SettleFailureEvent`. Adapter logs WARN |
| 8 | `TaiXiuUtil.generateResult` recursive no-op | Not on active path; leave |
| 9 | Missing referenced doc | N/A — this spec replaces |
| 10 | balanceGate inverted at TXR:665 | Preserve. TODO(SUN-BAL-INV) |
| 11 | Refund msg/money desync 48 vs 56 | Preserve. RefundMsg event fires at count=48, wallet credit at count=56 |
| 12 | `MGRoomSicbo.calculateMoneyReturn` dead | Sicbo-only |

### 9.2 Anti-cheat AMBIGUOUS
| # | Item | Verification |
|---|------|--------------|
| A1 | `UpdateTaiXiuPerSecondMsg` field shape | Read class; safe-by-construction (TXR:529-548 does NOT assign dice). Add byte-level JUnit |
| A2 | `allow_betting_*` HZ reader | `grep -rn 'allow_betting' backend-master/`. If reader exists, engine must keep adapter writes |
| A3 | `forceResultTaiXiu` cmd 2003 reachability | Confirm BitZero cmd 2003 on player socket vs admin. If player → delete handler in bridge |

### 9.3 Implementation risks
- **HZ 3.12 lock-in.** Spring Boot 2.7 wants HZ 5.x. Do NOT pull `spring-boot-starter-hazelcast`. Manual `@Bean HazelcastInstance` from `HazelcastClientFactory.getInstance()`.
- **JDK 8 target.** Boot 2.7.18 supports JDK 8. Verify `bootJar` on existing JRE 8 image.
- **Thread-pool collision.** Engine `@Scheduled` + BitZero `taskScheduler` co-exist during ghost mode. Feature flag exclusivity mandatory.
- **HZ `cacheToken` schema** — STOMP/REST auth depends on existing token format. Confirm field names match portal-api.
- **Shadow storage cost.** `shadow_settle_results` ~1MB/day per game. TTL index 30 days.

## Section 10: Timeline

| Section | Engineer-days |
|---------|--------------|
| §1 Gradle setup | 1 |
| §2.1 Round lifecycle | 3 |
| §2.2 Bet acceptance | 4 |
| §2.3 Dice gen + force | 2 |
| §2.4 Prize + cross-pot | 4 |
| §2.5 Jackpot | 1 |
| §2.6 Settlement adapter | 2 |
| §2.7 Bots | 2 |
| §2.8 History + RTP | 1 |
| §3 Reveal hardening | 2 |
| §4 Adapters | 2 |
| §5 REST + STOMP API | 3 |
| §6 BitZero bridge | 1 |
| §7 Shadow mode | 2 |
| §8 Tests | 3 |
| **Subtotal** | **33d** |
| Buffer 25% | 8 |
| **Total** | **~41d ≈ 4 wk × 2 engineers** |

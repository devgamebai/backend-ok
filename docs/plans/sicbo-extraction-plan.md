# Sicbo Extraction Plan

Counterpart to `taixiu-extraction-plan.md`. Inherits shared module skeleton; this document specifies Sicbo-specific deltas. Behavior-preserving; legacy quirks replicated with `// TODO(SUN-xxxx)` markers.

## Resolved AMBIGUOUS up front

**#2 (force-result map name)** — VERIFIED 2026-05-14. `TaiXiuSicboServiceImpl.suaKetQuaTaiXiu()` (DAL-SB:370-378) reads `ketquataixiusicbo`. `SicboModule.forceResultTaiXiu` (SBM:296-297) writes `ketquataixiusicbo`. **Force-result works correctly today.** "TaiXiu" in class name + method name is misleading but harmless. JavaDoc clarification only. No follow-up ticket.

## 1. Gradle — defer to TaiXiu plan

Shared `minigame-engine` and `minigame-api` modules. Sicbo-only additions:
- Engine package `engine.sicbo`
- API controllers `api.sicbo`
- Tests under `minigame-engine/src/test/java/engine/sicbo/`

## 2. Engine extraction — method-by-method

### 2.1 Sicbo-specific engine classes (13 total)

| # | Class | Mirrors / replaces |
|---|---|---|
| 1 | `SicboRound` | `MGRoomSicbo` round state — lifecycle + RevealPhase machine |
| 2 | `SicboBetType` (enum) | `PotSicbo` (PS:7-58) — 52 entries: id, name, rotation, isOneDiceSpecial |
| 3 | `SicboBet` | `TransactionTaiXiuDetail` (Sicbo subset) — per-bet record + transactionCode |
| 4 | `SicboDiceGenerator` (iface) | `TaiXiuUtil.generateRandomResult` — pluggable RNG |
| 5 | `SicboRtpDiceGenerator` | `MGRoomSicbo.generateResultWithHouseEdge` (SBR:732-776) — 216-combo brute-force |
| 6 | `SicboPrizeCalculator` | `MGRoomSicbo.reward` + `sotienphaitra` (SBR:1146-1255) |
| 7 | `SicboWinningStatusEvaluator` | `getWinningStatuses` (SBR:1090-1144) — pure function |
| 8 | `SicboFundProtector` | SBR:707-721 fallback while-loop — bounded retry |
| 9 | `SicboBotPlanner` | `BotMinigame.getBotSicbo` + distribution (BM:408-491) — random-per-bet |
| 10 | `SicboHistory` | `lichSuPhienTX` (SBM:134, 497-500) — 120-cap ring buffer |
| 11 | `SicboForceResultStore` | `suaKetQuaTaiXiu` HZ IMap — single-use forced dice |
| 12 | `SicboSnapshot` | `SicboInfoMsg` builder (SBR:991-1011) — censoring snapshot |
| 13 | `SicboReferenceIdStore` | `mgService.getReferenceId(30)` / `saveReferenceId(31)` — preserve gameId quirk |

### 2.2 Round lifecycle (55s) — `SicboRound`

| Engine method | Maps to | Notes |
|---|---|---|
| `startNewRound(refId)` | `MGRoomSicbo.startNewGame` (SBR:217-243) | Resets `listResult`, `listUserBet`, `bettingRound=true`, `enableBetting=true`, `startTime`, `totalValueBetUser=0`. `clearUserBetToDb` async via port. **AMBIGUOUS #5: preserve init order** (tax → 0.0f init → ctor-set) |
| `tick(int count)` | `SicboModule.gameLoop` (SBM:417-477) | `count==40` → disableBetting; `==43` → finish; `==44` → generate; `==48` → reward; `==53` → bot schedule; `==55` → new round. Driven by RevealClock |
| `disableBetting()` | SBR:310-322 | Sets enableBetting=false + bettingRound=false (2026-05-08). HZ `allow_betting_<refId>=0`. Phase OPEN→LOCKED |
| `finish()` | SBR:245-268 | Clears resultTX, bettingRound=false. Removes HZ keys. **Does NOT reset startTime** (2026-05-08) |
| `getRemainTime()` | SBR:341-392 | bettingTime=40, resultTime=8. Time-based (SUN-1245) |
| `isBetting()` | SBR:1013-1015 | Returns `bettingRound` |

### 2.3 Bet acceptance (52 PotSicbo types) — `SicboBetService`

| Engine method | Maps to | Notes |
|---|---|---|
| `acceptBet(SicboBetRequest)` | `MGRoomSicbo.betTaiXiu(User, BetSicboCmd)` (SBR:401-433) | Pre-check `bettingRound`; decode via `SicboBetType.byName(str)`; emits `BetSicboMsg` |
| `internalBet(...)` | `MGRoomSicbo.betTaiXiu(String, ...)` (SBR:526-638) | **Bots always debit** (SUN-880, SBR:568-577) — distinct from TaiXiu |
| Cross-side guard | SBR:551 dead code | **AMBIGUOUS #3 — preserve as-is.** PotSicbo IDs never match 0/1. Sicbo allows multi-side. JavaDoc: "DEAD CODE — Sicbo intentionally allows multi-side bets per user" |
| Min bet | SBR:545 `>= 100L` | INV-13 |
| `txId` | SBR:568 `refId * 1e6 + txnSequence.incrementAndGet()` | INV-12. AtomicLong per room |
| `transactionCode` | SBR:413 `refId + "-" + betIndex` | INV-21 |
| Mongo bet-log | `insertUserBetToDb` (SBR:435-446) | `user_bet_tai_xiu_sicbo` |
| Pot accumulation | potTai/potXiu by betSide==1 else else-branch potXiu | Misleading; payout driven by `listUserBet`. Preserve structure |
| `totalValueBetUser` | SBR:529 real users only | RTP balancer input |

### 2.4 Dice generation + RTP balancer

| Engine method | Maps to | Notes |
|---|---|---|
| `generateDices(SicboRound)` | `MGRoomSicbo.getResult` (SBR:698-730) | (1) `ForceResultStore.consume()`; (2) else `SicboRtpDiceGenerator.generate()`; (3) `SicboFundProtector.protect()` if totalValueBetUser>0 |
| `SicboRtpDiceGenerator.generate(listUserBet, totalValueBetUser)` | `generateResultWithHouseEdge` (SBR:732-776) | Feature-gated `CanCuaRtpBalancer.isEnabled()`. `RtpResolver.effectivePct(0L, "sicbo")`. Tiny-pot guard `< 100_000`. 216 combos. Ties accumulate, uniform random pick |
| Pot-snapshot | At `generate()` call time | **RACE:** mutated by bet thread until disableBetting at count=40; generate runs at count=44. 4s gap. `final List<SicboBet> snapshot = List.copyOf(listUserBet)` defensively |
| `SicboFundProtector.protect(initial)` | SBR:707-721 fallback while-loop | **AMBIGUOUS #4 (infinite loop).** Bound with `MAX_REROLL_ITERATIONS = 1000`. Return best-seen on exhaust. Metric `sicbo.fund_protector.exhausted` |
| `ForceResultStore.consume()` | DAL-SB.suaKetQuaTaiXiu (DAL-SB:370-378) | Atomic remove on `ketquataixiusicbo`. Resolved AMBIGUOUS #2 |
| Tai/Xiu mapping | SBM:488 `total > 10 ? 1 : 0` | INV-14 |

### 2.5 Prize calc — `SicboPrizeCalculator`

| Engine method | Maps to | Notes |
|---|---|---|
| `calculatePrizes(SicboRound)` | `MGRoomSicbo.reward()` (SBR:1170-1255) | **Synchronous in gameLoop** (unlike TaiXiu async settle) |
| Exploit guard | SBR:1179-1200 | If `diceRs==null \|\| listResult.isEmpty()` → refund all via END_TRANS, drain listUserBet. **Critical — preserve.** Quochuy98 incident 2026-05-02 |
| `computePrize(bet, dice)` | SBR:1207 ternary | `betSide∈[15..20]` → count(diceRs, betSide-14)==2?bet*3 : ==3?bet*4 : bet*2. Else `bet * rotation`. INV-8, INV-9 |
| `computeFee(prize)` | SBR:1208 | `(tax * prize) / (200 - tax)` where `tax = MINIGAME_TAX_TX = 5.0f` |
| `updateSumTran` | SBR:870-891 | Cross-side aggregation per user (different from TaiXiu) |
| Wallet credit | SBR:1210 `updateMoney(..., END_TRANS, refId)` | Single credit per bet |
| Persist via DAL | SBR:1236-1247 | RMQ queue_taixiu_sicbo, types 28100/28101/28102 |
| Final per-user msg | SBR:1249-1254 `UpdateFinalSicboMsg` | Aggregate totalMoney across winning bets per user |
| `calculateMoneyReturn` | SBR:808-868 | **AMBIGUOUS #12 — DEAD CODE.** Do NOT port. Mark `// REMOVED-DEAD-CODE` |

### 2.6 Winning-status evaluator — `SicboWinningStatusEvaluator`

Pure function `int[3] dice → List<String> winningStatuses`. Direct port of `getWinningStatuses` (SBR:1090-1144).

Invariants:
- Triple → `TRIPLE_DICES_n` + `ANY_TRIPLE_DICES`, suppresses TAI/XIU/CHAN/LE/DOUBLE_DICES (INV-15).
- Total 3 → `TRIPLE_DICES_1`; total 18 → `TRIPLE_DICES_6`; early-return (no POINT/ONE_DICE).
- Non-triple, non-3, non-18: POINT_n + ONE_DICE_n per present + DOUBLE_DICES_x_y for pairs.

### 2.7 Bot planner — `SicboBotPlanner`

| Engine method | Maps to | Notes |
|---|---|---|
| `loadBots()` | `SicboModule.scheduleBot` (SBM:377-392) | `BotMinigame.getBotSicbo("vin") + getVipBotSicbo()` |
| `tickBots(int count)` | `SicboModule.botBet` (SBM:394-407) | Fires at `timeBetting == 37 - count` (NOT TaiXiu's 28-count) |
| Bet distribution | `BotMinigame.getBotSicbo` (BM:408-491) | **Random-per-bet across 52 IDs**. Different from TaiXiu block-split |
| Wallet | SBR:570 always debit | Preserve AMBIGUOUS #6 |

### 2.8 History

`lichSuPhienTX` (SBM:134, 497-500). 120-entry ring. `LichSuPhienSicboMsg` for client query (SBM:503-507).

## 3. Sicbo-specific reveal hardening

| Aspect | TaiXiu | Sicbo |
|---|---|---|
| Lock at gameLoop count | 45 | 40 |
| Lock window | 6s | 4s today → **recommend 6s parity**. Shrink post-reveal idle 7s → 5s |
| Generation tick | 51 | 44 |
| Settle | async +1s task | synchronous in gameLoop @ count=48 |
| Settled→Cleanup | count=68 | count=55 |

### Snapshot censoring — `SicboSnapshot.dice`

Replace `MGRoomSicbo.updateTaiXiuInfo` (SBR:991-1011). All client-facing serialization through `SicboRound.snapshotForClient(playerId)`. `dice` field null when `phase ∉ {REVEALED, SETTLED}`.

### RevealGuard for inner-reroll Debug.trace

`MGRoomSicbo.java:711, 716, 727` print trial dice during fund-protector while-loop. Per LOW-1 finding: wrap as `RevealGuard.traceInternal(phase, dice, "SICBO reroll")` — requires phase ∈ {GENERATING, REVEALED, SETTLED} else `IllegalStateException`. Prevents future regressions.

### Force-result admin auth (MED-1)

`SicboModule.forceResultTaiXiu` (SBM:275-303) — substring `"superadmin"`. Replace with explicit `Role.SUPERADMIN` in `SicboController`, mounted on admin-only socket.

## 4. RTP balancer deep-dive

### 4.1 Algorithm port (SBR:732-776 line-for-line)

```
input: listUserBet (snapshot), totalValueBetUser, betType→rotation, RtpResolver
output: short[3] dice

1. if !CanCuaRtpBalancer.isEnabled() → TaiXiuUtil.generateRandomResult()
2. winRatePct ← RtpResolver.effectivePct(0L, "sicbo")
3. targetEdgePct ← (winRatePct >= 92 && effectivePct("sicbo") >= 92) ? 0 : 100 - winRatePct
4. if targetEdgePct <= 0 || totalValueBetUser <= 0 → random
5. if totalValueBetUser < 100_000 → random   (tiny-pot guard)
6. targetProfit ← totalValueBetUser * targetEdgePct / 100.0
7. for d1,d2,d3 in 1..6:
     totalPayout ← sotienphaitra(d1, d2, d3)
     profit ← totalValueBetUser - totalPayout
     diff ← |profit - targetProfit|
     if diff < minDiff: clear+add ; elif diff == minDiff: add
8. random pick from bestCombinations
```

### 4.2 `sotienphaitra` purity

Extract `SicboPayoutCalculator.calculatePotentialPayout(listUserBet, dice)`. Pure. Iterates real users only (`tx.userId > 0`, SBR:1152). Uses `SicboWinningStatusEvaluator` + ONE_DICE_* special.

### 4.3 Bounded fund protector

Mitigates AMBIGUOUS #4.

```
input: initialDice, listUserBet, fundTaiXiu, totalValueBetUser
output: ProtectedResult { dice, iterations, exhausted }

const MAX_REROLL_ITERATIONS = 1000

iter ← 0
bestDice ← initialDice
bestTienloi ← totalValueBetUser - sotienphaitra(initialDice)

while (bestTienloi < 0 && fundTaiXiu + bestTienloi < 0):
  if iter >= MAX_REROLL_ITERATIONS:
    log.warn("SicboFundProtector exhausted; returning least-bad dice")
    metric.increment("sicbo.fund_protector.exhausted")
    return bestDice with exhausted=true
  candidate ← TaiXiuUtil.generateRandomResult()
  candidateTienloi ← totalValueBetUser - sotienphaitra(candidate)
  if candidateTienloi > bestTienloi:
    bestDice ← candidate
    bestTienloi ← candidateTienloi
  iter++

return bestDice
```

Behavior-preserving for non-pathological inputs; bounded for pathological.

### 4.4 Pot-snapshot race mitigation

First action in `generate()`: `final List<SicboBet> snapshot = List.copyOf(listUserBet);`. All calcs use snapshot. Eliminates torn reads.

### 4.5 JUnit

| Test | Asserts |
|---|---|
| `rtpBalancerFeatureGate_off_returnsRandom` | `CANCUA_USE_DYNAMIC_RTP` unset → INV-18 |
| `rtpBalancerTinyPot_returnsRandom` | `totalValueBetUser < 100_000` → random |
| `rtpBalancerDeterministic_givenSeed` | Same seed+bets+target → same dice |
| `rtpBalancer216Coverage` | Exactly 216 combos enumerated |
| `fundProtectorTerminates` | Property: any distribution → terminates ≤ MAX |
| `fundProtectorBoundedAndReportsExhaustion` | Pathological → 1000 iter, exhausted=true |
| `fundProtectorBestEffort` | Exhausted → returned dice maximizes tienloi |
| `sotienphaitraOneDiceSpecial` | INV-9: occurrences {1,2,3} → bet×{2,3,4} |
| `sotienphaitraTripleSuppression` | INV-15: triple → POINT/CHAN/LE/DOUBLE pay 0 |

## 5. Force-result — RESOLVED (AMBIGUOUS #2)

### 5.1 Verification done 2026-05-14

`TaiXiuSicboServiceImpl.java:370-378` (`suaKetQuaTaiXiu`):
```
IMap map = client.getMap("ketquataixiusicbo");
if (map.containsKey("ketquataixiusicbo")) return map.remove("ketquataixiusicbo");
```

`SicboModule.java:295-297` (`forceResultTaiXiu`):
```
IMap<String, short[]> map = hz.getMap("ketquataixiusicbo");
map.put("ketquataixiusicbo", dices);
```

**Both sides use `ketquataixiusicbo`.** Force-result works. Class/method names misleading only. No bug. No follow-up ticket.

### 5.2 Extraction action

- Rename engine port `ForceResultStore` (no "TaiXiu" prefix).
- DAL impl can remain `TaiXiuSicboServiceImpl` (wire compat). JavaDoc clarifies map.
- Admin endpoint `POST /api/v2/admin/sicbo/force-result` → `ForceResultStore.put(short[])`.
- Auth: explicit `Role.SUPERADMIN` (fixes MED-1).

### 5.3 Refund-on-stale refId

Force-result consumed at NEXT generateDices call (no refId binding). Document: "Admin forces next round's dice, not specific round."

### 5.4 Tests
- `forceResult_writeReadRoundtrip` — write [3,4,5], consume returns [3,4,5], subsequent returns null (INV-3)
- `forceResult_consumedByNextRound` — write between round N and N+1, dice for N+1 match

## 6. BitZero adapter

`SicboModule.handleClientRequest` (SBM:244-273) delegates via shim:

| cmd | handler | engine call |
|---|---|---|
| 28000 SUBSCRIBE | `subcribeMiniGame` | `SicboService.join(user, gameId, roomId)` |
| 28001 UNSUBSCRIBE | `unsubscribeMiniGame` | `SicboService.leave` |
| 28002 CHANGE_ROOM | `changeRoom` | leave + join |
| 28003 FORCE_RESULT | `forceResultTaiXiu` | `ForceResultStore.put` (admin) |
| 28110 BET | `betTaiXiu` | `SicboBetService.acceptBet` |
| 28116 HISTORY | `getLichSuPhienTX` | `SicboHistory.getRecent(100)` |

Wire-level messages unchanged: `BetSicboCmd`, `BetSicboMsg`, `UpdateSicboPerSecondMsg`, `UpdateResultSicboDicesMsg`, `UpdatePrizeSicboMsg`, `UpdateFinalSicboMsg`, `BetSicboBotMsg`, `StartNewGameSicboMsg`, `LichSuPhienSicboMsg`, `SicboInfoMsg`.

STOMP topics:
- `/topic/sicbo/{roomId}/tick` — per-second SicboSnapshot (censored)
- `/topic/sicbo/{roomId}/reveal` — `RevealMsg` at REVEALED only
- `/topic/sicbo/{roomId}/pot` — pot/player-count deltas

## 7. Shadow + cutover

**Sicbo lands 1 week behind TaiXiu** — higher risk:
1. RTP balancer pot-snapshot race
2. Bounded fund protector — first deployment of this codepath change
3. ONE_DICE_* special easy to break

### Phases

| Phase | Duration | Action |
|---|---|---|
| Shadow-A | 1 wk | Engine alongside legacy. Both compute. Engine discarded; diff logged to `sicbo.shadow.diff` metric. Alert on any diff |
| Shadow-B | 1 wk | Engine source-of-truth for `SicboSnapshot.dice`/`result`; legacy mirrored for wallet writes |
| Canary | 3d | XU room (low stakes) cuts over to engine for wallet. VIN stays legacy |
| GA | 1 wk | Both rooms on engine. Legacy retained as rollback |
| Cleanup | +2 wk | Delete legacy after stable |

**Total: ~5 weeks after TaiXiu GA.**

## 8. Test plan

### 8.1 Invariant JUnits

| Inv | Test class | Asserts |
|---|---|---|
| INV-8 | `SicboPrizeFormulaTest` | Winning non-special → `prize = bet × rotation` |
| INV-9 | `SicboOneDiceSpecialTest` | ONE_DICE_n {1,2,3} occurrences → prize ∈ {bet×2,×3,×4} |
| INV-12 | `SicboTxIdUniquenessTest` | All updateMoney unique txIds |
| INV-13 | `SicboMinBetTest` | bet < 100 → error 4, no wallet call |
| INV-14 | `SicboTaiXiuMappingTest` | total > 10 → result=1 |
| INV-15 | `SicboTripleSuppressionTest` | Triple → TAI/XIU/CHAN/LE/DOUBLE 0; POINT only 3,18 |
| INV-17 | `SicboGameLoopMonotonicityTest` | count reaches 55 once before reset |
| INV-18 | `SicboRtpFeatureGateTest` | `CANCUA_USE_DYNAMIC_RTP` unset → random only |
| INV-19 | `SicboFundDecrementTest` | After reward, `fundTaiXiu_new = fund_old + (totalValueBetUser - totalPayout)` |
| INV-20 | `SicboMongoBetHistoryTest` | Every real-player bet → 1 `user_bet_tai_xiu_sicbo` doc |
| INV-21 | `SicboTxnCodeUniquenessTest` | `refId + "-" + betIndex` unique per round |

### 8.2 Property tests (jqwik)
- `fundProtectorBoundedTermination` — terminates ≤ 1000 iter for any realistic input
- `oneDiceSpecialMatchesOccurrence` — payout = `bet × {2,3,4}` by count
- `getWinningStatusesIsPure` — same dice → same list (no listResult mutation)
- `rtpBalancer216Coverage` — every call evaluates exactly 216 combos

### 8.3 Reveal hardening (mirrors TaiXiu)
- `noDiceInSicboSnapshotPreReveal`
- `revealGuardThrowsBeforeReveal`
- `forceResultRejectsNonSuperadmin`

### 8.4 Integration / shadow
- Replay 1 week of `queue_taixiu_sicbo` through engine; byte-identical reveal + settle vs production
- `exploitGuardActivates` — simulate `diceRs == null`; assert refund to all listUserBet entries

## 9. Risks

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| R1 | RTP balancer pot-snapshot race | HIGH | `List.copyOf` at generate() start. Shadow diff alert |
| R2 | Force-result map name | RESOLVED | Verified `ketquataixiusicbo` both sides |
| R3 | Fund-protector infinite loop | MED | Bounded 1000 iter. Best-effort. Metric+alert |
| R4 | ONE_DICE_* breakage | HIGH | INV-9 + property test. Reviewer checklist |
| R5 | Bot wallet-debit regression | MED | Preserve SUN-880 always-debit. Shadow balance compare |
| R6 | refId gameId 30/31 mismatch (AMB #1) | LOW | Preserve in `SicboReferenceIdStore`. Follow-up SUN ticket |
| R7 | Cross-side dead-code re-intro (AMB #3) | LOW | `// PRESERVED-DEAD-CODE` + JavaDoc. JUnit `multiSidePerUserAllowed` |
| R8 | `calculateMoneyReturn` accidental port (AMB #12) | LOW | §2.5 excludes. Reviewer checklist |
| R9 | tax = 0.0f init order (AMB #5) | LOW | Preserve. `taxSetInCtor` test |
| R10 | Bots always-debit vs TaiXiu skip (AMB #6) | LOW | Document divergence |
| R11 | Exploit-guard refund regression | HIGH | Direct port SBR:1179-1200 + dedicated test |
| R12 | Sync settle blocks gameLoop | MED | Per-bet timeout in `SicboPrizeCalculator`. Alert if reward > 2s |

## 10. Timeline

| Week | Activity |
|------|----------|
| W0 | TaiXiu cutover Shadow-A start (gating event) |
| W1 | Sicbo engine skeleton — `SicboRound`, `SicboBetType`, `SicboWinningStatusEvaluator` (pure). PR-1 |
| W2 | `SicboPrizeCalculator`, `SicboPayoutCalculator`, `SicboFundProtector` + INV-8/9/15/19. PR-2 |
| W3 | `SicboRtpDiceGenerator` + 216 coverage + property + bounded protector. PR-3 |
| W4 | BitZero adapter + `SicboModule` delegation + reveal hardening. PR-4 |
| W5 | Shadow-A deploy + diff harness |
| W6 | Shadow-A soak + bug-bash → Shadow-B |
| W7 | Shadow-B (engine read; legacy wallet) |
| W8 | XU canary + monitor |
| W9-10 | GA both rooms + 2-week soak |
| W11-12 | Legacy `MGRoomSicbo`/`SicboModule` deletion |

**Dedicated effort: ~2 weeks engineering (W1-W4) once TaiXiu lands.**

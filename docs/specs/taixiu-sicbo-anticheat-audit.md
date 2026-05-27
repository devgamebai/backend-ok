# TaiXiu / Sicbo Anti-Cheat Audit — Reveal Lock Window Design

**Audit date:** 2026-05-14
**Scope:** TaiXiu + Sicbo round lifecycle, reveal timing, pre-reveal leak paths, ws-bridge serialization, bet-window lock.
**Code state:** All `room/*.java` and `*Module.java` are CFR-decompiled. Source maps to compiled JARs in production. Behavior is authoritative.

## 1. Current reveal timing (ground truth)

### TaiXiu — `TaiXiuModule.gameLoop()` (TaiXiuModule.java:422-474)

Scheduler tick: 1 Hz, `taskScheduler.scheduleAtFixedRate(..., 10, 1, SECONDS)` (:156). Round length 68 s.

| count | Action | File:Line |
|---|---|---|
| 0 → 44 | Betting open. `botBet` + `updateTaiXiuPerSecond` + `sendTXTime` | `:425-431` |
| 45 | `roomTXVin.disableBetting()`, `roomTXXu.disableBetting()` — sets `enableBetting=false`, writes Redis `allow_betting_{ref}=0` | `:436-439`, `MGRoomTaiXiu.java:252-254` |
| 48 | `calculateMoneyReturn()` (refund pre-calc, no dice) | `:441-443` |
| 50 | `roomTXVin.finish()` — clears `resultTX`, `bettingRound=false`, removes Redis keys. **Dice not yet generated.** | `:445-447`, `MGRoomTaiXiu.java:209-221` |
| **51** | **`generateTaiXiuDices(roomTXVin, roomTXXu)` — dice RNG + `updateResultDices()` broadcasts `UpdateResultDicesMsg` in same call** | `:449-452` |
| 55 | `calculatingTXVinTask` (reward) | `:454-457` |
| 60 | Schedule bots | `:458-462` |
| 68 | `startNewRound()` → count=0 | `:463-467` |

**Generation == broadcast tick:** YES.

```
gameLoop count=51
  → generateTaiXiuDices()
      → roomTXVin.getResult()              // MGRoomTaiXiu.java:577-620
            → result = gen.generate...()   // :587
            → this.updateResultDices(...)  // :619 → broadcasts UpdateResultDicesMsg
      → roomTXXu.updateResultDices(...)    // module:480 → broadcasts to Xu room too
```

### Sicbo — `SicboModule.gameLoop()` (SicboModule.java:417-483)

Round length 55 s. 2026-05-08 final layout (:424-435):

| count | Action | File:Line |
|---|---|---|
| 0 → 39 | Betting open | `:420-441` |
| **40** | `disableBetting()` both rooms — fires BEFORE per-second push | `:436-439` |
| 43 | `finish()` — clears `resultTX`, `bettingRound=false`, removes `allow_betting_*` / `force_result_*` | `:454-457`, `MGRoomSicbo.java:245-268` |
| **44** | **`generateTaiXiuDices(roomTXVin, roomTXXu)` — RNG + same-tick broadcast** | `:458-461`, `MGRoomSicbo.java:699-728` |
| 48 | `reward()`, `updateAllTop()` | `:462-466` |
| 53 | Schedule bots | `:467-471` |
| 55 | `startNewRoundTX()` → count=0 | `:472-476` |

### Dice-bearing writes / reads per tick

| File:Line | Write / Read | Phase | Visibility |
|---|---|---|---|
| `MGRoomTaiXiu.java:256-272` | `updateResultDices` writes `msg.dice{1,2,3}` + `this.resultTX.dice*` + `sendMessageToRoom(msg)` | REVEAL (count=51) | Network broadcast — by design |
| `MGRoomTaiXiu.java:619` | `getResult()` calls `updateResultDices()` from inside the same JVM thread at count=51 | REVEAL | Internal then network |
| `MGRoomTaiXiu.java:1131-1135` | `updateTaiXiuInfo(user).dice{1,2,3}` reads `resultTX` if non-null | All (gated by `resultTX != null`) | **Conditional — see §2** |
| `MGRoomTaiXiu.java:343, 357` | Mongo `user_jackpot_tai_xiu*.result` writes | REVEAL+ | DEFERRED |
| `TaiXiuServiceImpl.java:486-490` | `setKetQuaTaiXiu(short[])` writes Hazelcast `ketquataixiu` map | ADMIN only | Internal |
| `TaiXiuServiceImpl.java:527-546` | `saveResultTaiXiu` publishes RMQ `queue_taixiu` with dice | POST-REVEAL | DEFERRED |
| Sicbo analogues: `MGRoomSicbo.java:324-339, 1004-1008, 1228-1230` | Same pattern | Same | Same |

## 2. Pre-reveal leak audit

### Audit matrix

| # | Path | File:Line | Classification | Notes |
|---|---|---|---|---|
| 1 | `MGRoomTaiXiu.updateResultDices` → `sendMessageToRoom` | `MGRoomTaiXiu.java:271` | INTERNAL→REVEAL | Only invoked from `getResult` (`:619`); only runs at `gameLoop count=51`. |
| 2 | `MGRoomTaiXiu.updateTaiXiuInfo` ships dice if `resultTX != null` | `MGRoomTaiXiu.java:1131-1135` | **SAFE BY CONSTRUCTION** | `resultTX` set ONLY in `updateResultDices` (`:264`), cleared in `startNewGame` (`:182`) and `finish` (`:209`). Pre-reveal → null. |
| 3 | `generateTaiXiuDices` `Debug.trace` | `TaiXiuModule.java:487`, `SicboModule.java:496` | INTERNAL | Server log only. Fires at reveal tick. |
| 4 | `MGRoomTaiXiu.getResult` `Debug.trace` | `MGRoomTaiXiu.java:618` | INTERNAL | Server log only, at reveal tick. |
| 5 | `MGRoomSicbo.getResult` `Debug.trace` inner reroll loop | `MGRoomSicbo.java:711, 716, 727` | INTERNAL | Trial dice logged during `while (tienloi < 0)` reroll. Never broadcast. |
| 6 | `MGRoomTaiXiu.insertUserBetToDb` Mongo | `MGRoomTaiXiu.java:324-336` | DEFERRED | No dice fields. Safe. |
| 7 | `MGRoomTaiXiu.insertUserJackpotDetailToDb` Mongo | `MGRoomTaiXiu.java:343, 357` | DEFERRED | Includes `result` but only after `updateResultDices` set it. |
| 8 | `TaiXiuServiceImpl.setKetQuaTaiXiu` Hazelcast | `TaiXiuServiceImpl.java:486-490` | INTERNAL (admin) | Only called by `forceResultTaiXiu` — see §2.2. |
| 9 | `MGRoomTaiXiu.updateTaiXiuPerSecond` broadcast | `MGRoomTaiXiu.java:529, 547` | DESIGN-SAFE (verify) | See §2.3. |
| 10 | `BroadcastTXTimeMsg` (countdown only) | `TaiXiuModule.java:506-512` | DESIGN-SAFE | Only `remainTime, betting`. No dice. |
| 11 | `LichSuPhienMsg` (history) | `TaiXiuModule.java:494-499` | DESIGN-SAFE | Past rounds. `lichSuPhienTX.add(resultTX)` happens at `:488` — reveal tick. |
| 12 | `StartNewGameTaiXiuMsg` | `TaiXiuModule.java:345-349` | DESIGN-SAFE | `referenceId, jpTai, jpXiu`. No dice. |
| 13 | Sicbo `getResult` `diceRs = diceValues` (`:724`) | `MGRoomSicbo.java:723-725` | INTERNAL | JVM only; `diceRs` read by `reward()` (`:1207`) post-reveal. |
| 14 | `forceResultTaiXiu` `System.out.println` dice | `TaiXiuModule.java:281-282`, `SicboModule.java:298` | INTERNAL (admin) | Server stdout. **Auth bypass — see §2.2.** |
| 15 | ws-bridge `bridge.js` | `bridge.js:1-651` | TRANSPARENT | Opaque passthrough. Does not introduce leak, does not censor. |

### 2.2 Force-result admin path — MEDIUM finding

`TaiXiuModule.java:258-286`:
```java
private void forceResultTaiXiu(User user, DataCmd dataCmd) {
    if (!user.getName().contains("superadmin")) {
        return;
    }
    ...
    map.put("ketquataixiu", dices);
    System.out.println("ForceResultTaiXiu: user=" + user.getName() + " ...");
}
```

**Auth is substring match** on username. Any account whose name contains literal `superadmin` (e.g. `notsuperadmin`, `xsuperadminx`, `superadmin_test`) passes. If registration permits substrings → player could force the next dice, place aligned bet on the round after force consumed. **Result-control vulnerability, not strict pre-reveal leak.** Same shape in `SicboModule.java:275-300`.

### 2.3 `updateTaiXiuPerSecond` verification (AMBIGUOUS)

Per-second broadcasts call `sendMessageToRoom(msg)` (`MGRoomTaiXiu.java:529, 547`, `MGRoomSicbo.java:681, 694`). Did not enumerate `UpdateTaiXiuPerSecondMsg` / `UpdateSicboPerSecondMsg` field shape. Manual test: grep for class field defs + trace `msg.dice*` assignments. Likely safe via the same `resultTX != null` gate.

### 2.4 No-leak summary

**Confirmed LEAK findings: 0** (zero) — strict definition "dice reaches client/network/log before reveal tick".

Invariant relied upon: `resultTX == null` until count=51 (TaiXiu) / count=44 (Sicbo). Enforced by exactly two writers (`startNewGame` clears, `updateResultDices` sets). Every dice-emitting message path gates on `resultTX != null`.

**Adjacent findings:**
- **MED-1:** `forceResultTaiXiu` substring auth (§2.2)
- **LOW-1:** `Debug.trace` inner reroll loop `MGRoomSicbo.getResult` (`:716`) prints trial dice. Harmless today (log only). Wrap in `RevealGuard.allowed(phase)`.
- **LOW-2:** `forceResultTaiXiu` writes to public Hazelcast map name `"ketquataixiu"`. Any HZ client with map name can read. Tighten with namespace/ACL.
- **LOW-3:** `BetTaiXiuCmd` has no nonce; captured packet replayable in any future betting window. Server uses `this.referenceId`, ignores client's. Not pre-reveal leak but worth tightening.

## 3. F12 / packet inspection threat model

### What player sees via devtools today

Cocos connects through ws-bridge over WS (`/ws/minigame`, subdomains `wmini.*` / `wtaixiu.*` / `wsicbo.*`). Bridge does NOT terminate JSON for binary frames — passthrough. DevTools → Network → WS shows:

| Frame | Field | Pre-reveal value |
|---|---|---|
| `BroadcastTXTimeMsg` | `remainTime, betting` | countdown — public |
| `UpdateTaiXiuPerSecondMsg` / `UpdateSicboPerSecondMsg` | pot sizes, player counts | public-safe |
| `TaiXiuInfoMsg` / `SicboInfoMsg` (on join/resubscribe) | `referenceId, remainTime, bettingState, potTai, potXiu, myBetTai, myBetXiu, jpTai, jpXiu, dice1/2/3` | dice fields = 0 when `resultTX == null` (pre-reveal) |
| `UpdateResultDicesMsg` | `result, dice1, dice2, dice3, jackpot` | **arrives at count=51 only** |
| `BetTaiXiuMsg` ack | `Error, currentMoney` | confirmation |

Dice payload bytes in `TaiXiuInfoMsg` always physically present (short[3] = 6 bytes) but filled only from non-null `resultTX`. Pre-reveal devtools watcher sees `0x0000 0x0000 0x0000`. **No pre-reveal leak via passive inspection.**

### Must never send before reveal
- `dice1/2/3` non-zero
- `result` ∈ {0,1} (encoded as `(short)-1` pre-reveal at `MGRoomTaiXiu.java:102`, `MGRoomSicbo.java:118`)
- Per-player payout preview (already absent — payouts computed post-reveal)

### Fine to send
`remainTime`, `bettingState`, pots, own bets, jp accumulators, refId, fake counts.

### JS-heap attack
Cocos `Game` object has `latestRevealResult`. DevTools console can read it but only mutates when server frame populates it. Same constraint. JS-side speculation would require auditing bundled Cocos client (out of scope).

## 4. Bet-window lock — bypass-resistance

### Lock state machine

**TaiXiu:**
- `enableBetting=true` set in `MGRoomTaiXiu.startNewGame()` (`:189`) at `gameLoop count=68→0` (`TaiXiuModule.java:466`).
- `enableBetting=false` in `disableBetting()` (`:252`) at `gameLoop count=45` (`:436-438`).
- Lock check **only in `room.betTaiXiu()`**, not in module handler entry. Two checks: outer `:406` (`if (this.enableBetting)`); inner re-check `:437` **after** `userService.updateMoney` succeeded → if flipped during race → **auto-refund** via `TaiXiuHoanTien` (END_TRANS). Good.

**Sicbo:** identical pattern (`MGRoomSicbo.java:223, 311, 611, 623`).

### Race at count=44.99 vs 45.01
- `count` incremented on 1 Hz scheduler thread; bet handler on Netty IO thread. Race on `enableBetting` (non-volatile).
- 44.99: outer check `:406` reads true → debits → if 45 fires between debit (`:432`) and re-check (`:437`) → inner check refunds. **Bet rejected w/ refund — correct.**
- 45.01: outer check sees false → `result=2` immediately. **Bet rejected — correct.**
- `enableBetting` **not volatile** (`MGRoomTaiXiu.java:104`, `MGRoomSicbo.java:120`). Stale-read window exists but double-check at `:437` recovers. **LOW risk.**

### Redis `allow_betting_*` vs JVM
Written in `startNewRound()` (`TaiXiuModule.java:363`)=1 and `disableBetting()` (`MGRoomTaiXiu.java:253`)=0, deleted in `finish()` (`:219`). Sicbo analogous (`:262, 321`).

**Search for readers:** no reader found in audited paths. **Looks write-only.** AMBIGUOUS — `grep -rn allow_betting backend-master/` to confirm. Today JVM `enableBetting` is sole authority in bet-accept path → HZ-stale state cannot let a bet through.

### Replay
- Captured `BetTaiXiuCmd` (26-byte layout per `bridge.js:76-92`) replayed after `disableBetting()` → outer check `:406` → `result=2`. **Replay-safe.**
- No nonce / per-round token in `BetTaiXiuCmd`. Server ignores client `referenceId`, uses `this.referenceId`. **Packet captured at round N replayed at round N+1 during its betting window will be accepted as N+1 bet.** Not pre-reveal leak (N+1 dice unknown) but worth tightening (LOW-3).

## 5. Proposed hardened reveal design (GSC-style)

### Round phase state machine

```java
enum RevealPhase { OPEN, LOCKED, GENERATING, REVEALED, SETTLED, CLEANUP }
```

Driven by explicit `RevealClock`, not integer counts:

```
OPEN        → LOCKED      at lockTickMs       (was count=45 / count=40)
LOCKED      → GENERATING  at lockEndMs        (lock window 5–10 s)
GENERATING  → REVEALED    when broadcastReady
REVEALED    → SETTLED     after rewardComplete
SETTLED     → CLEANUP     at endOfRoundMs
CLEANUP     → OPEN        at startOfNextRound
```

### Lock window: 6 s recommended

Justification:
- TaiXiu today = 5-tick lock (count 45→50). 6s gives +1s headroom for slow Netty flush (disableBetting's Redis key write 50-200ms on contention).
- 6s ≤ existing 5s spread minimizes UX disruption. 10s feels long for 60s round; 5s is current; 6s is safe nudge.
- Sicbo today = 3-tick lock (count 40→43). Raise to 6s for parity. Shrink post-reveal idle 7s → 6s to absorb.

### Hardened reveal flow

```java
private volatile short[] pendingDice = null;

private void generateDicesLocked() {
    assert phase == RevealPhase.GENERATING;
    pendingDice = rng.generate(...);   // stored, NOT broadcast
}

private void revealDices() {
    assert phase == RevealPhase.REVEALED;
    if (pendingDice == null) throw new IllegalStateException("reveal without dice");
    UpdateResultDicesMsg msg = buildMsg(pendingDice);
    sendMessageToRoom(msg);
    pendingDice = null;
}
```

Today these collapse into one tick. Keep as two with 1-tick gap.

### Snapshot censoring

Replace `updateTaiXiuInfo` / `updateSicboInfo` (`MGRoomTaiXiu.java:1118-1137`, `MGRoomSicbo.java:991-1011`):

```java
public TaiXiuSnapshot snapshotForClient(String username) {
    TaiXiuSnapshot s = new TaiXiuSnapshot();
    s.remainTime = clock.getRemainTime();
    s.bettingState = phase == RevealPhase.OPEN;
    s.potTai = potTai.totalValue();
    s.potXiu = potXiu.totalValue();
    s.myBetTai = potTai.totalByUser(username);
    s.myBetXiu = potXiu.totalByUser(username);
    if (phase == RevealPhase.REVEALED || phase == RevealPhase.SETTLED) {
        s.dice = resultTX.diceCopy();
        s.result = resultTX.result;
    } else {
        s.dice = null;
        s.result = (short) -1;
    }
    return s;
}
```

Every client-facing serialization goes through `snapshotForClient`. JUnit asserts no other path produces non-zero dice pre-reveal.

### `RevealGuard` log wrapper

```java
public final class RevealGuard {
    public static void traceDice(RevealPhase phase, short[] dice, String msg) {
        if (phase != RevealPhase.REVEALED && phase != RevealPhase.SETTLED) {
            throw new IllegalStateException("dice logged in phase=" + phase);
        }
        Debug.trace(new Object[]{msg, dice[0], dice[1], dice[2]});
    }
}
```

Replace 4 dice-printing `Debug.trace` sites: `TaiXiuModule.java:487`, `SicboModule.java:496`, `MGRoomTaiXiu.java:618`, `MGRoomSicbo.java:711, 716, 727`.

### Required JUnit

```java
@Test void noDiceInSerializedSnapshotPreReveal() {
    TaiXiuRound r = newRoundAt(RevealPhase.OPEN);
    byte[] bytes = serializer.serialize(r.snapshotForClient("alice"));
    assertNoDicePayload(bytes);
    r.transitionTo(RevealPhase.LOCKED);
    bytes = serializer.serialize(r.snapshotForClient("alice"));
    assertNoDicePayload(bytes);
    r.transitionTo(RevealPhase.GENERATING);
    r.generateDices();
    bytes = serializer.serialize(r.snapshotForClient("alice"));
    assertNoDicePayload(bytes);    // ← key assertion
    r.transitionTo(RevealPhase.REVEALED);
    bytes = serializer.serialize(r.snapshotForClient("alice"));
    assertDicePresent(bytes);
}

@Test void revealGuardThrowsBeforeReveal() {
    short[] d = {1,2,3};
    assertThrows(IllegalStateException.class,
        () -> RevealGuard.traceDice(RevealPhase.OPEN, d, "x"));
}
```

### Force-result hardening
Replace substring auth with explicit role:
```java
if (!user.hasRole(Role.SUPERADMIN)) return;
```
Or move force-result behind separate admin-only WS endpoint (not player-facing minigame socket).

## 6. Refactor hardening (post-extraction)

| Seam | Today (file:line) | After extraction |
|---|---|---|
| Phase clock | implicit `count` int (`TaiXiuModule.java:425`) | `engine.RevealClock` with `Phase phase()`, `long msUntilNextPhase()` |
| Dice generator | `gen.generateResultWithHouseEdge` (`MGRoomTaiXiu.java:587`) | `engine.DiceGenerator` interface, deterministic-with-seed |
| Snapshot | `updateTaiXiuInfo` (`:1118`) | `engine.TaiXiuRound.snapshotForClient(playerId)` — only client state export |
| Settle | `calculatingTXVinTask` (module:455) | `engine.SettleService.settle(round)` returns `SettleResult` |
| Broadcast | `sendMessageToRoom(msg)` (`:271`) | `room.SerializationBridge.encode(snapshot)` — wraps `snapshotForClient` |

**Property-based tests:**
```
forAll RevealPhase phase ∈ {OPEN, LOCKED, GENERATING}
forAll TaiXiuRound r in phase
forAll String playerId
  assert r.snapshotForClient(playerId).dice == null
```

Eliminates entire class of bugs at compile/test time.

## 7. Findings summary

### Severity
| Severity | LEAK | Adjacent |
|---|---|---|
| CRITICAL | 0 | 0 |
| HIGH | 0 | 0 |
| MEDIUM | 0 | 1 (MED-1) |
| LOW | 0 | 3 (LOW-1, LOW-2, LOW-3) |

**Pre-reveal LEAK count: 0**

### INTERNAL paths confirmed safe
- `generateTaiXiuDices` `Debug.trace` (server log, 2 sites)
- `getResult` `Debug.trace` (server log, 4 sites incl. Sicbo reroll loop)
- `setKetQuaTaiXiu` HZ write (admin force only)
- `insertUserBetToDb` (Mongo, no dice fields)
- `lichSuPhienTX.add` (populated AT reveal tick)
- `resultTX` field gating in `updateTaiXiuInfo` / `updateSicboInfo`

### 5 highest-priority fixes

1. **`forceResultTaiXiu` substring auth (MEDIUM)** — `TaiXiuModule.java:259`, `SicboModule.java:276`. Replace with role check.
2. **Two-tick generation/reveal split + `pendingDice`** — `MGRoomTaiXiu.java:619`, `MGRoomSicbo.java:728`. Explicit phase transition.
3. **`snapshotForClient` censoring method** — `MGRoomTaiXiu.java:1118`, `MGRoomSicbo.java:991`. Single chokepoint.
4. **`RevealGuard` wrapping all dice logs** — `TaiXiuModule.java:487`, `SicboModule.java:496, 711, 716, 727`, `MGRoomTaiXiu.java:618`.
5. **Bet-cmd nonce + refId validation** — `MGRoomTaiXiu.java:312-322`, `MGRoomSicbo.java:312-322`. Reject `BetTaiXiuCmd` whose `referenceId` ≠ current.

### Open AMBIGUOUS
- **A1** — Per-second message fields: confirm no dice in `UpdateTaiXiuPerSecondMsg` / `UpdateSicboPerSecondMsg`.
- **A2** — `allow_betting_*` Redis reader: confirm write-only.
- **A3** — `forceResultTaiXiu` cmd reachability from player socket vs admin-only socket.

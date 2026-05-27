# Lock-Pattern Porting Plan — Hazelcast → Redis

## Executive summary

Nine cache maps still hold cluster locks against Hazelcast IMaps. Each is a small read-modify-write of derived state (streaks, jackpot pot, OTP envelope, broadcast list, daily quest, withdrawal-luck rotation, vippoint event counter); none of them holds wallet balance directly, so the latency target (p99 < 5ms per critical section) and rollback-via-flag pattern apply. The plan ports each site to `DistCache.acquireLock(...)` with try-with-resources, except `cacheApiOtp` which collapses to a Lua-atomic `remove`-returns-prev-value pattern, and treats `huGameBai` carefully because its critical section co-locks the still-Hazelcast `users` and `freeze` IMaps and uses a Hazelcast transaction.

## Per-map sections

### 1. cacheLossThanhDuTX, cacheWinThanhDuTX (TaiXiuServiceImpl)

- **File / lines**: `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/TaiXiuServiceImpl.java` lines 138–158 (caller `calculateThanhDu`), 168–227 (`incrementThanhDu`), 229–235 (`clearThanhDu`).
- **Conversion approach**: `acquireLock(username, 5, SECONDS)` + try-with-resources. The critical section reads, mutates fields, publishes an RMQ message, then `put`s; too rich to atomicize as Lua and the lock semantic carries.
- **Helper signature changes**: `incrementThanhDu(IMap<String,ThanhDuTXModel>, ...)` → `incrementThanhDu(DistCache<String,ThanhDuTXModel>, ...)`. Same for `clearThanhDu`. `calculateThanhDu` body switches the two IMap declarations to `CacheFactory.get("cacheWinThanhDuTX", ThanhDuTXModel.class)` and `cacheLossThanhDuTX`. Drop the `lock(...) / unlock(...)` calls; the read-modify-write block becomes the body of `try (LockHandle h = map.acquireLock(username, 5, TimeUnit.SECONDS)) { if (h == null) { logger.warn(...); return; } ... }`. Preserve the existing "if not contains, init from DAO max + put" branch outside the lock — it's a fresh insert and needs only an extra `putIfAbsent`-style guard if collision is possible (rare; use `containsKey` re-check inside the lock if you keep both branches).
- **Container rebuild scope**: Source lives in `VinPlayDAL`. Loaded by `backend-api` (via `TaiXiuModule` and processors), `game-minigame` (`MGRoomTaiXiu`, `MGRoomCaoThap`, etc.), and the `TaiXiuModule` static initializer. So **rebuild backend-api + game-minigame**. No vbee, no portal, no slot.
- **Order/grouping**: One executor task; both maps flip together because they are mutated by the same method (`calculateThanhDu`). Same task as #2 (OU twin) is fine — same shape, just different module — but keep separate flips for blast-radius isolation: TX first, then OU 24h later.
- **Acceptance test**: 
  1. JMeter / curl two parallel TX game settlements that both should increment a winning streak for the same player. After both complete, the cache `number` field for that player must equal the pre-existing value + 2 (not +1, which would indicate lost update). Inspect via `redis-cli GET cache:cacheWinThanhDuTX:<user>`.
  2. Validate `clearThanhDu` after the opposite-side win: read the loss counter immediately after, confirm `number=0`. 
  3. Confirm that `playOnToday()==false` reset path still produces a fresh `ThanhDuTXModel(username)`.
- **Rollback**: flip `cache.cacheLossThanhDuTX = hazelcast` and `cache.cacheWinThanhDuTX = hazelcast` in `cache.routing.properties`, restart `backend-api` and `game-minigame`. Hazelcast IMap stays primed because it was never depopulated.

### 2. cacheLossThanhDuOU, cacheWinThanhDuOU (OverUnderServiceImpl)

- **File / lines**: `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/OverUnderServiceImpl.java` lines 244–264 (`calculateThanhDu`), 267–321 (`incrementThanhDu`), 323–328 (`clearThanhDu`). Identical structure to #1.
- **Conversion approach / signature changes / rebuild scope**: Same as #1 — `DistCache` parameter swap, `acquireLock` try-with-resources, rebuild backend-api + game-minigame.
- **Order/grouping**: Single executor task, flip 24h after #1 once #1 has soaked.
- **Acceptance test**: Same shape as #1 against an Over/Under game session.
- **Rollback**: flip `cache.cacheLossThanhDuOU` and `cache.cacheWinThanhDuOU` to `hazelcast`, restart.

### 3. cacheBroadcast (BroadcastMessageServiceImpl)

- **File / lines**: `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/BroadcastMessageServiceImpl.java`. Lock guards two methods: `putMessage` (line 41, mutates a top-N `List<BroadcastMsgEntry>`) and `clearMessage` (line 107, drains the list). Reader `toJson()` (line 82) is unlocked.
- **Conversion approach**: `acquireLock(KEY_BROADCAST, 2, SECONDS)`. The critical section is small (sort-insert into a 20-element list); SETNX cost dominates. Lua would not be simpler — the sort logic is Java code. Keep the unlocked `containsKey`-then-init fall-through, but replace the bare `map.put(KEY_BROADCAST, new ArrayList(entries))` first-init with `putIfAbsent` to avoid two threads racing to seed the list.
- **Helper signature changes**: None (single class, all sites internal). Replace `IMap` with `DistCache<String, ArrayList<BroadcastMsgEntry>>` field-level. Also swap the `toJson()` reader to use `DistCache.get`.
- **Container rebuild scope**: `VinPlayDAL` source, used by `game-minigame` (`BauCuaModule`, `MGRoomMiniPoker`, etc. — every minigame room references `GameUtils.broadcastMsgService`) and by `backend-api`. **Rebuild backend-api + game-minigame**. Not used by slot (verify with `grep` if uncertain).
- **Order/grouping**: One executor task. Independent from #1/#2 (different file, different rebuild but overlapping containers). Can run in parallel with #1 if executor capacity permits. Recommended: serial after #1 finishes to keep one-rebuild-at-a-time discipline.
- **Acceptance test**: 
  1. Generate 5 simultaneous big wins (money ≥ MIN_MONEY) from different players. `toJson()` after must show all 5 entries sorted by `getM()` desc, no dropped entries.
  2. `clearMessage()` followed by `toJson()` must return an empty entries list.
  3. Confirm `MAX_SIZE=20` enforced under burst: 25 simultaneous wins → result size ≤ 20.
- **Rollback**: flip `cache.cacheBroadcast = hazelcast`, restart backend-api + game-minigame.

### 4. cacheToiChonCa (BauCuaServiceImpl)

- **File / lines**: `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/BauCuaServiceImpl.java` lines 158–207 (`calculteToiChonCa`). Lock at 163, unlock at 192 inside a per-transaction loop.
- **Conversion approach**: `acquireLock(tran.username, 5, SECONDS)`. Same shape as Thanh-Du: read `ToiChonCaModel`, mutate counters, publish `ToiChonCaMsg` if new high score, `put` back.
- **Helper signature changes**: Method-internal; just swap the local `IMap map` to `DistCache<String, ToiChonCaModel>`. The `newHighScoreToiChonCa` private helper takes a model only — no signature change.
- **Container rebuild scope**: `VinPlayDAL` source, used by `backend-api` and `game-minigame` (`BauCuaModule`, `MGRoomBauCua`). **Rebuild backend-api + game-minigame**.
- **Order/grouping**: One executor task. Schedule after #2 (so the TX/OU rebuild has already proven the shared adapter path).
- **Acceptance test**: 
  1. Run two concurrent BauCua phien results that both should increment `soCa` for the same player. After both, `soCa = pre + 2` and exactly one `ToiChonCaMsg` published if the high-score threshold crossed exactly once.
  2. `playOnToday()==false` branch resets `soCa=0` correctly on the first call after midnight.
- **Rollback**: flip `cache.cacheToiChonCa = hazelcast`, restart backend-api + game-minigame.

### 5. dailyQuestCache (DailyQuestUtils)

- **File / lines**: `backend-master/VinPlayUserCore/src/main/java/com/vinplay/dailyQuest/DailyQuestUtils.java`. Three lock sites — `playerReceiveGift` line 38, `playerPlayGame` line 62, `playerLogin` line 85. Static methods, single static helper class.
- **Conversion approach**: `acquireLock(userName, 5, SECONDS)` per method. `playerReceiveGift` returns a boolean `check` derived from `dailyQuestModel.receiveGiftDailyQuest(index)`; ensure the return value is captured inside the try-with-resources block and returned after.
- **Helper signature changes**: None (no helpers; bodies are inlined). The four sites all do `IMap slotMap = ... .getMap("dailyQuestCache")` — replace each with `DistCache<String, DailyQuestModel> slotMap = CacheFactory.get("dailyQuestCache", DailyQuestModel.class)`. Also replace the unlocked `getDailyQuestModel` reader (line 16–26).
- **Container rebuild scope**: `VinPlayUserCore` source, used by `game-minigame` (`game.modules.quest.DailyQuestModule`, `LobbyModule`) and indirectly by other game-server containers if `MGRoomXxx` calls these utils. **Rebuild backend-api + game-minigame** (verify slot doesn't call DailyQuestUtils — `grep` shows it does not).
- **Order/grouping**: One executor task, independent from prior sites.
- **Acceptance test**: 
  1. Two simultaneous `playerReceiveGift(user, 0)` calls — exactly one returns `true`, the other `false` (the model's `receiveGiftDailyQuest` flips the flag idempotently).
  2. `playerPlayGame` from two threads — final `dailyQuestModel.playGame` cumulative tally equals sum of the two values.
- **Rollback**: flip `cache.dailyQuestCache = hazelcast`, restart backend-api + game-minigame.

### 6. cacheEventVpBonus (VippointServiceImpl + VippointUtils)

- **File / lines**: 
  - `backend-master/VinPlayUserCore/src/main/java/com/vinplay/usercore/service/impl/VippointServiceImpl.java` lines 243–296 (decrement-by-one inside the per-player loop, wraps a sub-block under both `userMap.lock(nickname)` AND `bonusMap.lock(vin)`).
  - `backend-master/VinPlayUserCore/src/main/java/com/vinplay/usercore/utils/VippointUtils.java` lines 392–425 (`calculateNumBonusInDay` — daily reset writer that recomputes `useToday` and overwrites).
- **Conversion approach**: `acquireLock(vin, 5, SECONDS)` for both sites. The decrement is a classic read-1-modify-write; the daily reset is a write-only-after-conditional-read which still benefits from the lock to avoid concurrent reset vs decrement.
- **Helper signature changes**: None (inline). Important wrinkle: the `VippointServiceImpl` site has the `bonusMap.lock(vin)` nested inside the still-Hazelcast `userMap.lock(nickname)` (the `users` map is Wave 4, not migrated). **CORRECTED 2026-05-07 during Task G review**: the original draft of this section said "userMap outer, bonusMap inner". That contradicts the cross-cutting rule below ("Redis OUTSIDE Hazelcast locks"). The cross-cutting rule wins. Task G **inverts** the original nesting so Redis (`bonusMap.acquireLock(vin)`) is OUTER and Hazelcast (`userMap.lock(nickname)`) is INNER. The two locks live in different backends; no deadlock concern. Document the inversion explicitly in the executor's commit message so the future `users` migration to Redis (Wave 4) eliminates this multi-backend concern entirely.
- **Container rebuild scope**: `VinPlayUserCore` source, used by **backend-api**, **portal-api**, and (via `UpdateVippointEventProcessor`) the cron job in backend-api. Not loaded by game servers (`grep` confirmed). **Rebuild backend-api + portal-api**.
- **Order/grouping**: One executor task. Independent of #1–#5; can run in parallel.
- **Acceptance test**: 
  1. Pre-seed `cache:cacheEventVpBonus:<vin>` with `useToday=2`. Trigger `UpdateVippointEventProcessor` such that 5 users would all attempt to claim that bonus. Confirm exactly 2 succeed (counter floors at 0; 3rd–5th get `numBonusToday <= 0` short-circuit).
  2. Run `calculateNumBonusInDay()` (cron at midnight via the `ResetVippointEventProcessor`) and verify the recomputed `useToday` matches `maxInday * (dayRuned + 1) - use` clamped to `[0, num-use]`.
- **Rollback**: flip `cache.cacheEventVpBonus = hazelcast`, restart backend-api + portal-api.

### 7. cacheApiOtp (RechargeServiceImpl)

- **File / lines**: `backend-master/VinPlayUserCore/src/main/java/com/vinplay/dichvuthe/service/impl/RechargeServiceImpl.java`. Three sites: line 2538 (write-only put with TTL — no lock, no port needed), line 2579 (read-then-remove, **no explicit lock**, uses `containsKey`+`get`+`remove`), line 2693 (`apiOtpMap.lock(requestId)` at line 2698, `apiOtp.remove(requestId)` at 2757, `apiOtpMap.unlock(requestId)` at 2763). The lock at 2698 is the only one in scope.
- **Conversion approach**: **Replace lock + get + remove with the existing atomic `DistCache.remove(K)` which returns the prior value.** This collapses the entire critical section to one Redis round-trip. The semantics: only one of N concurrent callbacks can consume the OTP (`remove` returns non-null on the winner; subsequent callers see `null` and short-circuit). All the validation logic (mobile / transId / requestId / amount equality checks) moves to operate on the local `ApiOtp apiOtp = apiOtpMap.remove(requestId)` value. Callers that fail validation must NOT re-put — the OTP envelope is single-use. Worth noting: the inner `userMap.lock(nickname)` block (line 2712 onward) is on the still-Hazelcast `users` map, leave unchanged. Same pattern at line 2585–2587 stays as-is (already lock-free `get`+`remove` race-tolerant by the same atomic-remove logic, but it's not racy because it's invoked from a `synchronized` method `sendConfirmChargingOTP`).
- **Helper signature changes**: None — inline. Replace local `IMap apiOtpMap = client.getMap("cacheApiOtp")` at lines 2538, 2579, 2693 with `DistCache<String, ApiOtpModel> apiOtpMap = CacheFactory.get("cacheApiOtp", ApiOtpModel.class)`.
- **Container rebuild scope**: `VinPlayUserCore` source, used by **backend-api** (RechargeProcessor) and **portal-api** (AwcCallbackProcessor). **Rebuild backend-api + portal-api**.
- **Order/grouping**: One executor task. Independent. Can run in parallel with #6.
- **Acceptance test**: 
  1. Pre-seed `cache:cacheApiOtp:<reqid>` with a valid `ApiOtpModel`. Fire two parallel SMS-Plus callbacks with the same `requestId`. Confirm exactly one applies the recharge to `users` (verify via `LogMoneyUserMessage` count = 1) and the other gets the `containsKey == false` short-circuit.
  2. Confirm TTL still works: write at line 2540 still uses `put(key, value, API_OTP_TIMEOUT, MINUTES)` — unchanged semantics in `RedisCache.put(K,V,long,unit)`.
- **Rollback**: flip `cache.cacheApiOtp = hazelcast`, restart backend-api + portal-api.

### 8. huGameBai (PotServiceImpl, PotUtils)

- **File / lines**: 
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/PotServiceImpl.java` lines 51–58 (read-only `getPot`), 60–129 (`addMoneyPot` — locks `potName` AND `"Vinplay"` simultaneously), 134–216 (`noHu` — locks `potName`, `users:nickname`, `freeze:sessionId` simultaneously, INSIDE a `client.newTransactionContext` Hazelcast transaction).
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/utils/PotUtils.java` lines 19–28 (`init()` — load-from-DB seeder, no lock).
- **Conversion approach**: This is the trickiest map. Two issues:
  1. **Hazelcast transaction**. `addMoneyPot` (line 89) and `noHu` (line 154) wrap mutations of `huGameBai`, `users`, `freeze` in a `TransactionContext`. Because `users` and `freeze` are still Hazelcast (Wave 3/4), we can't relocate the whole txn to Redis. **Required port: drop the Hazelcast transaction context entirely and rely on per-key SETNX locks for `huGameBai` keys + the existing Hazelcast IMap.lock for `users` and `freeze`.** The transaction was providing best-effort atomicity across the three IMaps, but Hazelcast `ONE_PHASE` is itself non-distributed-atomic. Document this regression explicitly in the commit message: it is no different from the pre-existing failure mode of a JVM crash mid-section.
  2. **Two-key lock pattern**. `addMoneyPot` locks both `potName` and `"Vinplay"`. With the LockHandle pattern this nests cleanly:
     ```
     try (LockHandle h1 = potMap.acquireLock(potName, 5, SECONDS);
          LockHandle h2 = potMap.acquireLock("Vinplay", 5, SECONDS)) {
         if (h1 == null || h2 == null) return res;
         // critical section
     }
     ```
     Lock ordering: always acquire `potName` BEFORE `"Vinplay"` to avoid deadlock if two callers race with different `potName`. (`"Vinplay"` is the singleton accumulator, so it serializes anyway.)
  3. **`noHu`** holds three locks: `potMap.lock(potName)`, `userMap.lock(nickname)`, `freezeMap.lock(sessionId)`. Only `potMap` migrates this round. Replace `potMap.lock(potName)` with `try (LockHandle h = potMap.acquireLock(potName, 5, SECONDS))`. Leave `userMap` and `freezeMap` as-is (they're still Hazelcast). Order: acquire Redis lock OUTSIDE Hazelcast locks (so a Hazelcast cluster stall can't pin a Redis lock past TTL).
- **Helper signature changes**: None (no helpers passed). Replace `IMap potMap = client.getMap("huGameBai")` with `DistCache<String, PotModel> potMap = CacheFactory.get("huGameBai", PotModel.class)` at lines 53, 72, 143, and `PotUtils.java` line 21.
- **Container rebuild scope**: `VinPlayDAL` source. `PotServiceImpl` is used by **backend-api** (and via `LogNoHuGameBaiMessage` consumers) and **game-minigame** (card games — `MGRoomXxx`) and **`game-thirdparty`** (verify `grep -rln PotServiceImpl backend-master/game/thirdParty`). PotUtils.init() is called from a bootstrap. **Rebuild backend-api + game-minigame + game-thirdparty if it references PotServiceImpl** (the `grep` above showed `UserMoneyServiceImpl.java` in thirdParty does NOT reference PotServiceImpl directly; verify via fresh grep before flip).
- **Order/grouping**: One executor task, **last** in the sequence — most invasive change. Schedule after #1–#7 have soaked.
- **Acceptance test**: 
  1. Two parallel `addMoneyPot("xidach", 1000, false)` requests against the same pot. Final pot value = pre + 2000, `Vinplay` system pot = pre - 2000 (no lost-update).
  2. `noHu` end-to-end: confirm one and only one player wins the pot; `LogNoHuGameBaiMessage` published once; `users.vin` increments and `freeze.money` adjusts per the maxFreeze logic.
  3. Crash injection (kill the backend-api process mid-`addMoneyPot`): on restart, the SETNX TTL (30s default) expires, next call succeeds, no orphan lock.
- **Rollback**: flip `cache.huGameBai = hazelcast`, restart backend-api + game-minigame + game-thirdparty.

### 9. cacheRutLocOU, cacheRutLocTX (OverUnderServiceImpl, TaiXiuServiceImpl, TaiXiuMD5ServiceImpl)

- **File / lines**: 
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/TaiXiuServiceImpl.java` lines 296–322 (`updateLuotRutLoc`), 333–355 (`getLuotRutLoc`).
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/OverUnderServiceImpl.java` lines 390–416 (`updateLuotRutLoc`), 422–444 (`getLuotRutLoc`).
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/TaiXiuMD5ServiceImpl.java` lines 203–230 (`updateLuotRutLoc`), 248–270 (`getLuotRutLoc`).
- **Conversion approach**: `acquireLock(username, 5, SECONDS)` try-with-resources on every site. The critical section is a single `RutLocCacheModel` field increment / read; could be Lua-ized but the model is JSON-serialized (Jackson decode required to read `getSoLuotRut`), so a Java lock is simpler.
- **Helper signature changes**: None — all sites are inlined. Swap the local `IMap userMap` to `DistCache<String, RutLocCacheModel>` per method body.
- **Container rebuild scope**: All three impls live in `VinPlayDAL`. Loaded by **backend-api** (recharge / api processors call `updateLuotRutLoc` from `LogTanLocMessage` consumers in vbee… verify) and **game-minigame** (`MGRoomTaiXiu`, `MGRoomTaiXiuMD5`, etc.). **Rebuild backend-api + game-minigame**. Confirm vbee processors don't import `TaiXiuServiceImpl` directly (they consume RMQ messages and call back into the service).
- **Order/grouping**: One executor task spanning all three files (TX + TXMD5 + OU rut-loc). All three files' rut-loc methods are isomorphic; treat as one task.
- **Acceptance test**: 
  1. Two parallel `updateLuotRutLoc(user, 1)` against same player. Final `soLuotRut = pre + 2`.
  2. Read after write: `getLuotRutLoc(user)` matches the post-update value.
  3. Cold-start branch (key absent) creates `RutLocCacheModel(soLuotThem)` and `put`s it; subsequent read returns same value.
- **Rollback**: flip `cache.cacheRutLocTX = hazelcast` and `cache.cacheRutLocOU = hazelcast`, restart backend-api + game-minigame.

## Cross-cutting concerns

### Lock TTL choice
`RedisCache.DEFAULT_LOCK_TTL_MS = 30_000`. For all 9 sites the critical section is <100ms; 30s is safely conservative. Don't try to override per-call — the existing `tryAcquireWithToken(key, token, ttlMs)` only takes `DEFAULT_LOCK_TTL_MS` from inside `acquireLock`. If a future critical section grows past 5s (e.g. an RMQ publish blocks), the executor should add a `acquireLock(key, timeout, unit, ttlMs)` overload. Out of scope for this round.

### Multi-backend lock ordering
Sites #6 (`cacheEventVpBonus` nested inside `users`), #7 (`cacheApiOtp` followed by `users`), and #8 (`huGameBai` outer to `users`+`freeze`) all interleave a Redis lock with a Hazelcast IMap.lock. **Rule: acquire Redis lock OUTSIDE Hazelcast locks.** A Hazelcast partition stall while holding a Redis lock means the Redis SETNX TTL will expire and another caller can take it (token-based release ensures we don't unlock theirs). The reverse — Hazelcast outer, Redis inner — risks a Hazelcast lock leak if the Redis call hangs. The PotServiceImpl `noHu` site already nests `potMap.lock` outermost, so swapping potMap to Redis preserves the rule.

### Rebuild scope summary

| Map | backend-api | portal-api | game-minigame | game-thirdparty | game-slot | vbee |
|-----|------------|-----------|---------------|-----------------|-----------|------|
| #1 cacheLossThanhDuTX, cacheWinThanhDuTX | yes | no | yes | no | no | no |
| #2 cacheLossThanhDuOU, cacheWinThanhDuOU | yes | no | yes | no | no | no |
| #3 cacheBroadcast | yes | no | yes | no | no | no |
| #4 cacheToiChonCa | yes | no | yes | no | no | no |
| #5 dailyQuestCache | yes | no | yes | no | no | no |
| #6 cacheEventVpBonus | yes | yes | no | no | no | no |
| #7 cacheApiOtp | yes | yes | no | no | no | no |
| #8 huGameBai | yes | no | yes | verify | no | no |
| #9 cacheRutLocOU, cacheRutLocTX | yes | no | yes | no | no | no |

Game-minigame is the most-touched container; consider one consolidated rebuild after every flip-pair instead of N rebuilds.

## Estimated total effort

| Task | Code change | Manual test | Rebuild + recreate | Total |
|------|-------------|-------------|--------------------|-------|
| #1 ThanhDuTX | 1.5h | 0.5h | 0.3h | 2.3h |
| #2 ThanhDuOU | 1.0h | 0.5h | 0.3h | 1.8h |
| #3 cacheBroadcast | 1.0h | 0.5h | 0.3h | 1.8h |
| #4 cacheToiChonCa | 1.0h | 0.5h | 0.3h | 1.8h |
| #5 dailyQuestCache | 1.0h | 0.5h | 0.3h | 1.8h |
| #6 cacheEventVpBonus | 1.5h | 0.7h | 0.3h | 2.5h |
| #7 cacheApiOtp | 1.5h | 0.7h | 0.3h | 2.5h |
| #8 huGameBai | 3.0h | 1.5h | 0.4h | 4.9h |
| #9 cacheRutLoc TX+OU+MD5 | 1.5h | 0.5h | 0.3h | 2.3h |
| **Subtotal** | | | | **21.7h** |
| Plan/audit overhead, routing-flag commits, soak observation | | | | 4h |
| **Total** | | | | **~26h** |

## Recommended sequencing for executor dispatch

Serial chain (each task waits for prior to land + soak ≥2h in production):

1. **Task A** — #1 (ThanhDuTX). Smallest blast radius for the streak family.
2. **Task B** — #2 (ThanhDuOU). Mirror of A.
3. **Task C** — #5 (dailyQuestCache). Independent; quick win.
4. **Task D** — #3 (cacheBroadcast). Independent; quick win.
5. **Task E** — #4 (cacheToiChonCa). Mirrors #1 shape.
6. **Task F** — #9 (cacheRutLoc TX+OU+MD5). Same container set.
7. **Task G** — #6 (cacheEventVpBonus). Independent container set (backend-api + portal-api).
8. **Task H** — #7 (cacheApiOtp, remove-collapse rewrite). Same container set as G.
9. **Task I** — #8 (huGameBai). Most invasive. Last.

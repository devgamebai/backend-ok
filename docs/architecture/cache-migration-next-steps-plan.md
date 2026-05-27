# Cache Migration — Next Steps Plan

## Executive summary

35 of 69 IMaps are on Redis. 34 remain — `cacheToken` (89 sites, auth-critical, 1 EntryListener), `users` (296 sites, ~50 lock-bound files, wallet money), and 32 long-tail maps. **Recommendation: Wave 3 (cacheToken) ships next as a straight cache-backend swap with one adapter feature added (`put` with maxIdle / sliding TTL) plus `SessionKickListener` ported to a new `removeAndPublish`-only Redis pub/sub bridge. Wave 4 (`users`) ships as Option C — cache-backend swap first under full dual-write soak, BalanceService (Ledger Fix #9) follows independently next quarter.** Doing Fix #9 first would gate the latency win on a 12-week architectural project that can't be staged behind a feature flag with the same blast-radius safety as a routing-flag flip; doing them together overloads the soak window. The long tail splits cleanly into 21 mechanical L1 ports (batchable 5–10/day), 4 L2 lock-bound ports, and 6 L3 careful ports (wallet-coupled, cross-IMap, or with EntryListeners). Realistic Hazelcast decom date: **2026-07-23** (~11 weeks).

---

## Section A — Wave 3: cacheToken (auth-critical)

### A.1 Pre-flip checklist (must complete before any flip)

P0 BLOCKERS:
- **Adapter feature gap — sliding TTL via maxIdle**: `PortalUtils.loginSuccess` and `AdminAuthHelper.generateToken` use the 5-arg `IMap.put(key, value, ttl, unit, maxIdle, unit)`. `DistCache.put` only supports absolute TTL. Without sliding-TTL support, idle admin sessions live longer than configured. Add `DistCache.put(K, V, long ttl, TimeUnit, long maxIdle, TimeUnit)` and a Redis-side impl (e.g. `GETEX` to atomically read+refresh-TTL on every `get`).
- **`RedisCache.publish` envelope upgrade — carry prior value on REMOVED**: `SessionKickListener` reads `entry.getOldValue()` (the nickname) on `entryRemoved`. The current Redis pub/sub envelope (`{type, key}`) drops the value; the listener calls `get(key)` after removal and gets null → kick handler short-circuits silently. **Modify `RedisCache.remove` to publish `{type:REMOVED, key, value:<prior>}` and `RedisCache.addEntryListener` to read the value from the envelope.**
- **`revokeAllTokensForNickname` reverse-index** (otherwise relogin storms cause 5–10K Redis SCAN round-trips). Either implement adapter helper `DistCache.removeAllByValue(V)` with O(1) Redis SET backing, or maintain a parallel reverse-index Redis SET in `PortalUtils`. **Decision: open #1.**

OPERATIONAL:
- Lettuce pub/sub connection pre-warmed at game-server boot.
- Grafana dashboards split for `cacheToken` (keyspace size, GET p99, pub/sub publish rate, per-game-server subscriber count, error rate).
- Alerts: GET p99 > 50ms 5min, error rate > 1/sec 2min, subscriber count drift > 5min, keyspace drop > 30% in 5min.
- All-Java-containers rebuild flight (backend-api + portal-api + game-minigame + game-slot + every CardCoreLib-using game-thirdparty + vbee).
- Adapter unit tests covering sliding-TTL put + remove-with-prior-value publish + cross-JVM kick within 100ms.

### A.2 Call-site inventory (98 files, ~90 logical call sites)

| Module | Files | Notes |
|---|---:|---|
| api/VinPlayBackend | 54 | All admin auth (token-presence, lookup user by token, refresh TTL). Reads except `AdminAuthHelper.generateToken` (5-arg put) and `LoginAdminProcessor` (put). |
| api/VinPlayPortal | 37 | Player auth on every API call. `PortalUtils.loginSuccess` is the canonical write path (5-arg put + reverse-revoke scan). `BalanceWebSocketServlet` reads token to nickname. `LoginByTokenProcessor`, `VerifyTelegramOtpProcessor` write tokens. |
| game/Minigame | 2 | `BaseGameExtension` registers `SessionKickListener`. `ForceLogoutProcessor` reads cacheToken. |
| game/slot | 1 | `SlotMachineExtension` registers `SessionKickListener`. |
| CardCoreLib | 1 | `BaseGameExtension` registers `SessionKickListener` (used by Tour/Poker/etc.). |
| VbeeCommon | 2 | `Consts.CACHE_TOKEN`, `SessionKickListener`. |
| VinPlayUserCore | 1 | `UserExtraServiceImpl` reads cacheToken. |
| **Totals** | **98** | |

Read vs write vs delete:
- Reads ~75 sites (`get`/`containsKey`).
- Writes ~6 sites (5-arg put — all need new adapter overload).
- Removes ~6 sites (`remove(token)` + `revokeAllTokensForNickname` scan).
- Listeners 1 (SessionKickListener).

**No locks on cacheToken** anywhere.

### A.3 Conversion approach

Mechanical `IMap → DistCache` rename. Type signatures: `IMap<String, String>` → `DistCache<String, String>`. Method shapes match for `get`, `containsKey`, `put(K,V,long,TimeUnit)`, `remove`. The 5-arg put requires the new adapter overload.

`revokeAllTokensForNickname`: replace `tokenMap.entrySet()` iteration with either `DistCache.removeAllByValue(nickname)` (preferred) or maintain a parallel reverse-index Redis SET. See A.1.

### A.4 SessionKickListener port

1. `SessionKickListener.register` swaps from `IMap.addEntryListener` to `DistCache.addEntryListener`. Pub/sub bridge already implemented in `RedisCache`.
2. **Adapter prereq**: `RedisCache.publish` must carry prior value on REMOVED. The `GET_AND_DEL_LUA` already returns prior value to Java (Wave 2 Task H); marshal it into the publish envelope.
3. `LifecycleListener` (Hazelcast client-UUID rotation defense) becomes unnecessary — Lettuce auto-reconnects pub/sub.
4. **Cutover ordering**: ship in two atomic steps — (1) listener uses `CacheFactory.get("cacheToken").addEntryListener` while routing flag stays `hazelcast` (no behavior change, code path is on the new abstraction); (2) flip `cache.cacheToken = redis`, full container rebuild.

### A.5 Soak protocol

7 days minimum on staging. Watch:
- Hit rate ~100% on warm cache.
- Error rate (RedisCache[cacheToken]) zero.
- p99 GET < 5ms (Hazelcast baseline ~2–8ms).
- `entryRemoved` propagation < 200ms (Hazelcast baseline ~50–150ms).
- Sliding-TTL semantics: 6h elapsed with periodic activity should keep session alive.
- `revokeAllTokensForNickname` cost under load (10K sessions, 100 nicknames, relogin storm).

Production flip: synchronized 3am window, all 6+ containers recreated together. Watch for 24h.

**Rollback triggers**: any of — p99 > 50ms 5min, error > 1/sec 2min, subscriber drift > 5min, user reports of failed login or stale-session.

### A.6 Rebuild scope

backend-api + portal-api + game-minigame + game-slot + every CardCoreLib-using game-thirdparty (Tour, Poker, etc.) + vbee. All-Java-containers flight.

### A.7 Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Redis down → all auth lost | HIGH | Routing-flag flip-back (instant). Hazelcast still primed. |
| Sliding-TTL bug → premature expiry | HIGH | Block on adapter unit test; 6h manual soak. |
| `revokeAllTokensForNickname` SCAN cost | MEDIUM | Reverse-index pre-flip; load test 10K. |
| `entryRemoved` drops prior value → kick silent fails | **HIGH** | Adapter fix is P0 blocker; verify in staging. |
| Split-brain mid-deploy (some on Redis, some on Hazelcast) | HIGH | Synchronized container flight, no incremental rollout. |
| Lettuce pub/sub connection drop → silent kick failure | MEDIUM | Subscriber-count watchdog log every 30s, alert on drift. |

Worst case: full auth failure. Recovery: routing-flag rollback + restart all containers in parallel (~3 min). Lost-window cohort re-logs in.

---

## Section B — Wave 4: users (wallet) + BalanceService coupling

### B.1 Decision: Option C (cache-backend swap first; BalanceService follows next quarter)

**Three options:**
- **A — pure cache-backend swap**: ~3 weeks. Latency win immediate. Architectural debt unchanged.
- **B — BalanceService first, then no swap**: ~12 weeks (Fix #9 Phase A 4–6 wks + soak). Latency win delayed.
- **C — swap first, BalanceService follows next quarter (RECOMMENDED)**: 3 weeks for swap; Fix #9 starts independently after.

**Why C wins**:
1. Latency is the stated goal of this migration. Deferring 12 weeks loses operational priority alignment.
2. Fix #9 explicitly needs a dedicated quarter-scale resource per ledger roadmap; this migration has not had that.
3. Rollback safety: cache-backend swap preserves call shape (`userMap.get(nick).getVin()`), so `cache.users = hazelcast` flip-back is the rollback. Switching to `BalanceService.get(...)` requires code revert + redeploy.
4. The two efforts are orthogonal — Fix #9 reads/writes are downstream of the cache regardless of backend.
5. Parent ledger roadmap §10 explicitly orders cache migration BEFORE Fix #9.

**Caveat**: if dual-write soak shows non-zero diff → ship minimal BalanceService as soak instrumentation (open decision #2).

### B.2 Per-call-site analysis

127 distinct files, 297 logical call sites. Distribution:

| Module | Files | Notes |
|---|---:|---|
| api/VinPlayPortal | 42 | Most player-facing — login, balance, deposit/withdraw, gift code, recharge. |
| VinPlayUserCore | 32 | Recharge, cashout, security, vippoint, OTP. Heavy lock-bound write paths. |
| api/VinPlayBackend | 24 | Admin balance edits, deposit/withdraw approvals, agency commission. |
| VinPlayDAL | 15 | `MoneyGateway`, BotService, ResetXuService, BroadcastMessage. |
| game/slot | 5 | Slot pre-spin balance read. |
| game/Minigame | 4 | Same idea, minigame-side. |
| api/vbee | 2 | Cron-driven recompute. |
| game/thirdParty | 1 | Game568win adapter. |
| **Total** | **127 / 297** | |

**Lock distribution**: 194 `userMap.lock`/`tryLock` occurrences across 50 files. VinPlayUserCore alone holds 156 (top 5 files: MoneyInGameServiceImpl, MoneyInGameServiceSub, RechargeServiceImpl, CashOutServiceImpl, SecurityServiceImpl). VinPlayDAL: 7 (incl. `MoneyGateway:169` — May-6 incident hot path). All read-modify-write; all become Redis SET-NX EX during the swap. ~140 are likely DELETABLE per parent plan (SQL `UPDATE users SET vin = vin + ?` is itself atomic) but defer to Phase 5 audit AFTER the flip.

### B.3 Multi-backend lock-ordering implications

Wave 2 rule: Redis lock OUTER, Hazelcast lock INNER. Once `users` flips to Redis the rule is moot for the wallet path. During transition:
- `cacheToken` (Wave 3 Redis): read-only on wallet path → no interaction.
- `freeze` (long tail Hazelcast initially): `PotServiceImpl.noHu` already nests `potMap` Redis OUTER → `userMap` Hazelcast → `freezeMap` Hazelcast. When `users` flips: `userMap` becomes Redis-on-Redis with `potMap`. Order `potMap < userMap < freezeMap` already alphabetical-key-stable; no change needed.
- `usersSetWin` (long tail HIGH): if it lands on Redis BEFORE `users`, callers nesting `userMap.lock` inside `usersSetWinMap.lock` would invert the rule. **Mitigation: migrate `usersSetWin` AFTER `users`**.

### B.4 Dual-write soak protocol

| Phase | Window | Routing | Notes |
|---|---|---|---|
| 4-A | 7 days | `cache.users = hazelcast` | Code introduction (rename IMap → DistCache). No behavior change. Compile-error catches unexpected caller patterns. |
| 4-B | 7 days | `cache.users = dualwrite_hz_primary` | New `DualWriteCache<K,V>` decorator. Writes both. Reads from Hazelcast; shadow-reads Redis with diff metric. |
| 4-C | 7 days | `cache.users = dualwrite_redis_primary` | Reads from Redis; shadow-reads Hazelcast. Latency win realizes here. Daily nightly diff job (3-way: MySQL truth, Redis cache, Hazelcast cache). |
| 4-D | 14 days | `cache.users = redis` (still dual-writing for rollback) | Cutover; 14-day stable observation. Wallet justifies double the parent plan's 7-day soak. |
| 4-E | 1 day + 7-day stable soak | drop dual-write | Hazelcast disconnect. **Point of no return** — only enter after 14 stable days + explicit human sign-off. |

Total Wave 4 elapsed: **~5 weeks**.

### B.5 Rollback plan

- 4-A: `git revert`, redeploy.
- 4-B/C: routing-flag flip back to `hazelcast`, redeploy. Diff job logs are forensic.
- 4-D: routing-flag flip back. Hazelcast still being dual-written → data current.
- 4-E: **CANNOT rollback** — Hazelcast no longer written.

If Redis users diverges from MySQL `users.vin` post-cutover:
1. Stop new wallet writes (admin flag rejecting bets).
2. Per-nick reconciliation from MySQL truth (NOT bulk flush — risks cache stampede).
3. Open incident ticket.

### B.6 Pairing with BalanceService (fallback only)

If diff appears mid-soak: ship minimum-viable `BalanceService.get(int userId, String currency, String nickname)` as soak instrumentation (~1 week add-on). Reads `money_account.balance`, shadow-reads `users.vin`, increments diff metric on divergence, returns ledger truth. Replace 5 highest-volume readers (game-server bet-placement). **Does not require Postgres or schema change.**

---

## Section C — The long tail (31 maps)

### C.1 Triage table

(after dropping `cacheToken` Wave 3, `users` Wave 4, `huGameBai` Wave 2 done)

#### Group L1 — mechanical, lock-free, low criticality (21 maps)

`cacheAgentCommission`, `cacheDsAgent`, `cacheDvt`, `cacheSmsPlusPending`, `cacheUserCountLog`, `cacheUserSumLog`, `cache_user`, `cache_user_extra_info`, `cache_user_vp_event`, `game568win_users`, `gscCache`, `jackpottaixiu`, `ketquabaucua`, `ketquataixiu`, `ketquataixiumd5`, `ketquataixiusicbo`, `ketquaxocdia`, `VPMinigame`, `cacheGameBai`, `cacheSlotFree`, `eventMissionCache`.

**Caveat verifications before classifying:**
- `cacheSlotFree`, `cacheGameBai`, `eventMissionCache`: confirm no locks on their own keys.
- `eventMissionCache`: verify no EntryListener.

#### Group L2 — lock-bound, simple RMW (4 maps)

`cacheReports` (17 sites), `cacheTop` (29 sites — leaderboards, HIGH crit), `usersSetWin` (3 sites, HIGH — migrate AFTER `users`), `cacheConfig` (43 sites, HIGH — mostly read-only but verify locks).

#### Group L3 — complex / cross-IMap / listener / wallet-coupled (6 maps)

| Map | Sites | Notes |
|---|---:|---|
| `freeze` | 37 | Tied to wallet. Coupled with `huGameBai` (done) and `users` (Wave 4). **Migrate AFTER `users`**. |
| `gsc_wager_codes` | 2 | Cancels external bets; correctness-critical. |
| `missionMap` | 4 | EntryListener (1 of 3 in audit). Coupled to `userMissionCache`. |
| `userMissionCache` | 1 | EntryListener (1 of 3). Pair with `missionMap`. |
| `cacheSetUserJackpot` | 5 | Slot RTP correctness. |
| `cache_user_active` | 1 | CRITICAL classification but boot-time only — verify in source. |
| `cache_user_money` | 1 | CRITICAL. **Migrate AFTER `users`** unless source read confirms standalone use. |

### C.2 Recommended batch sizes

- **L1 (21 maps)**: 4 batches × 5–6 maps. Multi-subagent: one Plan per batch reviews, 5 Executors in parallel for renames, 5 Reviewers for post-merge audit. ~4 days elapsed.
- **L2 (4 maps)**: one map per multi-subagent loop. ~5 days.
- **L3 (6 maps)**: one map per loop with 48h soak. `freeze` and `cache_user_money` AFTER Wave 4. `missionMap` + `userMissionCache` paired. ~12 days.

### C.3 Effort estimate

| Group | Maps | Hours/map | Total | Subagent invocations |
|---|---:|---|---:|---:|
| L1 | 21 | 1.0 | 21 | ~63 |
| L2 | 4 | 3.0 | 12 | ~12 |
| L3 | 6 | 6.0 | 36 | ~24 |
| Plan/audit overhead | — | — | 8 | — |
| **Total** | **31** | | **~77** | **~99** |

### C.4 Recommended execution order

- **Week 1**: L1 batches A+B (10 maps incl. all `ketqua*` result-history). Parallel-dispatched.
- **Week 2**: L1 batches C+D (11 maps incl. `cacheGameBai`, `cacheSlotFree`, `eventMissionCache`).
- **Week 3**: L2 (4 maps). Save `cacheTop` for last in this group.
- **Week 4–5**: L3 first half (`gsc_wager_codes`, `missionMap`+`userMissionCache` paired, `cacheSetUserJackpot`). 4 maps over 10 days.
- **Week 6+** (post-Wave 4): `freeze`, `cache_user_money`. 2 maps over 5 days.

Wave 4 runs in parallel with weeks 1–6 (Phase 4-A and 4-B don't conflict with long-tail flips).

---

## Section D — Soak / decommission timeline

### D.1 Soak window post-final-map

**14 days** of zero diff + zero error. Final map will be `freeze` or `cache_user_money` (L3 wallet-coupled).

### D.2 Hazelcast decom checklist

1. Confirm zero `getMap` / `HazelcastClientFactory.getInstance().getMap` references in source.
2. Confirm zero `import com.hazelcast` non-test references.
3. Delete `HazelcastCache.java`, `HazelcastClientFactory.java`, old SessionKickListener IMap path, unreferenced `Consts.CACHE_TOKEN` etc.
4. Replace `CacheFactory.get` to always return `RedisCache`. Delete `CacheRouter` HAZELCAST branch. Optionally rename `cache.routing.properties` → `cache.legacy.routing.properties` for forensics.
5. Remove `com.hazelcast:hazelcast-client` from root + every module's `build.gradle`.
6. Drop `hazelcast-1/2/3` services from `docker-compose.database.yml`. Remove `start.sh`/`deploy.sh` Hazelcast preflight.
7. Remove `config/hazelcast.properties` template + `entrypoint.sh:sed` substitutions.
8. `./deploy.sh --rebuild`. JVM startup ~30s faster per JVM. Java image size −80MB.
9. `docker image prune` for stale Hazelcast layers.

### D.3 Final smoke

`tests/run_all.sh`. Missed Hazelcast reader → `NoClassDefFoundError` or `HazelcastClientFactory.getInstance() == null` NPE on next login or wallet read. Decom is one atomic commit + redeploy + restart all 17+ Java services in one window. Keep `:last-working` images for D+7.

### D.4 Estimated date

| Phase | Window | Cumulative |
|---|---|---|
| Wave 3 adapter fixes (sliding TTL, value-on-REMOVED, reverse-index) | 5 days | 2026-05-12 |
| Wave 3 staging soak | 7 days | 2026-05-19 |
| Wave 3 production flip + 7-day stable | 7 days | 2026-05-26 |
| Long tail L1 (4 batches parallel, runs alongside Wave 4-A) | 7 days | 2026-06-02 |
| Long tail L2 + L3 first half (parallel with Wave 4-B/C) | 14 days | 2026-06-16 |
| Wave 4-A code introduction | 7 days | 2026-06-02 |
| Wave 4-B dualwrite hz-primary | 7 days | 2026-06-09 |
| Wave 4-C dualwrite redis-primary 7-day soak | 7 days | 2026-06-16 |
| Wave 4-D cutover + 14-day stable | 14 days | 2026-06-30 |
| Wave 4-E Hazelcast disconnect | 1 day | 2026-07-01 |
| L3 second half (`freeze`, `cache_user_money`) | 5 days | 2026-07-06 |
| Final 14-day all-Redis soak | 14 days | 2026-07-20 |
| Hazelcast decom (D.1–D.3) | 3 days | **2026-07-23** |

**Realistic Hazelcast decom: 2026-07-23** (~11 weeks). Parent plan's 8–10 week estimate was off by 1–3 weeks because Wave 4 wallet soak doubled from 7 to 14 days.

---

## Open decisions (need human input before any executor dispatch)

1. **`revokeAllTokensForNickname` reverse-index** (blocks Wave 3): adapter helper `DistCache.removeAllByValue(V)` with O(1) Redis SET, OR inline reverse-index in `PortalUtils`, OR accept SCAN cost at current scale? **Recommend adapter helper.**
2. **Wave 4 fallback if dual-write diff is non-zero**: ship minimal BalanceService as soak instrumentation (~1 week), OR halt and pivot to Fix #9? **Recommend minimal BalanceService instrumentation.**
3. **`cache_user_money` migration timing**: 30-min source read closes whether it can migrate independently or must wait for Wave 4.
4. **Wave 3 deploy window**: 3am Vietnam-time, Tuesday-Thursday for ops staffing.
5. **L3 EntryListener parity** (`missionMap`, `userMissionCache`): do they fire on entryAdded or entryRemoved? 30-min source read closes.
6. **`users` rollback policy if mid-soak diff**: per-nick reconciliation (recommended) vs full flush.
7. **Hazelcast decom rollback safety**: keep `:last-working` images for D+7 (recommended).

---

## Summary delta from prior plans

vs `HAZELCAST_TO_REDIS_PLAN.md`:
- Wave 3+4+long-tail timeline tightened against actual remaining count (33 vs assumed 70).
- `cacheToken` adapter sliding-TTL maxIdle is a new P0 blocker.
- `RedisCache.publish` value-on-REMOVED is a new P0 blocker for SessionKickListener.
- Wave 4 soak doubled from 7 to 14 days for wallet correctness.
- Decom date concretized to 2026-07-23.

vs `lock-pattern-porting-plan.md`:
- Wave 2 is COMPLETE. This plan does not regress on it.
- Redis-OUTER / Hazelcast-INNER lock-ordering rule carries forward; B.3 documents how `users` migration retires it.

vs `LEDGER_HARDENING_ROADMAP.md` Fix #9:
- Explicitly defers Fix #9 to next quarter (Option C). Does NOT couple it to the cache migration.
- B.6 documents minimum-viable BalanceService as Wave 4 fallback instrumentation.

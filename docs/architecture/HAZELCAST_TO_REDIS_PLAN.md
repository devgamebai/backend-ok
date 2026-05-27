# Hazelcast → Redis Cache Migration Plan

Replace Hazelcast 3.12.13 with Redis 7 as the distributed cache + lock layer across the Java backend. Pairs with the in-flight Redis Streams work (tasks #243–263); we already operate Redis at scale, so this is consolidation, not net-new infra.

> **Status:** plan only. Owner: TBD. Target: 6–10 weeks one engineer (or 4–6 weeks two engineers parallel). Companion to [`POSTGRES_MIGRATION_PLAN.md`](POSTGRES_MIGRATION_PLAN.md) and [`LEDGER_HARDENING_ROADMAP.md`](LEDGER_HARDENING_ROADMAP.md).

---

## 1. Why

Two reasons, in order of impact:

1. **Latency.** The May-6 incident traced `MoneyGateway.debitUser` to ~1,500 ms. Of that, 300–700 ms was `userMap.tryLock` + `userMap.put` round-trips inside `updateCacheAndPushSync`. Redis collapses lock-get-modify-put to a single atomic `SET NX EX` (~0.3 ms). The cache portion of the async task drops from ~500 ms to ~1 ms.
2. **Consolidation.** We already run Redis 7 in production for the Streams migration (task #254). Operating two distributed-state stacks (Hazelcast + Redis) doubles ops surface for no gain. Hazelcast 3.12 is also pinned forever (Cocos client compatibility) — it's not getting faster.

What we **don't** lose: the audit confirmed our codebase uses **zero** of the Hazelcast features that would block this migration:
- 0 `ITopic` (cluster pub/sub) — would need Redis pub/sub if used
- 0 `ReplicatedMap` — would need different topology
- 0 `EntryProcessor` / `executeOnKey` (server-side compute) — has no Redis equivalent
- 0 `Predicates` / queryable map — has no Redis equivalent
- 0 distributed primitives (`AtomicLong`, `CountDownLatch`, `Semaphore`)

Only IMap + `tryLock` + 12 `EntryListener` call sites. All have clean Redis equivalents.

## 2. Scope inventory

From `scripts/cache-migration-audit.sh` against `backend-master/` on 2026-05-07:

| Surface | Count | Distribution |
|---------|-------|--------------|
| Distinct IMaps | **69** | 1 dominates (`users` — 126 files, 296 call sites). Other 68 are 1–5 files each. |
| Source files using Hazelcast | 57 | |
| `tryLock` / `lock` call sites | **255** | Auto-triage heuristic was deliberately conservative — all 255 currently in REVIEW, expected post-human-walk: ~180 deletable (SQL UPDATE already atomic), ~65 → `SET NX EX`, ~10 → Redlock |
| `EntryListener` call sites | 3 | `UserMissionServiceImpl`, `ManagerMission`, `SessionKickListener` → Redis pub/sub |
| Hazelcast-only features used (ITopic / EntryProcessor / Predicates / distributed primitives / ReplicatedMap) | **0** | Confirmed by audit. No blockers. |

### Top IMaps by call-site count

| IMap | Call sites | Criticality | Migration risk |
|------|-----------|-------------|----------------|
| `users` | 296 | **Critical** — wallet cache, every game read | Highest. Last to migrate. Full dual-write soak. |
| `cacheToken` | 89 | Critical — auth tokens, every API call | High. Soak 1 week. |
| `freeze`, `cacheSlotFree`, `usersSetWin` | 4 ea. | Medium — game RTP / win-rate overrides | Medium. Batch with similar maps. |
| `huGameBai`, `gsc_wager_codes`, `cache_user`, `cache_device_fp` | 2 ea. | Medium | Medium. |
| 60+ smaller maps (`bannerCache`, `cacheCaptcha`, `cacheReports`, `jackpottaixiu`, …) | 1 ea. | Low — single-consumer caches | Low. Batch in waves. |

**Order of migration: smallest → largest.** `users` is last; `cacheToken` is second-to-last.

## 3. Architecture

### 3.1 Adapter approach (drop-in IMap surface)

Build a thin Java facade that mimics the IMap surface we actually use. Keep call-site code unchanged; only the factory swaps the implementation.

```java
public interface DistCache<K, V> {
    V get(K key);
    void put(K key, V value);
    void put(K key, V value, long ttl, TimeUnit unit);
    boolean putIfAbsent(K key, V value, long ttl, TimeUnit unit);
    V remove(K key);
    boolean containsKey(K key);
    boolean tryLock(K key, long timeout, TimeUnit unit);
    void unlock(K key);
    void addEntryListener(EntryEventListener<K, V> listener);
}

public final class CacheFactory {
    public static <K, V> DistCache<K, V> get(String name) {
        return CacheBackend.current() == REDIS
            ? new RedisCache<>(name)
            : new HazelcastCache<>(name);
    }
}
```

Per-IMap routing flag (mirrors the `MessageBusFactory` pattern from the Streams migration):

```properties
# cache.routing.properties — toggle per-map
cache.users=hazelcast       # last to flip
cache.cacheToken=hazelcast
cache.bannerCache=redis     # already cut over
cache.jackpottaixiu=redis
# ...
cache.default=hazelcast
```

Roll forward by flipping one entry at a time; roll back by flipping it back. Same pattern proven in the Streams migration.

### 3.2 Redis-side schema

```
key                                 type    TTL     access pattern
─────────────────────────────────────────────────────────────────
cache:<map>:<key>                   STRING  per-map get/set
cache:<map>:lock:<key>              STRING  5–30s   SET NX EX (lock token = uuid)
cache:<map>:events                  STREAM  -       cluster events (replaces EntryListener)
```

- Cache values: serialize as JSON (`Jackson`) — Hazelcast was using Java serialization; the JSON cutover is a forced upgrade and gets us human-debuggable cache values.
- Locks: opaque token per acquisition; `unlock` is a Lua script `if (redis.call('get', k) == token) then del k end` to prevent unlock-by-other.
- Events: one `XADD` per put/remove with the key + op type; subscribers `XREAD BLOCK 0 STREAMS`.

### 3.3 Lock-call triage

The 200 `tryLock` sites split into three buckets:

| Bucket | ~Count | Resolution |
|--------|--------|------------|
| **Delete** — the SQL `UPDATE … WHERE …` already provides atomicity; the cache lock is guarding a stale-write race that doesn't actually exist | ~140 | Remove `tryLock` / `unlock`. Cache `put` becomes unconditional. Documented in audit CSV with a "why safe to delete" line each. |
| **`SET NX EX`** — single-process coordination (e.g. "only one outbox poller runs at a time") | ~50 | Replace with `RedisCache.acquireLock(key, ttl)` returning a `LockHandle` with auto-release on close. |
| **Redlock** — genuinely cross-shard critical sections (none confirmed yet, kept as escape hatch) | ~10 | Use Redisson's `RedissonRedLock` only after confirming the section actually needs cross-shard guarantees. Default-deny: prove it's needed before using. |

The 140 deletions are the biggest perf win — every BET write loses 4 cluster round-trips it never needed.

## 4. Phased timeline

**One engineer: 8–10 weeks.** Two engineers in parallel: 6–7 weeks.

### Phase 1 — Audit + classify (1 week)

Build `scripts/cache-migration-audit.sh`:

- For each of the 70 IMaps: list all call sites, classify access pattern (read-heavy / write-heavy / lock-heavy / listener-bound), estimate hot-path-ness from `grep`-based call-graph.
- For each of the 200 lock sites: classify as DELETE / SETNX / REDLOCK with a reason. Output CSV.
- Identify any IMap whose semantics depend on Hazelcast-specific behavior we missed (e.g. write-through to a `MapStore`, `EvictionPolicy`).

**Deliverable:** `docs/architecture/cache-migration-audit-2026-MM-DD.csv` — the work breakdown for Phases 4–6.

### Phase 2 — Provision (3 days)

Redis is already running (used by Streams migration). Add:
- A separate logical DB (`SELECT 1`) for cache to keep keyspace clean from streams.
- AOF persistence enabled (already configured per task #245).
- Memory cap + `maxmemory-policy allkeys-lru` for cache-eviction safety if a cold map balloons.
- Monitoring: `redis_exporter` cache-specific dashboards (hit rate, evictions, lock-acquire latency).

### Phase 3 — Adapter layer (1 week)

In `VbeeCommon`:

1. `DistCache<K, V>` interface (per §3.1).
2. `HazelcastCache<K, V>` implementing it by delegating to existing IMap (the no-change baseline).
3. `RedisCache<K, V>` implementing it via Lettuce (already on classpath from task #244).
4. `CacheFactory` with routing flag.
5. `LockHandle` AutoCloseable for try-with-resources lock pattern.
6. Lua script bundle: `acquire_lock.lua`, `release_lock.lua`, `set_if_eq.lua` (CAS for the lock-get-modify-put pattern).
7. Unit tests against an embedded Redis (`embedded-redis` or testcontainers).

**Deliverable:** `VbeeCommon/.../cache/` package compiled and tested. No call sites changed yet.

### Phase 4 — Per-IMap migration in batches (3–4 weeks)

Migrate from least-critical to most-critical. Each batch:

```
1. Update each call site to use CacheFactory.get(name) instead of HazelcastClientFactory.getMap(name).
2. Set cache.<map>=hazelcast (no behavior change yet).
3. Deploy. Verify behavior unchanged.
4. Flip cache.<map>=redis on one service first (e.g., portal-api).
5. Watch metrics for 24h. If clean, flip remaining services.
6. After 1 week stable on Redis, mark map done.
```

Ordering (smallest → largest):

| Wave | Maps | Duration | Notes |
|------|------|----------|-------|
| Wave 1 | All single-call-site maps (60+) | 5 days | Batched 10 per day. Low risk, build muscle memory. |
| Wave 2 | 2–4 call-site maps (~6 maps: `freeze`, `cacheSlotFree`, `usersSetWin`, `huGameBai`, `gsc_wager_codes`, `cache_user`, `cache_device_fp`) | 5 days | One per day. Per-map smoke test. |
| Wave 3 | `cacheToken` (89 sites — auth tokens) | 1 week | 3-day soak in dual-read mode (Redis as truth, Hazelcast verified equal); flip; 4-day stable window. |
| Wave 4 | `users` (296 sites — wallet cache) | 2–3 weeks | Full dual-write phase (writes → both, reads → Hazelcast for first week, then Redis with Hazelcast shadow); 1-week stable observation; final cutover. |

### Phase 5 — Lock-call audit (1 week, can parallelize with Phase 4)

Walk the audit CSV. Per site:

- DELETE bucket: remove `tryLock`/`unlock`. Add a one-line comment explaining what made it safe (e.g. "safe to remove: SQL UPDATE … WHERE vin >= ? is the atomic guard"). Test under load.
- SETNX bucket: replace with `try (LockHandle lh = cache.acquireLock(key, 30, SECONDS)) { … }` pattern.
- REDLOCK bucket: reconfirm the section needs cross-shard atomicity. If not, downgrade to SETNX.

**Deliverable:** zero remaining `userMap.tryLock` calls; SETNX-equivalents documented; Redlock dependencies justified per call.

### Phase 6 — `EntryListener` → Redis pub/sub (3–4 days)

12 sites. Per site:

- Identify the trigger (put / remove / evict).
- Replace listener with `cache.subscribe(name, (event) -> { … })` backed by `XREAD BLOCK 0 STREAMS cache:<map>:events`.
- Update the writer side: `RedisCache.put` does `SET …` + `XADD cache:<map>:events …` in a `MULTI`/`EXEC`.

Audit: confirm no listener depends on Hazelcast-specific event ordering guarantees (we don't believe any do, but check).

### Phase 7 — Soak + final cutover (1 week)

After all 70 maps + lock sites + listeners migrated and routing flag = `redis` for everything:

- 1 week observation under production load.
- Tail latency on `MoneyGateway.debitUser` should be at the SQL/SP floor (~10–30 ms p99).
- Hazelcast cluster still running but unused.

### Phase 8 — Decommission Hazelcast (3 days)

1. Confirm zero `getMap`/`HazelcastClientFactory` references in source.
2. Remove `com.hazelcast:hazelcast-client` from `build.gradle` (root + every module).
3. Drop `hazelcast-1`, `hazelcast-2`, `hazelcast-3` from `docker-compose.database.yml`.
4. Delete `config/hazelcast.properties` template + `entrypoint.sh` references.
5. Smoke test full-stack rebuild. Build size + startup time should drop measurably (Hazelcast client + 3 nodes is ~80 MB and ~30 s startup).

## 5. Risks

**Highest:**
- **`users` cache cutover** — wallet correctness depends on this map being right. Dual-write soak with daily diff job (`SELECT vin FROM users JOIN cache:users ON nick`) is non-negotiable. If diff > 0, halt and diagnose before flipping reads.
- **Lock deletion regret** — deleting a `tryLock` that turns out to have been load-bearing causes a race only visible under high concurrency. Each deletion needs a written justification + load-test verification.

**Medium:**
- **Serialization format change** (Java serialization → JSON) means cache contents are not transferable cross-version. The dual-write phase handles this — Redis is rebuilt fresh from app writes, not from a Hazelcast dump.
- **Lettuce vs Redisson choice** — Lettuce is already on the classpath (Streams). Redisson has built-in distributed primitives. Recommend stay on Lettuce + small handcrafted Lua, avoid the Redisson dep weight.
- **Cache-warmup gap** — when a service flips to Redis, its cache is empty for a few minutes until first reads populate it. Acceptable for read-through caches; for write-only caches we may need a one-shot Hazelcast → Redis dump on cutover.

**Low:**
- **Redis single-instance failure** — Streams migration already sized for this; cache adds load but proportionally small. If Sentinel or Cluster topology becomes warranted, decide before Phase 7 cutover.

## 6. Per-map cutover playbook

```
T-30m   Announce cutover for cache:<map> in #ops.
T-15m   Confirm Redis hit-rate for that map > 80% (warmup done if applicable).
T-5m    Final diff: sample 100 keys, assert Hazelcast.get(k) == Redis.get(k) for all.
T-0     Set cache.<map>=redis in cache.routing.properties on portal-api only.
T-0     Restart portal-api. Verify smoke test for that map's feature.
T+15m   Watch error rates, latency. If clean, flip remaining services.
T+1h    Mark map ✅ in tracker. Schedule 24h stability review.
T+24h   If clean, no manual action; if problems, rollback (flip to hazelcast, restart).
T+1wk   Stable. Move to next map.
```

## 7. Per-map rollback playbook

```
1. Set cache.<map>=hazelcast in cache.routing.properties.
2. Restart affected services. Hazelcast was still running (Phase 8 is the only point we drop it), so this is instant.
3. Diff job: confirm Hazelcast cache is current (it should be; we dual-wrote during the soak).
4. Page incident commander.
5. Diagnose with both backends frozen at known state.
```

If the map being rolled back is `users` and the diff is non-zero, the SQL `users.vin` column is the source of truth — clear both caches and let them refill from the next read.

## 8. Open decisions before kickoff

1. **Redis topology** — single instance (current) vs Sentinel (HA failover) vs Cluster (sharded)? Streams workload + cache workload combined still fits one instance comfortably; only revisit if memory pressure shows up. Recommend single instance + replica for read scale-out.
2. **Serialization format** — Jackson JSON (recommended), MessagePack, or Kryo? JSON is human-readable, slightly larger; the 70 caches don't have hot-payload sizes that justify binary.
3. **Per-map TTLs** — Hazelcast has implicit defaults; we'll need to set explicit TTLs per map. Audit CSV captures current `IMapConfig` TTL per map.
4. **Lettuce vs Redisson** — Lettuce + Lua, or Redisson primitives? Recommend Lettuce; Redisson adds 6 MB and an opinionated cluster client we don't need.
5. **Diff-job tolerance** — for `users` cutover, what's the acceptable diff rate? Recommend zero. Anything else means we lose money in the wallet path.

## 9. Cost / timeline estimate

| Item | Cost |
|------|------|
| Audit + classify (1 wk × 1 senior) | ~$5K |
| Adapter layer (1 wk × 1 senior) | ~$5K |
| Per-IMap migration (3–4 wk × 1 senior) | ~$20K |
| Lock audit (1 wk × 1 senior) | ~$5K |
| Listener replacement (3–4 days × 1 senior) | ~$3K |
| Soak + cutover + decom (1.5 wk × 1 senior) | ~$8K |
| Infra: no new infra (Redis already deployed for Streams) | $0 |
| **Total** | **~$46K + 8–10 weeks elapsed** |

For comparison: lets us defer the Postgres migration ($110K, 5–7 mo) without delaying the latency wins, since the cache rewrite kills the most acute production-incident class on its own.

---

## 10. Connection to the broader ledger / DB roadmap

| Project | Status | Why this one first |
|---------|--------|--------------------|
| **Hazelcast → Redis (this plan)** | Plan written, ready | Highest leverage per dollar; eliminates the May-6 incident class permanently. Standalone — doesn't depend on anything. |
| Redis Streams (RMQ replacement) | Soak in progress (#254) | Already running, finishing soon. |
| Postgres migration | Plan written, not started | 5–7 month commitment. Wait until cache + streams are settled. |
| Ledger Fix #9 (`users.vin` decom) | Scaffold only | Quarter-scale. Pairs with Postgres ledger move. Last in the sequence. |

**Recommended sequence:** finish Streams (already in flight) → cache migration (this plan, 8–10 weeks) → Postgres ledger-only (4–6 weeks) → Postgres full migration → Ledger Fix #9.

---

**Next step:** ratify Section 8 decisions, then start Phase 1 (audit script). Phase 1 is a 1-week deliverable that gives an evidence-based per-map plan and the lock-site triage CSV, after which the rest of the work can be parallelized.

# Runbook — Redis Streams down / degraded

**Audience:** on-call engineer, ops/SRE
**Status:** Active during S1 dual-write soak and forward
**Last updated:** 2026-05-04
**Related docs:**
- Migration plan: [`RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md`](./RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md)
- AOF recovery script: [`scripts/recover-redis-aof.sh`](../scripts/recover-redis-aof.sh)
- DLQ tooling: [`scripts/redis-dlq-inspect.sh`](../scripts/redis-dlq-inspect.sh), [`scripts/redis-replay-dlq.sh`](../scripts/redis-replay-dlq.sh)
- RMQ→Redis backfill: [`scripts/redis-replay-from-rmq.sh`](../scripts/redis-replay-from-rmq.sh)
- Alert rules: [`infra/alerting/redis-streams-alerts.yml`](../infra/alerting/redis-streams-alerts.yml)

---

## Quick-reference: alert → action

| Alert | Severity | First action | If unresolved at 10 min |
|---|---|---|---|
| `RedisContainerRestart` | HIGH | §1 — confirm AOF replay completed; check container logs | Page DBA, run `recover-redis-aof.sh` |
| `RedisMemoryPressure` | HIGH | §2 — check `INFO memory`, identify worst-offender stream | Lower MAXLEN on offending stream; raise maxmemory if all streams near cap |
| `RedisAofRewriteSlow` | MEDIUM | §3 — observe one cycle, confirm not blocking writes | Disk I/O check; consider tuning `auto-aof-rewrite-percentage` |
| `RedisStreamConsumerLag` | HIGH | §4 — identify which `vbee` worker is behind | Restart vbee consumer pool; check downstream MySQL |
| `RedisStreamPendingStuck` | HIGH | §5 — XPENDING the group, find the stuck delivery | XCLAIM via runtime auto-reaper or manual `XAUTOCLAIM` |
| `RedisStreamDLQNonEmpty` | MEDIUM | §6 — `redis-dlq-inspect.sh` to view, decide replay or drop | Replay with `redis-replay-dlq.sh --apply` if root cause known |
| `MessageBusAuditDropping` | MEDIUM | §7 — audit-side data loss; check MySQL pool + table | Add dedicated `mqsqlpool_audit` pool; reconciliation will be approximate for the dropped window |
| **No alert, but Redis unreachable** | CRITICAL | §8 — full Redis-down decision tree | Rollback per §10 |

---

## Section 1 — `RedisContainerRestart`

**Symptom:** `sunwinkr-redis` healthcheck flapped or container restarted.

**Likely causes (most common first):**
1. Host OOM-killed the process.
2. AOF tail corrupted on ungraceful shutdown — restart loop. (See [`recover-redis-aof.sh`](../scripts/recover-redis-aof.sh) header for the 2026-04-21 incident pattern.)
3. Manual restart by an operator.

**Actions:**
```bash
docker logs --tail=200 sunwinkr-redis | grep -iE "error|fatal|loading|aof"
docker exec sunwinkr-redis redis-cli -n 1 PING            # expect PONG
docker exec sunwinkr-redis redis-cli INFO persistence | grep aof_
```

If `aof_last_write_status:err` or container is in CrashLoopBackoff:
```bash
DRY_RUN=1 ./scripts/recover-redis-aof.sh    # show plan
./scripts/recover-redis-aof.sh              # snapshot+repair+restart
```

**Producer impact during the gap:**
- `MESSAGE_BUS_DEFAULT=rmq` (current default through S1) → producers wrote to RMQ first; RMQ has the messages. Redis missed them.
- After Redis is back, run §9 (RMQ→Redis backfill) for queues whose audit table shows `redis=failure` rows in the gap window.

---

## Section 2 — `RedisMemoryPressure`

**Symptom:** `used_memory > 0.75 × maxmemory`. With `noeviction` policy, hitting 100% means publishes start failing.

**Triage:**
```bash
docker exec sunwinkr-redis redis-cli -n 1 \
  --bigkeys --memkeys --memkeys-samples 0 2>/dev/null | head -40
docker exec sunwinkr-redis redis-cli -n 1 \
  XINFO STREAM "{queue_log_money}.stream" | grep -E "length|max-deleted"
```

**Decision tree:**
- **One stream dominates** → its MAXLEN is misconfigured or its consumer is stalled (see §4). Trim manually:
  `XADD <key> MAXLEN ~ 50000 1-1 force_trim 1` (the trim happens on the next real publish; this just establishes the ceiling).
- **Spread evenly across streams** → workload genuinely outgrew capacity. Bump `maxmemory` in `docker-compose.yml` Redis service (current 24G per Plan task P3). Capacity model in [`docs/REDIS_STREAMS_CAPACITY.md`](./REDIS_STREAMS_CAPACITY.md) (O4, draft) should be updated alongside.
- **DLQ is the culprit** → see §6.

**Never** flip eviction policy off `noeviction` — Redis Streams under `allkeys-lru` will silently drop pending messages.

---

## Section 3 — `RedisAofRewriteSlow`

**Symptom:** AOF background rewrite > 60s. With `appendfsync everysec`, the main loop is unblocked, but slow rewrites mean fsync pressure.

**Actions:**
```bash
docker exec sunwinkr-redis redis-cli INFO persistence | \
  grep -E "aof_rewrite|aof_last_rewrite|aof_pending"
docker exec sunwinkr-redis redis-cli LATENCY HISTORY aof-write
```

If consistently slow:
- Check host disk I/O (`iostat -x 1` on the host).
- Tune `auto-aof-rewrite-percentage 100` → `200` to halve rewrite frequency.
- If rewrites are starving foreground, temporarily switch `appendfsync` to `no` for the duration, then revert. Document in incident log.

---

## Section 4 — `RedisStreamConsumerLag`

**Symptom:** stream length grows faster than consumer drains it (Prometheus: `redis_stream_messages_lag` per stream/group).

**Triage:**
```bash
# Which group is behind?
docker exec sunwinkr-redis redis-cli -n 1 \
  XINFO GROUPS "{queue_log_money}.stream"
# Last-delivered-id vs latest stream id.
docker exec sunwinkr-redis redis-cli -n 1 \
  XLEN "{queue_log_money}.stream"
```

**Common causes:**
1. `vbee` container hung or restarted with cold cache → restart it (`docker restart sunwinkr-vbee`). PEL drain on startup will pick up un-acked entries.
2. Downstream MySQL is slow → check Hikari pool stats; the consumer pool is bound by DB write speed. Pool stats are in `MessageBusMetrics` (Prometheus: `messagebus_consumer_inflight`).
3. Consumer pool too small for incoming TPS → bump `GSC_SIDE_EFFECT_THREADS` (current 16) only after profiling — more threads against a saturated MySQL pool just queues more contention.

**Pre-existing levers:**
- `XAUTOCLAIM` reaper inside `RedisStreamConsumer` will reclaim stuck deliveries from dead workers within `pending_idle_ms` (default 60000ms).
- Atomic Lua DLQ promotion fires when delivery count exceeds `max_deliveries` (default 5).

---

## Section 5 — `RedisStreamPendingStuck`

**Symptom:** an entry in PEL has `idle > 5 min` and is not making progress.

**Actions:**
```bash
docker exec sunwinkr-redis redis-cli -n 1 \
  XPENDING "{queue_log_money}.stream" "vbee" IDLE 300000 - + 10
```

For each stuck `(consumer, message-id, idle, deliveries)`:
- If `deliveries < 5`: the runtime's `XAUTOCLAIM` will redeliver — wait one reaper cycle (~30s).
- If `deliveries >= 5`: should already be DLQ-promoted. If it isn't, the Lua promotion script likely failed. Manual:
  ```bash
  docker exec sunwinkr-redis redis-cli -n 1 \
    XCLAIM "{queue_log_money}.stream" vbee operator 0 <message-id> JUSTID FORCE
  ```
  to bump the delivery counter — next cycle will promote.

**Never** `XACK` a stuck entry to make the alert go away. That drops the work without delivering it. Use DLQ promotion instead.

---

## Section 6 — `RedisStreamDLQNonEmpty`

**Symptom:** `{queue}.dlq` has at least one entry, meaning the consumer hit `max_deliveries` and gave up.

**Actions:**
```bash
./scripts/redis-dlq-inspect.sh queue_log_money       # read-only inventory
./scripts/redis-dlq-inspect.sh queue_log_money --tail=20
```

**Decision:**
- **Known transient cause** (MySQL was down, downstream service flapped): replay.
  ```bash
  ./scripts/redis-replay-dlq.sh queue_log_money                    # dry-run
  ./scripts/redis-replay-dlq.sh queue_log_money --apply --count=50
  ```
- **Poison message** (malformed payload, schema violation): drop. Document in incident log; do **not** replay.
- **Mixed**: filter by `--reason=` in the replay script.

---

## Section 7 — `MessageBusAuditDropping`

**Symptom:** `MessageBusAuditWriter.droppedCount` is rising. Audit-side data loss; the publish itself was fine, but reconciliation queries during S1 will under-count for the affected window.

**Likely causes:**
1. MySQL slow / pool exhausted → audit worker can't drain its 8192-row queue fast enough.
2. `message_bus_audit` table missing → re-check S1 schema; the writer logs WARN once/min until the table appears.
3. Sustained TPS spike beyond audit worker throughput (single-threaded INSERT loop).

**Actions:**
- Confirm table:
  ```sql
  SHOW CREATE TABLE vinplay.message_bus_audit\G
  ```
- Check MySQL connection pool stats (Hikari JMX or `mysqlpoolname` MBean).
- For sustained pressure, S1's "dedicated `mysqlpool_audit`" item moves from "preferable" to "required." The writer comment block flags this — see `MessageBusAuditWriter.java` §"Connection pool".

**Does NOT cause message loss** — only audit/reconciliation gaps.

---

## Section 8 — Redis fully down

This is the critical scenario the migration plan task line 644 calls out. The producer-side blast radius depends on the active `MESSAGE_BUS_*` mode.

### 8a. Mode = `rmq` (S0 / pre-cutover default)
**Impact:** none. Producers go to RMQ only; Redis is not in the publish path. Consumers reading from Redis (vbee runtime) are idle, no work missed.

### 8b. Mode = `dual` (S1 soak, current setting per `.env` `MESSAGE_BUS_DEFAULT=rmq` + per-queue overrides flipping to `dual`)
**Impact:** RMQ wrote first → no end-to-end data loss. Redis-side audit rows for the gap window will be `success=0`. Consumers reading the Redis stream are starved during the gap.
**After recovery:** run §9 (RMQ→Redis backfill) for any queue whose Redis-side consumer must catch up.

### 8c. Mode = `redis` (S2 cutover and forward)
**Impact:** **DATA LOSS** if Redis publish fails — the dual-write swallow is no longer present, and there is no RMQ fallback. Migration plan line 644 calls this out: *"currently swallowed → data loss; need explicit `safety net` rollback to `dual`."*
**Action:** flip the affected queue back to `dual` immediately:
```bash
# Per-queue env override; restart producer.
echo "MESSAGE_BUS_QUEUE_LOG_MONEY=dual" >> .env
docker restart sunwinkr-vbee sunwinkr-backend-api    # or whichever publish
```
Then run §9 backfill for the gap.

---

## Section 9 — RMQ → Redis backfill

When Redis missed publishes (8b/8c above) but RMQ has them, drain RMQ into Redis using
[`scripts/redis-replay-from-rmq.sh`](../scripts/redis-replay-from-rmq.sh).

**Mandatory pre-step: stop the RMQ consumer for the affected queue.** Otherwise the live consumer races the replay script and drains messages from RMQ before the script can read them, and they're lost from Redis permanently.

```bash
# Pause vbee consumer (simplest is a full container stop; finer-grained
# control requires per-consumer flags not present today).
docker stop sunwinkr-vbee
# Confirm RMQ depth.
docker exec sunwinkr-rabbitmq rabbitmqctl list_queues name messages | grep queue_log_money

# Dry-run first.
./scripts/redis-replay-from-rmq.sh queue_log_money
# Then apply.
./scripts/redis-replay-from-rmq.sh queue_log_money --apply
# (Hot-path queues require --confirm-hot.)

# Resume consumer.
docker start sunwinkr-vbee
```

**Verification:**
```bash
docker exec sunwinkr-redis redis-cli -n 1 \
  XLEN "{queue_log_money}.stream"
docker exec sunwinkr-redis redis-cli -n 1 \
  XINFO STREAM "{queue_log_money}.stream" | grep -E "length|first-entry|last-entry"
```

**Caveats:**
- Replay re-publishes from RMQ; RMQ delivery semantics ack the message off RMQ as the script reads. If Redis XADD fails after RMQ ack, the message is gone from both. The script aborts after 3 consecutive XADD failures to bound the loss.
- Replayed entries get fresh stream-ids (current millis); their position in the stream does **not** reflect the original publish time. This affects time-range queries against the stream but not consumer-group delivery.
- Wire format is preserved (`b`, `c` fields), so consumers can't tell replayed entries from native ones.

---

## Section 10 — Rollback (per-queue or full)

### Per-queue rollback (safest, S2 cutover regret)
```bash
# Flip the queue's MESSAGE_BUS_<QUEUE> env back to dual or rmq.
sed -i 's/^MESSAGE_BUS_QUEUE_LOG_MONEY=.*/MESSAGE_BUS_QUEUE_LOG_MONEY=rmq/' .env
docker restart sunwinkr-vbee sunwinkr-backend-api sunwinkr-portal-api
```
Producers immediately stop writing to Redis for that queue. Consumers continue draining whatever's already in `{queue}.stream`. No data loss.

### Full rollback (S1/S2 abort)
```bash
sed -i 's/^MESSAGE_BUS_DEFAULT=.*/MESSAGE_BUS_DEFAULT=rmq/' .env
# Remove any per-queue dual/redis overrides.
sed -i '/^MESSAGE_BUS_QUEUE_/d' .env
./deploy.sh   # or per-service restart
```
Any in-flight Redis-only entries (S2 cutover only): replay them back to RMQ via the inverse script (TODO — symmetric of `redis-replay-from-rmq.sh`; document and write before S2). For S1 dual-write all messages also went to RMQ, so nothing to backfill.

---

## Section 11 — Pre-soak verification (S0 prerequisites)

Before flipping any queue to `dual` for the soak:
1. `MessageBusAuditWriter.insertedCount > 0` after a synthetic publish (table exists, worker running).
2. Per-queue audit row appears with `backend='rmq'` AND `backend='redis'` for the same `(queue, command)` within 1s.
3. `XLEN {queue}.stream` increments by 1 per producer publish.
4. Consumer (vbee) decreases PEL within `pending_idle_ms` window.
5. All 7 alert rules in [`infra/alerting/redis-streams-alerts.yml`](../infra/alerting/redis-streams-alerts.yml) load in Prometheus and have at least one valid evaluation cycle.

If any of (1)-(5) fails, do not start the soak — fix the gap first.

---

## Reference

| Item | Value |
|---|---|
| Redis container | `sunwinkr-redis` (image `redis:7-alpine`) |
| Redis DB for streams | `1` (DB 0 is the cache, isolated per Plan P3) |
| Stream key format | `{<queueName>}.stream` (literal `{}` = cluster hashtag) |
| DLQ key format | `{<queueName>}.dlq` (same hashtag, co-located slot) |
| Default consumer group | `vbee` |
| Default `max_deliveries` | 5 (DLQ-promote threshold) |
| Default `pending_idle_ms` | 60000 (XAUTOCLAIM reaper) |
| `maxmemory` | 24G, `noeviction` |
| Persistence | AOF, `appendfsync everysec` |
| Audit table | `vinplay.message_bus_audit` |
| Default mode env var | `MESSAGE_BUS_DEFAULT` (rmq / dual / redis) |
| Per-queue override pattern | `MESSAGE_BUS_<QUEUE_UPPER>` (e.g. `MESSAGE_BUS_QUEUE_LOG_MONEY`) |

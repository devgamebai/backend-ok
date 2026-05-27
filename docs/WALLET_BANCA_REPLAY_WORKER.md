# BanCa Failed-Settle Replay Worker

**Module:** `backend-master/banca-replay-worker/`
**Main class:** `com.vinplay.bancareplay.BancaReplayWorker`
**Predecessors:** SUN-1054 (BanCa C# unified-wallet bridge), Phase 5b/5c
**Compose entry:** [`docker-compose.banca-replay.yml`](../docker-compose.banca-replay.yml)

## Why this exists

When the BanCa C# server fails to settle a session balance via the Java
`MoneyGateway` HTTP bridge (`c=9998`) after 3 retries, it pushes the
payload onto the Redis list `banca:failed_settle` and drops it from the
caller's view (see
`banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs`, `QueueFailed`).

Before this worker existed, that list grew forever. The replay worker
drains it back into MoneyGateway, leaning on the server-side idempotency
guarantee (each payload carries an `external_ref` of the form
`banca:settle:{userId}:{sessionId}:{checkpointMs}`; the
`(tx_id, source)` UNIQUE on `money_gateway_log` dedupes a replay of an
already-applied entry).

## Operation

Every `BANCA_REPLAY_INTERVAL_SEC` seconds (default 30) the worker:

1. **Promotes due retry buckets.** Scans keys matching
   `banca:failed_settle:retry_at:*`; for any whose timestamp suffix has
   elapsed, drains them back onto `banca:failed_settle` and deletes the
   bucket.
2. **LPOPs up to `BANCA_REPLAY_BATCH_SIZE` items.** For each:
   - **HTTP 2xx + `success:true`** -> dropped.
   - **HTTP 4xx, or 2xx + `success:false` with errorCode 1xxx/4xxx** ->
     moved to `banca:failed_settle_dead` (terminal validation / missing
     user / insufficient balance — replaying will not help).
   - **HTTP 5xx, IO timeout, or 2xx + `success:false` with errorCode 9999**
     -> rescheduled into `banca:failed_settle:retry_at:{ts}` with
     exponential backoff (1s, 2s, 4s, 8s ... capped). After
     `BANCA_REPLAY_MAX_ATTEMPTS` (default 8) the entry also moves to the
     dead list.
3. **Stats.** Emits a one-line tick log; writes a `WARN` row to
   `vinplay.money_anomaly` on any dead-list growth so the SUN-1141
   audit-bot Telegram pipeline alerts ops.

The attempt counter is carried inside the JSON payload as
`_replay_attempt` so it survives a re-push through the retry bucket
without needing a side table.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `BANCA_REPLAY_ENABLED` | `false` | Master kill switch. `1` -> live, `0` -> idle heartbeat. |
| `BANCA_REPLAY_INTERVAL_SEC` | `30` | Tick cadence. |
| `BANCA_REPLAY_BATCH_SIZE` | `200` | Max items LPOPed per tick. |
| `BANCA_REPLAY_MAX_ATTEMPTS` | `8` | Retries before forced dead-list. |
| `BANCA_MONEYGATEWAY_URL` | `http://backend-api:19082/api_backend?c=9998` | Full URL of `BanCaSettleProcessor`. |
| `BANCA_SERVICE_TOKEN` | _required_ | Shared `X-Service-Token` header (same value used by BanCa C# and SUN-1054 LogBetCommission). |
| `REDIS_STREAMS_HOST` / `REDIS_STREAMS_PORT` / `REDIS_PASSWORD` | match BanCa | Redis connection. |
| `BANCA_REPLAY_REDIS_DB` | `0` | DB index — BanCa C# pushes onto DB 0 by default. |
| `BANCA_REPLAY_HTTP_TIMEOUT_MS` | `5000` | Per-attempt HTTP timeout. |

## Build + test

```bash
cd backend-master
./gradlew :banca-replay-worker:jar    # produces build/libs/banca-replay-worker-1.0.jar
./gradlew :banca-replay-worker:test   # runs the JUnit suite
```

The shared backend Dockerfile (`backend-master/Dockerfile`) already
copies `banca-replay-worker-1.0.jar` into `/app/libs/app/` so the worker
is shipped on every rebuild of the backend image. The stand-alone
`backend-master/banca-replay-worker/Dockerfile` is provided as a
fallback for ops teams that want an independent rollout cadence.

## Bring up

```bash
# Edit .env first — at minimum BANCA_SERVICE_TOKEN must be set.
# Flip BANCA_REPLAY_ENABLED=true in docker-compose.banca-replay.yml
# (or via .env override) once you're ready to drain.

docker compose \
  -f docker-compose.yml \
  -f docker-compose.database.yml \
  -f docker-compose.backend.yml \
  -f docker-compose.banca-replay.yml \
  up -d banca-replay-worker
```

## Monitoring

### Pending / dead list sizes

```bash
# Pending — should drain to zero between bursts
docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" LLEN banca:failed_settle

# Dead — investigate every entry; this is operator-driven
docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" LLEN banca:failed_settle_dead

# Retry buckets in flight
docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" KEYS 'banca:failed_settle:retry_at:*'
```

### Anomaly alert

Each tick that grows the dead list inserts a row into
`vinplay.money_anomaly` with severity `WARN` and invariant
`BANCA_FAILED_SETTLE_DEAD`. The SUN-1141 audit-bot picks this up and
posts to Telegram. Grow tolerance is 0 — every entry on the dead list
represents a real settle the player either won or lost that the wallet
never recorded.

### Worker logs

```bash
docker logs -f sunwinkr-banca-replay-worker | grep BancaReplayWorker
```

Look for the tick line:

```
BancaReplayWorker tick replayed=N ok=N dead=N rescheduled=N | total ok=... dead=...
```

### Prometheus

Phase 1 ships stats via the log line above. A follow-up may expose
`/metrics` on a Jetty servlet inside the worker for native Prometheus
scrape — the counters are already broken out per outcome inside the
worker (`getTotalReplayed`, `getTotalSucceeded`, `getTotalToDead`,
`getTotalRescheduled`).

## Manually draining the dead list

Each dead-list entry is a JSON payload identical to what the BanCa C#
client originally produced, with the addition of a `_replay_attempt`
counter. Ops investigation steps:

1. **Inspect:**
   ```bash
   docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" LRANGE banca:failed_settle_dead 0 -1 \
     | jq -r '.' | less
   ```
2. **Triage:** for each entry, decide if the settle should be applied:
   - **Validation bug on the BanCa side** (wrong sign, unknown tx_type)
     -> drop the entry.
   - **User no longer exists** (account deleted, fraud freeze) -> drop.
   - **Insufficient balance** (4004) -> drop; the player already
     bottomed out via the legacy path.
   - **Genuine transient failure that age-bombed** -> re-queue:
     ```bash
     docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" RPOPLPUSH \
       banca:failed_settle_dead banca:failed_settle
     ```
3. **Bulk re-queue everything:**
   ```bash
   while docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" \
       RPOPLPUSH banca:failed_settle_dead banca:failed_settle | grep -q .; do :; done
   ```
4. **Bulk drop everything (only after manual audit!):**
   ```bash
   docker exec sunwinkr-redis redis-cli -a "$REDIS_PASSWORD" DEL banca:failed_settle_dead
   ```

## Rollback

The worker is gated entirely behind `BANCA_REPLAY_ENABLED=false`.
Setting the var to `0` (or omitting it) drops the container into an
idle heartbeat loop that exercises no code paths. Stopping the
container at any time is safe — every item that hasn't been LPOPed yet
remains on `banca:failed_settle`.

## Constraints honoured

- Java 8, matches the rest of the backend tree.
- Reuses `VbeeCommon` for Lettuce + `ConnectionPool`; no new transitive
  deps.
- Per-tick cap (200) + per-attempt HTTP timeout (5s) bound the worst-case
  duration of a single tick.
- Memory ceiling 256MB in compose (`-Xmx128m` JVM); sidecar footprint.
- Server-side idempotency means a stuck worker plus an eventually-healed
  backend never double-applies a settle.

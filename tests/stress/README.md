# GSC seamless wallet stress harness

Exercises all hot-path GSC seamless endpoints under configurable concurrency
so we can compare client-observed and server-observed percentiles (via the
in-process `AggregatorP99Scheduler` that already records p50/p99/max per
aggregator).

## Endpoints exercised

| Endpoint               | Sig label    | Aggregator name |
|------------------------|--------------|-----------------|
| `/seamless/balance`    | `getbalance` | `GscBalance`    |
| `/seamless/withdraw`   | `withdraw`   | `GscWithdraw`   |
| `/seamless/deposit`    | `deposit`    | `GscDeposit`    |
| `/seamless/cancel`     | `cancel`     | `GscCancel`     |
| `/seamless/rollback`   | `rollback`   | `GscRollback`   |
| `/seamless/transfer`   | `transfer`   | `GscTransfer`   |
| `/seamless/pushbet`    | `pushbet`    | `GscPushBet`    |

Current harness covers the **balance / withdraw / deposit** triplet (the
BET→SETTLE round a live-casino vendor pushes per hand). Cancel / rollback /
transfer / pushbet share the same aggregator family — same instrumentation
already in place, add to `profile` if needed.

## Files

| Path                          | Purpose |
|-------------------------------|---------|
| `stress_gsc_seamless.py`      | asyncio harness, signs each request, records per-endpoint latency. |
| `run_stress.sh`               | Wrapper: pulls 50 funded accounts from MySQL, runs harness, dumps `AggregatorP99Scheduler` log tail. |
| `results/`                    | Per-run JSON + log artifacts (timestamped). |

## Usage

```bash
# Defaults: concurrency=100, duration=60s, profile=bet-round
bash tests/stress/run_stress.sh
bash tests/stress/run_stress.sh 50 60 bet-round
bash tests/stress/run_stress.sh 200 60 bet-round

# balance-only (read-heavy, no wallet mutation)
bash tests/stress/run_stress.sh 200 60 balance-only

# Raw harness — direct invocation
python3 tests/stress/stress_gsc_seamless.py \
    --host https://staging-play.sunkr.bet \
    --operator G7A1 --secret "$GSC_SECRET_KEY" --currency IDR2 \
    --product 1052 --concurrency 100 --duration 60 \
    --accounts-file tests/stress/results/accounts_*.txt \
    --profile bet-round --insecure
```

## What "concurrency" means here

One worker = one virtual GSC player; each worker loops `balance → withdraw
(BET) → deposit (SETTLED)` back-to-back. At N=100, the system sees ~3 ×
in-flight HTTP requests per worker × N → ~300 in-flight wallet calls plus
the steady-state request rate the wallet can sustain.

## Reading the output

Client-side (per endpoint):
- `n`, `avg_ms`, `p50_ms`, `p95_ms`, `p99_ms`, `max_ms`
- HTTP status counts (`http={200: ...}`) — anything other than 200 is an
  HTTP-layer error
- GSC business codes (`biz={0: success, 1000: member_not_exist, 1002: …}`).
  Code 1000 is expected when test accounts have no GSC member mapping yet.

Server-side (last few `AggregatorP99Scheduler` ticks, threshold-gated by
`GSC_P99_THRESHOLD_MS`):

```
AggregatorP99Scheduler: SLOW handler GscWithdraw count=1134 avgMs=544 p50Ms=489 p99Ms=1481 maxMs=1621
```

These are server-internal — the time `aggregator.handle(request)` took
inside the JVM — exclude network and httpx queueing.

## Staging baseline (2026-05-16)

### 50-account pool — hot-row contention dominates

| Concurrency | Throughput | Server p99 (write) | Client p99 (write) | Errors |
|-------------|------------|---------------------|---------------------|--------|
| 5           |  130 r/s   |  283 ms             |  303 ms             | 0      |
| 50          |   77 r/s   | 1481 ms             | 3283 ms             | 0      |
| 100         |   61 r/s   | 1512 ms             | 7467 ms             | 0      |
| 200         |   60 r/s   |  559 ms / 1.5 s     | 14466 ms            | 0      |

### 500-account pool — realistic player distribution

| Concurrency | Throughput | Server p99 (write) | Client p99 (write) | Errors |
|-------------|------------|---------------------|---------------------|--------|
| 100         |   60 r/s   |     **363 ms**      | 7048 ms             | 0      |
| 200         |   54 r/s   |    **430-468 ms**   | 15919 ms            | 0      |

Read path (`GscBalance`) stayed healthy throughout: p99 104–243 ms at every
load level (Hazelcast cached).

### Key finding

Server p99 at c=200 dropped from **1.5 s (50 accounts) → 468 ms (500
accounts)** — the dominant cost was **InnoDB row-lock contention on the
hot `users.vin` row**, not raw CPU or I/O. With realistic player
distribution, staging holds the SLA at c=200.

Bottleneck shifts and infra implications:

| Tier      | Role under load                       | Capacity at c=200/500 |
|-----------|----------------------------------------|------------------------|
| MySQL     | `money_account` + `users.vin` writes   | 74% CPU, healthy       |
| MongoDB   | `log_gsc_bets` inserts (one per round) | 121% CPU (>1 core)     |
| Hazelcast | Balance cache + per-user lock          | 25-30% CPU             |
| thirdparty JVM | aggregator + HTTP                 | <7% CPU                |
| backend-api    | unused on seamless path           | <1% CPU                |

**Read replicas won't help** — the path is all writes. The 50→500-account
delta proves it.

Production guidance:
- Prod MySQL spec is bigger than staging, so this baseline is conservative.
- Increase `ACCOUNT_COUNT` (or use the prod player pool) before claiming
  any future regression — synthetic 50-account runs trigger artificial
  contention not representative of live play.
- MongoDB `log_gsc_bets` is the next hottest path. If `mongo.cpu` saturates
  in prod, batch the inserts in the after-debit executor instead of one
  doc per round.

## Tuning the failure budget

`FAIL_P99_MS` (default 500) and `FAIL_ERR_PCT` (default 1.0) gate the exit
code so the script can be wired into a smoke job. Raise them when running
a deliberate over-saturation test.

## Caveats

- Test accounts are pulled from `users` where `vin >= 100000 AND is_bot=0`.
  Not all of them have a GSC member mapping — biz code 1000 is expected for
  the unmapped slice.
- Bet/settle amounts are tiny (1-50 vin), so per-account balance drifts only
  if BET and SETTLED stop being equal — both sides use the same amount, so
  steady state is net-zero.
- Bypasses Cloudflare via `--insecure` against the staging IP only when
  pointed at it; staging.sunkr.bet works either way.

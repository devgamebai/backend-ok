# Phase 5a — Latency benchmark results (2026-05-12)

**Setup:** staging cluster, all containers on shared docker networks. Benchmark run from inside `sunwinkr-banca` container hitting `http://backend-api:19082/api_backend?c=9998`.

## Sequential — n=100

| metric | value |
|---|---|
| p50 | 4.0 ms |
| p95 | 6.0 ms |
| p99 | 8.2 ms |
| max | 8.2 ms |
| avg | 4.2 ms |

## Concurrent — n=100 parallel

| metric | value |
|---|---|
| p50 | 10.5 ms |
| p95 | 25.2 ms |
| p99 | 36.6 ms |
| max | 36.6 ms |
| avg | 12.2 ms |

## SLA check

Target: p99 ≤ 100 ms per design doc.

Sequential 8.2 ms = **12× under SLA**.
Concurrent 36.6 ms = **2.7× under SLA**.

Both pass.

## Notes

- Endpoint just hits the auth path + returns. Not a full settle yet — production-shape would also do a `MoneyGateway.creditUser` / `debitUser` SQL trip. Real settle latency adds ~3-5 ms based on `MoneyGateway` benchmarks.
- Even with full settle, expected p99 ~ 50-60 ms concurrent, still inside SLA.
- Hot path in BanCa is unchanged (in-memory `Player.Cash` arithmetic). Only settle path crosses HTTP.

## Go/No-Go for Phase 5b shadow flip

| Gate | Status |
|---|---|
| Latency SLA passes | ✅ pass |
| Java `c=9998` endpoint live | ✅ |
| C# `MoneyGatewayClient` in Core.dll | ✅ |
| BanCa env vars set | ✅ |
| Source mappings WAGER_DEBIT_BANCA/WAGER_CREDIT_BANCA/EMERGENCY_BANCA | ✅ |
| Server-side dedupe via `(tx_id, source)` UNIQUE | ✅ |
| PM signoff on cash_safe/cash_silver fate | ⏳ pending |
| WAL/checkpoint for crash recovery | ⏳ pending |
| Maintenance window for migration drain | ⏳ pending |

## Recommended next step

`BANCA_USE_UNIFIED_WALLET=shadow` once PM answers items 1-3 in the pending list. Shadow runs BOTH legacy `IncEpicCash` and the new MoneyGateway HTTP call, then a reconciliation cron compares Redis `User_Cash:{id}` vs ledger `PLAYER_VIN`. After 7 days clean drift, flip to `1` and start the migration drain.

# Phase 5 — BanCa Cash Migration Design

**Status:** Design only (no code yet).
**Scope:** Retire `cgame.users.cash` / `cash_safe` / `cash_silver`. BanCa fish-shooting game reads/writes `PLAYER_VIN` via unified MoneyGateway.

---

## Current architecture (3 layers)

**Layer 1 — In-memory hot path:**
- `Player.Cash` / `Player.Profit` mutated in `GameBanCa.cs` during gameplay
- Shoot debit: `GameBanCa.cs:2127-2128` — pure arithmetic, no I/O
- Kill credits: `GameBanCa.cs:790-791`, `:918-919`, `:2365-2366`, `:2704-2706`, `:2865-2866`
- ~10 shots/sec/player, no I/O on hot path

**Layer 2 — Redis (session boundaries):**
- `RedisManager.IncEpicCash` (`RedisManager.cs:1316-1399`) — async `StringIncrementAsync` on `User_Cash:{userId}` + MySQL `bc_trans_log`
- `RedisManager.IncEpicCashCache` (`RedisManager.cs:1401-1439`) — sync fire-and-forget
- Session settle on quit: `RemovePlayer` → `IncEpicCash(id, profit, "banca", "QUIT_BC")` (`GameBanCa.cs:1848-1853`)

**Layer 3 — MySQL `cgame.users.cash` (cold storage):**
- `MySqlUser.SaveCashToDb` (`MySqlUser.cs:22-37`) — raw `UPDATE users SET cash = ?`
- Called only from payment/admin paths (NOT hot path)

**Critical insight:** Redis IS the wallet. MySQL is cold storage. The migration must drain Redis keys, not just MySQL rows.

## Cash mutation site inventory — 18 sites / 7 C# files

| # | File | Type | Frequency |
|---|---|---|---|
| 1 | `GameBanCa.cs:2127-2128` | Shoot debit (in-memory) | ~10/s/player |
| 2-6 | `GameBanCa.cs:790,918,2365,2704,2865` | Kill credits (in-memory) | per kill |
| 7 | `GameBanCa.cs:1846-1853` | Session settle → IncEpicCash | per quit |
| 8-9 | `GameBanCa.cs:1409,1491,1885` | Solo fee/end (IncEpicCashCache) | per solo |
| 10-11 | `BanCaServer.cs:284,1231` | Revive + openTx | crash/tx |
| 12-13 | `LobbyService.cs:259,269,318` | xxeng cashin/out + rollback | per transfer |
| 14 | `LobbyService.cs:757,1019,1158` | Daily bonus | per login |
| 15 | `LobbyService.cs:1614,1733,2837` | Cashout/payment | per withdrawal |
| 16 | `OneTwoThreeBoard.cs:251,413,466` | OneTwoThree bet/refund | per game |
| 17 | `LotoGame.cs:148` | Loto bet | per bet |
| 18 | `BanCaService.cs:296,336,1244...` | CMS/payment | admin |

## Vestigial columns confirmed

- `cash_safe` / `cash_silver` — read only at login (`MySqlUser.cs:181`), reads commented out in 2 of 3 `GetUserInfo` methods. **Recommend drop** with 1:1 convert to vin on migration.

## FundManager stays independent

`FundManager.cs` tracks per-fish-type house-edge pools entirely in-memory. NOT player money. Must continue working unchanged.

---

## Target architecture

Session-batch design:
1. BanCa session opens → fetch `PLAYER_VIN` balance via 1 HTTP call from Java MoneyGateway → hold in-memory + Redis `User_Cash:{id}` (kept for parity during shadow)
2. Hot path stays in-memory (`Player.Cash` arithmetic)
3. Settle to MoneyGateway: every 5s timer OR on session-end OR on big-bet threshold OR before cross-game play
4. Per-session reconciliation hourly job: compare Redis vs ledger PLAYER_VIN, alert on drift

**Latency:** Session-batch p99 ≤ 100ms (NOT per-shot 5ms which is unachievable across .NET→Java HTTP)

---

## Sub-phase plan — 8 weeks

| Sub-phase | Scope | Weeks | Risk |
|---|---|---|---|
| 5a | Latency benchmark + session-batch design finalization | 1 | Low |
| 5b | Migrate main BanCa game loop (settle calls replace IncEpicCash) | 2 | High |
| 5c | Migrate sub-games (Loto, OneTwoThree, CMS/payment paths) | 1 | Medium |
| 5d | Shadow mode dual-write + reconciliation | 1 | Medium |
| 5e | Blue/green soak 10%→50%→100% over 2 weeks, then drop cgame columns | 2 | Medium |
| Buffer | Contingency for Redis→MoneyGateway race conditions | 1 | — |
| **Total** | | **8 weeks** | |

## Sub-phase detail

### 5a — Benchmark + design (week 1)
- Measure `IncEpicCash` Redis INCRBY p99
- Measure HTTP BanCa→Java MoneyGateway p99 inside Docker network
- Target SLA: settle ≤100ms p99
- Finalize batch policy via env var `BANCA_SETTLE_INTERVAL_MS`
- **Gate:** benchmark documented, PM accepts SLA

### 5b — Main game loop (weeks 2-3)
- Hot path unchanged: `Player.Cash`/`Profit` stay in-memory
- Replace `RemovePlayer` settle: `IncEpicCash` → `WAGER_DEBIT_BANCA`/`WAGER_CREDIT_BANCA` via MoneyGateway HTTP
- Replace `BanCaServer.Revive` emergency save → MoneyGateway with `external_ref=banca_emergency:{userId}:{timestamp}`
- Replace `openTx`/`closeTx` cross-game settle
- Add 5s periodic settle timer in `BanCaServer._updateEngine` (if `abs(profit) > threshold` flush)
- 3-retry exponential backoff on timeout, then queue to `failed_settle` Redis list
- Rollback: `BANCA_USE_UNIFIED_WALLET=0` reverts to Redis path
- **Gate:** 100 consecutive sessions settle correctly

### 5c — Sub-games + admin (week 4)
- Loto (1 site), OneTwoThree (3 sites), BanCaService CMS/payment (8 sites), LobbyService payment paths
- Map all 53 `TransType` enum values to MoneyGateway transaction types
- **Gate:** all enum mappings documented and tested

### 5d — Shadow mode (week 5)
- Dual-write: MoneyGateway AND Redis IncEpicCash; compare results
- Hourly reconciliation: per user Redis `User_Cash:{id}` vs ledger PLAYER_VIN
- Alert >1 VND drift
- **Gate:** 7 days zero drift

### 5e — Blue/green soak (weeks 6-7 + buffer)
- Two BanCa containers: old (Redis-primary) + new (MoneyGateway-primary)
- Hazelcast flag `BANCA_WALLET_VERSION={1|2}` routes traffic
- Shift: 10% day 1-2, 50% day 3-5, 100% day 6-14
- Per-session reconciliation must succeed for 1000 consecutive sessions before each bump
- After 14 days at 100%: drop `cgame.users.cash`, `cash_safe`, `cash_silver`
- **Gate:** 14 days zero drift at 100%

---

## Migration data plan

1. **Drain** — set maintenance mode. For each user with `Redis User_Cash:{id} > 0`, post `BANCA_CASH_MIGRATION` transaction: DEBIT LEGACY_RECONCILIATION, CREDIT PLAYER_VIN by Redis amount
2. **Idempotency** — `external_ref = banca_migration:{userId}` (per-user, NOT per-session — per v2 addendum B7)
3. **cash_safe / cash_silver** — drop entirely, 1:1 convert to vin with separate `external_ref = banca_safe_migration:{userId}`
4. **Rollback** — snapshot `cgame` schema + Redis `User_Cash:*` keys before; restore process documented

## Open questions (PM)

1. **cash_safe / cash_silver fate** — evidence supports drop, need confirmation
2. **BanCa downtime tolerance** — 15-min maintenance window for drain step?
3. **Latency SLA** — session-batch 100ms p99 (5s in-memory divergence window) acceptable?

## Top 3 concerns

1. **Redis-as-primary transition** — Redis is the real source of truth. Drain must enumerate `User_Cash:*` keys AND cross-reference active `SetPlaying` flags. Missed keys = lost player funds.

2. **Jackpot win settle failure** — `FundManager` credits `Player.Cash` directly on jackpot. If MoneyGateway settle fails, player sees win in-game but ledger has no record.

3. **Crash recovery fragility** — `BanCaServer.Revive` (`BanCaServer.cs:260-316`) batch-flushes player profits. If BanCa crashes mid-session with MoneyGateway backend, unsettled in-memory profit is lost. **Mitigation:** add WAL (write-ahead log) or periodic Redis checkpoint of per-player accumulated profit.

## Go/No-Go criteria for Phase 5 start

1. Phases 0-4 complete and soaking
2. Latency benchmark passes (HTTP p99 ≤100ms under 100 concurrent sessions)
3. PM answers all 3 open questions
4. WAL/checkpoint design approved for crash recovery
5. C# lint scan for `UPDATE cgame.users` added to BanCa Dockerfile (per v2 addendum H2)

# Wallet → Ledger-derived Balance Migration Plan

**Authored:** 2026-05-11
**Builds on:** [LEDGER_HARDENING_ROADMAP.md](./LEDGER_HARDENING_ROADMAP.md), [DATA_STORE_ROLES_AND_TRACEABILITY.md](./DATA_STORE_ROLES_AND_TRACEABILITY.md)
**Incident reference:** [docs/incidents/2026-05-11_taixiu_freeze_and_real_player_bet_loss.md](../incidents/2026-05-11_taixiu_freeze_and_real_player_bet_loss.md)
**Status:** Plan

This document is the implementation plan for finishing the move from "denormalized `users.vin` as authoritative balance" to "ledger-derived balance with proper read/write separation." It picks up where the [Ledger Hardening Roadmap](./LEDGER_HARDENING_ROADMAP.md) left off (drift alarm, outbox, idempotency keys) and adds the missing structural pieces — the ones that today's 2026-05-11 incident proved we still need.

The plan is **phased so each phase ships independently** and is **reversible at every step**. No "big bang" migration. Each phase has its own merge → smoke → bake-time before the next starts.

---

## Why this plan exists — the failure mode it eliminates

On 2026-05-11, the GSC seamless wallet's withdraw pre-check (`GscWithdrawAggregator.balanceForUser`) rejected legitimate bets from 14+ real players over a 90-minute window with "Insufficient balance," despite their MySQL `users.vin` showing ample funds. Root cause traced to:

1. **A cache that was authoritative for the pre-check** (`if (containsKey) return cached.vin`) but updated by an inconsistent set of writers. Some money-moving paths (MoneyGateway) evicted the cache on commit; others (direct SQL refunds, admin tooling, legacy code) did not.
2. **A balance source that is denormalized** (`users.vin` is a scalar column, not a derived value of the ledger). The double-entry `money_account`/`money_entry`/`money_transaction` tables exist and are written via dual-write, but the read path still consults `users.vin`. There is no structural guarantee that they agree.
3. **A pre-check that uses the cache as authority instead of an optimization.** The actual debit (`UPDATE users SET vin = vin - ? WHERE id = ? AND vin >= ?`) is atomic and correct. The pre-check is a fast-fail UX optimization that surfaces "Insufficient balance" as a friendly error code before the UPDATE contention — but when the cache is wrong, the pre-check is also wrong, and the real UPDATE never gets to prove the cache wrong.

**Today's hot-fix** (running v3 VinPlayDAL in game-thirdparty + JVM restart) addressed the immediate symptoms via cache eviction-on-credit in MoneyGateway plus the JVM-restart-clears-near-cache side effect. But the **architectural failure mode is still present** — any future write path that bypasses MoneyGateway can recreate the bug. This plan removes the failure mode by structure, not by discipline.

---

## Target end-state, one paragraph

`money_account.balance` is the single authoritative source of every player's money balance, per currency. It is updated only by `post_money_transaction` (the stored procedure that writes balanced `money_entry` rows atomically). The legacy `users.vin` (and `xu`, `safe`, `money_vp`) columns are deprecated, eventually removed, or kept only as a denormalized read-mirror maintained by trigger. All reads go through a single `WalletReader` API that either reads `money_account.balance` directly (authoritative path, used for debit decisions) or reads from a Hazelcast cache with explicit staleness bounds (display path, used for UI / `/balance` callbacks). There is no "is the cache fresh?" question because the cache layer is structurally separated from the authority layer and the application code never confuses the two.

---

## Phases at a glance

| Phase | Title | Effort | Risk | Reversible? |
|---|---|---|---|---|
| 0 | Stop the bleeding — immediate fixes from today's incident | 1-2 days | Low | Yes |
| 1 | WalletReader API and read-site consolidation | 1-2 weeks | Low | Yes |
| 2 | MoneyGateway as the only writer + lint enforcement | 2-3 weeks | Medium | Yes |
| 3 | Ledger-derived balance — `money_account.balance` as authority | 3-4 weeks | High | Yes (dual-read) |
| 4 | Deprecate and remove `users.vin` / `xu` / `safe` / `money_vp` | 1-2 weeks | Medium | Yes |
| 5 | Read replica deployment + cache layer redesign | 1-2 weeks | Low | Yes |
| 6 | Operational hardening — reconciliation, alerting, runbooks | Ongoing | — | — |

Total effort: ~8-12 weeks of focused work, but each phase delivers value independently and the worst phase (3) has well-defined exit criteria.

---

## Phase 0 — Stop the bleeding (1-2 days)

**Goal:** Make today's bug class impossible to recur, even if no other phase ships.

### 0.1 — `GscWithdrawAggregator.balanceForUser` bypasses cache

`backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/seamless/gsc/GscWithdrawAggregator.java:1202`

Change `balanceForUser` to skip the Hazelcast cache entirely and always call `doReadBalance` (DB-direct SELECT). The cache check saves <1ms per request and creates the exact failure mode that bit us today. The single indexed DB read replaces it with no observable latency penalty (sub-millisecond on a PK lookup).

```java
// Was:
//   if (userMap.containsKey(memberAccount)) { return cached.vin; }
//   else { return doReadBalance(memberAccount).newBalance; }
// Becomes:
//   return doReadBalance(memberAccount).newBalance;
```

Apply the same change to `GscDepositAggregator`, `GscCancelAggregator`, `GscRollbackAggregator`, `GscTransferAggregator`. All of them have a `balanceForUser` method that follows the same pattern (verified by grep). The withdraw path is the only one that surfaces the bug on bad data, but all five paths have the same latent risk.

**Exit criteria:** Zero "Insufficient balance" rejections in `gsc_event_log` over 24h for players whose MySQL `vin` >= attempted `bet_amount * exchangeRateIn(currency)`.

### 0.2 — Cache-DB drift alarm

New cron at `scripts/cache-drift-alarm.sh`, runs every 60 seconds:

```sql
-- Sample 100 random users that are in HZ cache, compare to MySQL.
-- Alert if more than 1% disagree by more than 1 unit (rounding tolerance).
```

Implementation: write a small Java tool in `scripts/CacheDriftCheck.java` that connects to HZ as a client, iterates a sample of the `users` map keys, fetches `vin` from each `UserCacheModel`, compares to a JOIN against MySQL, and prints stats. Cron pipes output to Telegram alerter on drift > 1%.

This catches both today's failure mode and any future regression instantly, with a one-minute detection window.

**Exit criteria:** Alarm is live; one test drift (intentionally evict + UPDATE without re-cache) triggers a page within 60s.

### 0.3 — Forbid direct SQL writes on `users.vin`

Add a `:checkNoDirectVinUpdate` Gradle task analogous to the existing `:checkNoCurrentMoneyTrap` task (per [CLAUDE.md](../../CLAUDE.md), already wired to every subproject's `compileJava`):

```bash
# scripts/check-no-direct-vin-update.sh
# Fails the build if any Java file outside MoneyGateway.java contains:
#   UPDATE\s+users\s+SET\s+(vin|xu|safe|money_vp)\b
```

Apply to all subprojects. Add a `legacy_allow` list (similar to the existing pattern) to grandfather in the few legitimate cases (admin reset-xu tool, etc.) with explicit comments explaining why.

**Exit criteria:** Build fails if a new direct UPDATE is introduced. Existing direct UPDATEs are either migrated to MoneyGateway or explicitly allow-listed with a TODO and ticket reference.

### 0.4 — Document the incident postmortem and feed lessons into runbooks

Already done — see [2026-05-11_taixiu_freeze_and_real_player_bet_loss.md](../incidents/2026-05-11_taixiu_freeze_and_real_player_bet_loss.md). One follow-up: link from `RUNBOOK_REDIS_DOWN.md` (HZ analog) so the next "balance shows wrong" report has a faster diagnosis path.

---

## Phase 1 — WalletReader API and read-site consolidation (1-2 weeks)

**Goal:** Every balance read in the codebase goes through ONE method, with explicit authoritative-vs-display semantics. After this phase, no code anywhere does `userMap.get(nick).getVin()` directly.

### 1.1 — Introduce `WalletReader` interface

New file `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/wallet/WalletReader.java`:

```java
public interface WalletReader {
    /**
     * Authoritative balance read. Hits primary DB, no cache.
     * Use for: debit decisions, settlement, audit, reconciliation, refund eligibility.
     * Latency: ~1ms (indexed PK lookup).
     * Staleness: zero.
     */
    long getAuthoritativeBalance(long userId, Currency currency);
    long getAuthoritativeBalance(String nickname, Currency currency);

    /**
     * Display balance read. May use cache or replica. Bounded staleness.
     * Use for: UI display, /balance API callbacks to providers, lobby HUD, history pages.
     * Latency: ~0.1ms (cache hit) to ~5ms (replica fallback).
     * Staleness: up to 2 seconds.
     */
    long getDisplayBalance(long userId, Currency currency);
    long getDisplayBalance(String nickname, Currency currency);

    /**
     * Bulk display read for list endpoints. Single round-trip.
     */
    Map<Long, Long> getDisplayBalances(Collection<Long> userIds, Currency currency);
}
```

Two implementations:

- `MySqlAuthoritativeWalletReader` — straight `SELECT vin FROM users WHERE id = ?` against the primary. No caching layer.
- `HzCachedDisplayWalletReader` — HZ users map read with explicit TTL + drift detection. Falls back to authoritative on miss.

For Phase 1, both implementations read `users.vin` (or per-currency column). Phase 3 will swap the read source to `money_account.balance` behind this interface without touching callers.

### 1.2 — Inventory all balance readers

Goal: list every call site that reads a balance, classify it as authoritative or display.

Run:

```bash
# Direct field access
grep -rn 'userMap.get\|\.getVin()\|\.getXu()\|\.getSafe()' --include='*.java' backend-master/
# Existing service methods
grep -rn 'getMoneyUserCache\|userService\.getUser' --include='*.java' backend-master/
# SQL reads
grep -rn 'SELECT.*\bvin\b.*FROM users' --include='*.java' backend-master/
```

Each call site gets categorized:

- **AUTH** — debit eligibility check, settlement, refund decision. Must use `getAuthoritativeBalance`.
- **DISPLAY** — UI, GSC `/balance` callback, lobby, history, admin list pages. Use `getDisplayBalance`.
- **INTERNAL** — already inside MoneyGateway / wallet logic, unchanged.

Estimate: 80-120 call sites across the codebase. Spreadsheet tracked in `docs/architecture/wallet-readers-inventory.csv` (created in this phase).

### 1.3 — Migrate call sites by package, one PR per service

Suggested order (by risk, low-to-high):

1. Admin / list-page readers (lowest risk — display-only)
2. Lobby + HUD pushes (display)
3. Game-specific balance reads (TaiXiu, Sicbo, Slot, BauCua, etc.) — these are AUTH at bet time
4. GSC seamless aggregators — already done in Phase 0 for withdraw; do the rest here
5. Agency / credit-wallet readers
6. Logging / reporting paths

Each PR:

- Replaces `userMap.get(nick).getVin()` with `walletReader.getDisplayBalance(nick, Currency.VIN)` (or AUTH)
- Adds a unit test that asserts the right method is called
- Touches at most one service package to keep blast radius small

### 1.4 — Smoke test and drift verification

After each PR ships:

- Phase 0's drift alarm shows steady state (zero drift)
- A new dashboard counts calls to `getAuthoritativeBalance` vs `getDisplayBalance` per service. AUTH-heavy services should be high-volume display + occasional auth; if a service is calling AUTH on every request, it's likely a classification error.

**Exit criteria:**

- 100% of balance reads go through `WalletReader`
- The grep at the start of 1.2 returns zero hits in production code (only in `WalletReader` impls)
- Drift alarm has been quiet for 7 consecutive days

---

## Phase 2 — MoneyGateway as the only writer + lint enforcement (2-3 weeks)

**Goal:** Every write that changes a player's balance goes through `MoneyGateway`. Direct `UPDATE users SET vin ...` is impossible to add without a build failure.

### 2.1 — Inventory all writers

```bash
grep -rn 'UPDATE\s\+users\s\+SET' --include='*.java' --include='*.sql' --include='*.xml' backend-master/
grep -rn 'setVin\|setXu\|setMoney(' --include='*.java' backend-master/
grep -rn 'creditUser\|debitUser\|updateMoney' --include='*.java' backend-master/
```

Expected hits: roughly 30-50 sites. Classify each as:

- **THROUGH_GATEWAY** — already calls `MoneyGateway.creditUserWithCumulative` (or one of its aliases). No work.
- **LEGACY** — calls `userService.updateMoney` (older path that pre-dates MoneyGateway). Migrate.
- **DIRECT_SQL** — bypasses everything (operator scripts, some admin tools, some legacy migrations). Migrate or allow-list.
- **CACHE_ONLY** — touches the HZ cache directly (e.g., `SecurityServiceImpl.login` populates the model). These need a different treatment — the cache populate is fine, but they should not write a balance VALUE that disagrees with the DB.

### 2.2 — Promote `MoneyGateway.creditUserWithCumulative` to the canonical API

The signature is already good. The job is to make it cover the cases LEGACY callers needed but didn't have:

- **Multi-currency** — already added (`col` parameter is `"vin"` / `"xu"` / `"safe"` / `"money_vp"`). Verify all callers pass it.
- **Bulk operations** — needed for refund scripts, mass adjustments. Add `creditUsersWithCumulative(List<CreditRequest>)` that opens one transaction per request but reuses a connection.
- **Cache evict** — already in place since today's hot-fix. Wrap in a try/catch so cache failure doesn't fail the SQL commit.
- **Outbox emit** — per Phase 2 of the existing LEDGER_HARDENING_ROADMAP, insert into `money_outbox` in the same transaction.

### 2.3 — Migrate LEGACY callers

For each LEGACY site, replace `userService.updateMoney(...)` with `MoneyGateway.creditUserWithCumulative(...)`. Key differences to handle in migration:

- `updateMoney` returned `MoneyResponse` with `isSuccess()` + `getCurrentMoney()`. `creditUserWithCumulative` returns `CreditResultWithCumulative` with `newBalance` and `newTotal`. Map at call site.
- `updateMoney` did its own RMQ publish for `queue_log_money_user`. `MoneyGateway` will route through the outbox in Phase 2C of the existing roadmap — until then, keep the legacy publish call alongside until outbox lands.
- Some callers (e.g., `TaiXiuHoanTien` refund on bet-window-close in `MGRoomTaiXiu.betTaiXiu`) need the *atomic* version that pairs a bet + refund as a single logical operation. Document why; do not split into two MoneyGateway calls without idempotency keys that link them.

### 2.4 — CI lint: `checkNoDirectWalletWrite`

Promote the script from Phase 0.3 into a proper Gradle task that runs on every commit:

```gradle
task checkNoDirectWalletWrite(type: Exec) {
    commandLine 'bash', "$rootDir/scripts/check-no-direct-wallet-write.sh"
}
compileJava.dependsOn checkNoDirectWalletWrite
```

Pattern matched:

- `UPDATE\s+users\s+SET\s+(vin|xu|safe|money_vp)\b` in `.java`/`.sql`/`.xml`
- `\.setVin\(|\.setXu\(|\.setSafe\(|\.setMoneyVP\(` in `.java` (outside `UserCacheModel` itself and `MoneyGateway`)

Allow-list mechanism: a comment marker `// legacy_allow: WALLET_WRITE — <ticket>` on the offending line, with the ticket required to be open in the tracker.

### 2.5 — Operator script migration

The refund script I ran on 2026-05-11 (direct SQL UPDATE + INSERT INTO money_gateway_log) is exactly the pattern this phase forbids. Replace it with a tiny CLI tool `scripts/wallet-admin-tool.jar` that:

- Takes a refund spec (user, amount, reason, tx_id)
- Calls `MoneyGateway.creditUserWithCumulative` via the running backend-api HTTP admin endpoint OR via a direct Java invocation that loads the same MoneyGateway code
- Confirms commit + cache eviction
- Emits a structured audit row

Same tool covers: mass refunds, manual ADMIN_TOPUP, balance corrections, currency adjustments.

**Exit criteria:**

- All LEGACY writers migrated (lint task is green on a fresh checkout)
- The `wallet-admin-tool.jar` is the documented refund / topup path
- Phase 0's drift alarm has been quiet for an additional 7 days post-migration

---

## Phase 3 — Ledger-derived balance, the structural fix (3-4 weeks)

**Goal:** `money_account.balance` becomes the authoritative read source. `users.vin` becomes a denormalized mirror, maintained by trigger or scheduled job, but no longer consulted for the debit decision. After this phase, the cache → DB → ledger triangle resolves to a single straight line: ledger → balance, full stop.

This is the highest-effort and highest-risk phase. We split it into three dual-read stages so each step is independently verifiable and rollback-able.

### 3.1 — Dual-read for verification (1 week)

Modify `MySqlAuthoritativeWalletReader` (from Phase 1.1):

```java
public long getAuthoritativeBalance(long userId, Currency currency) {
    long legacyValue = readUsersVin(userId, currency);
    long ledgerValue = readMoneyAccountBalance(userId, currency);
    if (legacyValue != ledgerValue) {
        driftCounter.increment(currency);
        logger.warn("Phase-3.1 dual-read drift: user=" + userId + " currency=" + currency
                + " legacy=" + legacyValue + " ledger=" + ledgerValue);
        // Conservative: return legacy until drift is at zero
        return legacyValue;
    }
    return legacyValue;
}
```

The dual-read does not change behavior. It just measures how often `users.vin` and `money_account.balance` already agree. After Phase 2 closed all direct-write loopholes, they should agree on every new transaction. Historical drift comes from pre-migration discrepancies and from the fact that the ledger dual-write isn't 100% complete for all sources yet.

**Backfill task in parallel:** Reconcile historical balances by running:

```sql
INSERT INTO money_entry (..., direction, amount, ...)
SELECT ... derive from the gap between users.vin and SUM(money_entry.amount) per account ...
```

This is a one-shot correction transaction per account with `transaction_type = 'BACKFILL_RECONCILIATION'`. The existing `v_money_account_drift` view is the input. Daily reconciliation cron (already running per LEDGER_HARDENING_ROADMAP) catches new drift; this backfill closes the historical gap.

**Exit criteria:**

- Drift counter for all currencies < 0.01% over 7 consecutive days
- `v_money_account_drift` shows < 10 rows total (all accounts converged)
- Per-source breakdown of drift (deposit, withdraw, game, agent transfer) shows which paths still drift and prioritizes them for the next phase

### 3.2 — Cutover: ledger-derived as the new authoritative source (1 week)

Once drift is at zero for 7 days, flip `MySqlAuthoritativeWalletReader` to return the LEDGER value instead of the legacy column. Behind a feature flag (`MONEY_READ_AUTHORITY=ledger` vs `=legacy`).

```java
public long getAuthoritativeBalance(long userId, Currency currency) {
    if (Flags.MONEY_READ_AUTHORITY.equals("ledger")) {
        return readMoneyAccountBalance(userId, currency);
    } else {
        return readUsersVin(userId, currency);
    }
}
```

Rollout: enable for staging first, soak for 24h, then 1% prod, then 10%, then 100%. Drift counter from 3.1 continues to track divergence. The feature flag lives in `cacheConfig` map so it's per-environment-controllable without redeploying.

**Exit criteria:**

- 7 days at 100% on ledger reads with zero drift incidents
- All GSC seamless flows (withdraw, deposit, balance, cancel, rollback) verified end-to-end with ledger reads

### 3.3 — Trigger-maintained `users.vin` (1-2 weeks)

`users.vin` stays as a column (some old code paths and admin tooling still read it directly outside of `WalletReader`, despite Phase 1's best efforts — there's always a few). Make it self-healing via DB trigger:

```sql
DELIMITER //
CREATE TRIGGER trg_money_account_to_users_vin
AFTER UPDATE ON money_account
FOR EACH ROW
BEGIN
  IF NEW.currency = 'vin' AND NEW.balance != OLD.balance THEN
    UPDATE users SET vin = NEW.balance WHERE id = NEW.owner_user_id;
  END IF;
END//
DELIMITER ;
```

Similar triggers for `xu`, `safe`, `money_vp`. After this, `users.vin` is read-only from application code and structurally cannot disagree with `money_account.balance` for more than the duration of one transaction.

**Side effect:** The cache eviction discipline of Phase 0/2 is no longer strictly required for correctness — only for read-after-write freshness latency. We keep it for performance reasons, but a bug that fails to evict can no longer cause incorrect rejections at the pre-check (because the pre-check now reads `money_account.balance` directly via the authoritative path).

**Exit criteria:**

- Trigger is in place; any direct UPDATE to `money_account.balance` propagates to `users.vin` within the same transaction
- 7 days with zero drift between trigger source and target

---

## Phase 4 — Deprecate and remove legacy columns (1-2 weeks)

**Goal:** `users.vin` (and `xu`, `safe`, `money_vp`) are removed from the schema entirely. Their job — "fast read of current balance" — is done by `money_account.balance` directly or by the display-cache layer.

This is conceptually simple but requires every reader to actually be on the `WalletReader` API (Phase 1 must be 100% complete) and every writer to be on `MoneyGateway` (Phase 2 must be 100% complete).

### 4.1 — Add a runtime "legacy column read" warning

Before dropping the columns, prove no one is reading them. Add a MySQL audit log or wrap the connection pool with a query interceptor that logs any `SELECT ... vin ... FROM users ...` outside of allow-listed positions (`WalletReader` implementations, trigger code, reconciliation views).

Run for 14 days. Zero hits = safe to drop.

### 4.2 — Drop the columns

```sql
ALTER TABLE users DROP COLUMN vin;
ALTER TABLE users DROP COLUMN xu;
ALTER TABLE users DROP COLUMN safe;
ALTER TABLE users DROP COLUMN money_vp;
```

Run on a single PR with a corresponding `users` model update across all JVMs (the field is removed from `UserModel.java` and `UserCacheModel.java` simultaneously). Requires coordinated rolling restart of all JVMs.

**Rollback:** If the column-drop migration fails post-deploy, immediately re-add the column and run the trigger from Phase 3.3 backwards (`money_account.balance` → `users.vin`) to repopulate, then revert the model change. The trigger is the safety net that makes Phase 4 reversible.

### 4.3 — Banned API #2 (analogous to `getCurrentMoney`)

Per [CLAUDE.md](../../CLAUDE.md), `UserModel.getCurrentMoney`/`setCurrentMoney` were banned via `:checkNoCurrentMoneyTrap`. Add `:checkNoLegacyVinAccess`:

```bash
# Fails the build if any code references UserCacheModel.getVin(),
# UserModel.getXu(), etc. (the removed-column accessors).
```

**Exit criteria:**

- Columns dropped without incident
- Banned-API check is wired and green
- 30 days of normal operation post-drop

---

## Phase 5 — Read replica deployment + cache layer redesign (1-2 weeks)

**Goal:** GSC `/balance` callbacks (and other high-volume display reads) come from a MySQL read replica, dropping primary read load by ~80%. The Hazelcast `users` map becomes a per-JVM near-cache with strict TTL and is reserved for non-balance fields (profile, status, VIP tier, etc.).

### 5.1 — Provision the replica

Add a second MySQL container to the compose stack (`docker-compose.database.yml`):

```yaml
mysql-replica-1:
  image: mysql:8.4
  command: --server-id=2 --read-only=1 --relay-log=replica-relay-bin
  # standard async replication from primary
```

Latency target: < 200ms replication lag P99. Monitor via `SHOW REPLICA STATUS` exported to Prometheus.

### 5.2 — Connection pool — second pool for replica

`db_pool.properties` adds `mysqlpool_replica.url=jdbc:mysql://mysql-replica-1:3306/vinplay?...&readOnly=true`.

`ConnectionPool.getReplicaConnection()` returns from this pool. Calling site decides which to use — `WalletReader.getDisplayBalance` uses replica; `WalletReader.getAuthoritativeBalance` always uses primary.

### 5.3 — Replication-lag-aware fallback

If `SHOW REPLICA STATUS Seconds_Behind_Source > 2`, the replica is too stale; `getDisplayBalance` automatically falls back to primary. Health check runs every 10 seconds and updates an in-process `replicaIsFresh` boolean. No per-request lag check (too expensive).

### 5.4 — Redesign the Hazelcast users map

Drop balance fields from the cached `UserCacheModel`. The cache holds: nickname, user_id, status, dai_ly tier, vip_point, is_bot, login_otp — all the things that change rarely.

The cache becomes safe-by-construction for the balance class of bugs: a stale cache simply doesn't contain a balance, so it can't cause a wrong-balance decision. The balance always comes from MySQL (primary for AUTH, replica for DISPLAY).

This is a structural simplification — the `users` map shrinks (no balance fields), the bug class disappears.

**Exit criteria:**

- Replica lag P99 < 200ms over 7 days
- GSC `/balance` callbacks served from replica with < 5ms response time at P50
- Primary read load dropped by 70%+ for the balance read path
- Hazelcast `users` map no longer contains balance fields

---

## Phase 6 — Operational hardening (ongoing)

Not a one-shot phase; this is a set of standing investments that keep the system healthy.

### 6.1 — Reconciliation as a continuous job

The existing `ledger-drift-alarm.sh` (per LEDGER_HARDENING_ROADMAP fix #1) checks `v_money_account_drift` every 15 minutes. Tighten to every 60 seconds once Phase 3 closes the structural gap. Auto-correct small drifts (< 100 units per account, single-currency, with audit trail) instead of paging; page only on systemic drift or sum-zero violations.

### 6.2 — Per-aggregator dashboard

One dashboard per money-moving aggregator (GscWithdrawAggregator, MoneyGateway, DepositApprovalService, BankWithdrawalApprovalService) showing:

- Requests per second
- Error rate (decomposed by error class)
- Latency P50 / P99
- Drift counter (when applicable)
- Cache hit / miss / eviction rate

Grafana boards at `docs/grafana/wallet-aggregators.json` (new file, version-controlled).

### 6.3 — Synthetic monitoring

Every 5 minutes, a synthetic test user deposits 1 KRW, places a 1 KRW bet on each game (TaiXiu, Sicbo, BauCua, MD5, GSC product 1002 / 1006 / 1085), settles, and verifies the resulting balance matches expectation. Alerts on any deviation.

This catches:

- Cache drift before a real player sees it
- New code paths that bypass the wallet API
- Provider integrations that desync
- Configuration regressions

### 6.4 — Quarterly disaster recovery drill

Once per quarter, in staging: drop the HZ cache entirely, restart all JVMs, replay a 1-hour traffic profile. Verify zero correctness incidents and recovery time < 5 minutes. Document the drill in `docs/runbook/dr-drills/`.

### 6.5 — Documentation: keep the source of truth current

This plan + LEDGER_HARDENING_ROADMAP + DATA_STORE_ROLES_AND_TRACEABILITY are the architecture trunk. Update them as phases complete. The "Status" header at the top of each gets updated, and the corresponding section in each phase moves from "Plan" to "Shipped (date)".

---

## Risk register

| Risk | Mitigation |
|---|---|
| **Phase 2 misses a writer**, direct SQL still exists in some path we didn't grep | Phase 0.2 drift alarm catches it within 60s; Phase 0.3 lint catches new ones at build time |
| **Phase 3.1 dual-read shows persistent drift** that can't be backfilled | Investigate per-source; expect ~5 LEGACY paths to surface, each is a 1-2 day fix; do not advance to 3.2 until drift is zero |
| **Phase 3.2 cutover causes incorrect rejections** | Feature flag rollback in < 1 minute; both reads are still happening, just the return value flips |
| **Phase 4 column drop reveals a missed reader** | Pre-flight audit in 4.1 with 14-day soak; if a reader is found, the column add-back + trigger from 3.3 reverses it cleanly |
| **Phase 5 replica lag spike** during a high-volume window | Auto-fallback to primary in 5.3; degraded throughput but correct results |
| **Phase 5 connection pool exhaustion** if replica fallback hammers primary | Separate pool sizing; primary pool is reserved for AUTH, can't be drained by display traffic |
| **Coordinated rolling restart for Phase 4 column drop** has a window where some JVMs have the new model and some don't | Drop the column AFTER all JVMs are on the new code (column read of dropped column would throw; do the code rollout first, then the schema migration) |

---

## Per-phase rollback procedures

| Phase | Rollback action | Recovery time |
|---|---|---|
| 0.1 | Re-enable cache pre-check (one-line revert in `balanceForUser`) | < 1 minute |
| 0.2 | Disable the cron | Instant |
| 0.3 | Remove the Gradle task | Next build |
| 1.x | Each PR is independently revertible; `WalletReader` interface stays in place | < 5 minutes per PR |
| 2.x | Revert the migration PR; lint task can be temporarily disabled while LEGACY is reintroduced | < 5 minutes per PR |
| 3.1 | Disable dual-read flag | Instant |
| 3.2 | Flip `MONEY_READ_AUTHORITY=legacy` in `cacheConfig` | < 1 minute |
| 3.3 | `DROP TRIGGER trg_money_account_to_users_vin` | < 1 minute |
| 4.2 | Re-add column, run reverse trigger backfill | ~30 minutes per 10M accounts |
| 5.1-5.3 | Disable replica use via config flag; all reads go to primary | Instant |
| 5.4 | Re-add balance fields to `UserCacheModel`, redeploy | One restart cycle |

---

## Effort and timeline

Assuming one full-time engineer + ~25% review time from a senior:

| Phase | Calendar |
|---|---|
| 0 | Week 1 (Mon-Wed) |
| 1 | Week 1-3 |
| 2 | Week 3-5 |
| 3 | Week 5-9 (longest single phase, includes 7-day bake periods) |
| 4 | Week 9-11 |
| 5 | Week 11-13 |
| 6 | Concurrent + ongoing |

Total: ~13 weeks, ~3 months, for full delivery. Useful intermediate milestones:

- **End of Week 1** — today's bug class is structurally impossible (Phase 0)
- **End of Week 5** — single write API, no more bypass possible (Phase 2)
- **End of Week 9** — ledger is authoritative, denormalized column is read-only (Phase 3)
- **End of Week 13** — denormalized column gone, replicas in place, system is at architectural target (Phase 4 + 5)

---

## Appendix A — file-level change map

Files that will be added, modified, or deprecated, organized by phase. Useful for sizing reviews and pre-flighting merge conflicts.

### Phase 0
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/seamless/gsc/Gsc*Aggregator.java` × 5 — `balanceForUser` modified
- `scripts/cache-drift-alarm.sh` (new)
- `scripts/CacheDriftCheck.java` (new)
- `scripts/check-no-direct-wallet-write.sh` (new, will move to Phase 2.4)
- `docs/incidents/2026-05-11_*.md` (created today)

### Phase 1
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/wallet/WalletReader.java` (new)
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/wallet/MySqlAuthoritativeWalletReader.java` (new)
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/wallet/HzCachedDisplayWalletReader.java` (new)
- All `*Module.java`, `*Processor.java`, `MGRoom*.java`, `Gsc*Aggregator.java` — call site migrations
- `docs/architecture/wallet-readers-inventory.csv` (new, tracking spreadsheet)

### Phase 2
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java` — bulk + cache evict already done
- `backend-master/VinPlayUserCore/src/main/java/com/vinplay/usercore/service/impl/UserServiceImpl.java` — `updateMoney` and `updateMoneyFromAdmin` deprecated, replaced by MoneyGateway delegates
- `scripts/wallet-admin-tool/` (new module)
- `backend-master/build.gradle` — wire `:checkNoDirectWalletWrite`

### Phase 3
- `backend-master/install/migrations/V0XX__money_account_balance_authority.sql` (new)
- `backend-master/install/migrations/V0XX__users_vin_trigger.sql` (new)
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/wallet/MySqlAuthoritativeWalletReader.java` — switch read source
- Reconciliation backfill script + audit log table

### Phase 4
- `backend-master/install/migrations/V0XX__drop_users_money_columns.sql` (new)
- `UserModel.java`, `UserCacheModel.java` — fields removed
- `:checkNoLegacyVinAccess` (new)
- All call sites that still reference `.getVin()` etc. — final cleanup

### Phase 5
- `docker-compose.database.yml` — replica container
- `backend-master/install/replication-setup.sql` (new)
- `db_pool.properties` — replica pool
- `ConnectionPool.java` — `getReplicaConnection()`
- `HzCachedDisplayWalletReader.java` — replica-aware reads with lag fallback

### Phase 6
- `docs/grafana/wallet-aggregators.json` (new)
- `scripts/synthetic-balance-check.sh` (new)
- `docs/runbook/dr-drills/wallet-cache-flush.md` (new)

---

## Appendix B — Open questions

These are decisions deferred to the implementing team. Each unlocks specific phases.

1. **Replica geography** — single-region async replica vs cross-region. For Phase 5, the simple answer is same-region async. Cross-region adds complexity and tail latency but improves disaster posture. Defer to ops team.
2. **Hazelcast retirement** — once Phase 5 removes balance from the HZ users map, the map only holds infrequently-changing user metadata. The team should evaluate whether HZ is still pulling its weight or whether a simpler in-process Caffeine cache + DB would do. Defer to post-Phase-5 review.
3. **xu / safe / money_vp parity** — this plan treats them as equivalent to vin. The reality is they each have slightly different semantics (xu is a different currency, safe is the secure wallet, money_vp is VIP points). Phase 3.1's drift analysis will surface any per-column quirks. If they differ materially, split into per-currency sub-phases.
4. **Cross-service idempotency** — Phase 2.2 mentions the outbox + idempotency-key plan from LEDGER_HARDENING_ROADMAP. The two plans interact at MoneyGateway. If idempotency-key lands first, MoneyGateway gains a `Idempotency-Key` parameter; if outbox lands first, MoneyGateway gains an `outbox_emit` step. Either order works; document the chosen sequence in the implementation kickoff.
5. **GSC re-onboarding** — once we hit the architectural target, we may want to re-validate the GSC seamless wallet integration end-to-end with their certification team. Their test suite includes deliberate stale-balance scenarios; we'd pass them on the new architecture but should formalize the certification.

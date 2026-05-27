# Ledger Hardening Roadmap

Tracks the gap between our current double-entry implementation (`money_account` / `money_entry` / `money_transaction` + `post_money_transaction` SP) and the principles in [`ledger-system-tech-stack-plan.pdf`](./ledger-system-tech-stack-plan.pdf).

**Status as of 2026-05-06** — landed in this same change:

| # | Fix | State |
|---|-----|-------|
| 1 | Drift alarm on `v_money_account_drift` | **DONE** — `scripts/ledger-drift-alarm.sh` runs every 15 min via cron, baselines at 80, pages on new drift or any unbalanced transaction |
| 2 | `double` → `BigDecimal` in TRON crypto path | **DONE** — `TronGatewayClient.createWithdrawal`, `CryptoWithdrawalApprovalService`, `TelegramCryptoWithdrawNotifier`, `WithdrawCryptoProcessor`, `CryptoDepositEventProcessor`. Display-only `getDouble("amount_usdt")` calls in 7 list/history processors left as-is (no money math, no leak risk) |
| 3 | Per-transaction sum=0 invariant | **DONE** — already enforced inside `post_money_transaction` SP (rejects with `'UNBALANCED_ENTRIES'`). MoneyLedger.java only writes via `CallableStatement`. Direct INSERT on `money_entry` would bypass — relies on app discipline + `v_money_unbalanced_transactions` safety-net view |
| 5 | GSC dual-write to ledger | **DONE** — added `GSC_DEBIT` / `GSC_CREDIT` / `GSC_HOURLY_DEBIT` mappings in MoneyGateway. Stops the `no ledger type for source=GSC_*` WARN spam after next backend-api rebuild |
| 4 | Money outbox table | **PARTIAL** — table created (`money_outbox` migration applied). Producer/poller wiring is the bigger work (below) |

Everything else is multi-week project work and is documented below.

---

## Fix #4 (continued) — outbox producer + poller

**Status:** Phase 1 (table) done. Phase 2 (producer + poller) = ~1 week.

### Phase 2A — Producer

Modify `post_money_transaction` SP to also `INSERT INTO money_outbox` in the same transaction:

```sql
INSERT INTO vinplay.money_outbox (event_type, aggregate_id, payload)
VALUES (CONCAT('LEDGER_', p_transaction_type), p_transaction_id,
        JSON_OBJECT(
          'transaction_id', p_transaction_id,
          'transaction_type', p_transaction_type,
          'external_ref', p_external_ref,
          'initiator_user_id', p_initiator_user_id,
          'entries', p_entries_json,
          'metadata', p_metadata
        ));
```

Place between the `INSERT INTO money_transaction` and the COMMIT, after the entries loop. Both rows become atomic.

### Phase 2B — Poller

New service `api/money-outbox-publisher/` (separate JVM, single instance), polls every 250ms:

```sql
SELECT id, event_type, aggregate_id, payload
  FROM money_outbox
 WHERE published_at IS NULL
 ORDER BY id
 LIMIT 500
 FOR UPDATE SKIP LOCKED;
```

For each row: publish to Redis Stream `stream:money-events`, then `UPDATE money_outbox SET published_at = NOW(6) WHERE id = ?`. On failure increment `publish_attempts`, write `last_error`, retry exponential backoff, page after 10 attempts.

`SKIP LOCKED` is MySQL 8+; we have it.

### Phase 2C — Migrate existing producers

Replace direct `RMQApi.publishMessage("queue_log_money_user", …)` calls inside money paths with "ledger write → outbox row → poller publishes". Migrate one queue at a time (ties into existing S1 Redis-streams migration, task #254).

### Why this matters

Today, after the wallet update, business code does `try { RMQApi.publishMessage(…) } catch { logger.warn(…) }`. If RMQ rejects the message, the wallet moved but downstream (commission, websocket push, notifications) never saw the event. Outbox eliminates that loss.

---

## Fix #6 — Idempotency-Key header

**Effort:** ~2-3 weeks (FE + BE coordinated). **Status:** not started.

### Backend changes (independent of FE)

1. New table `idempotency_records` (key UUID PK, response_body JSON, response_status INT, created_at, expires_at — TTL 24h).
2. `IdempotencyFilter` Jetty filter on every state-changing endpoint (`POST /api?c=*` writes). On request:
   - If header `Idempotency-Key` present and `idempotency_records.key` matches → replay cached response.
   - If absent → proceed normally; on 2xx response, store body keyed by header.
3. Per-endpoint metadata in `api_portal.xml` / `api_backend.xml` annotating which commands are state-changing (which need idempotency).

### Frontend coordination

Web (Next.js), mobile (Cocos), agency (Laravel) must all generate UUID v4 per state-changing request. Retry on network error with the same key. ~50 call sites.

### Why deferred

Without FE generating keys, the BE cache is dead infra — no current call site sends the header. Shipping BE-only doesn't reduce double-charge risk.

---

## Fix #7 — Move slow work out of webhook handlers

**Effort:** ~2 weeks. **Status:** not started.

### Current behavior

GSC seamless callbacks (`/gsc/v1/api/seamless/withdraw`, `/deposit`, `/balance`) do everything inline:
1. Verify HMAC signature
2. Parse + validate batch
3. SELECT user, debit/credit MoneyGateway
4. Publish RMQ event for vbee processing
5. Return 200

Latency observed in debug log: 600-1200ms per call. Spec target: <50ms. The slow-handle WARNs we saw correlate with `qtp*` thread saturation.

### Target

```
[Webhook handler — sync, <50ms]
  1. Verify HMAC
  2. Validate batch shape (no DB)
  3. INSERT INTO gsc_inbound_events (...)
  4. Return 200

[Async processor — separate thread pool]
  1. Poll gsc_inbound_events for unprocessed
  2. Run the existing aggregator dispatch
  3. Update event row to processed
```

GSC's wager visibility for the player becomes async. Acceptable — players see balance via a separate WebSocket push (already wired) which fires on the actual MoneyGateway commit.

### Why deferred

Touching the GSC callback path is high-risk; we'd need:
- Shadow-mode rollout: process events both inline AND async, compare results, cut over once parity is proven.
- 2-week soak.
- Ops runbook for "events queue depth > N → alert + manual drain".

Not landable in a single session.

---

## Fix #8 — Extended state machine

**Effort:** ~1 week. **Status:** not started.

### Current

`money_transaction.status` enum: `'PENDING','POSTED','REVERSED'`.

### Target (per spec §4.8)

`'INITIATED' → 'PENDING' → 'POSTED'/'CONFIRMED'`, plus `'FAILED'`, `'REVERSED'`. State-transition guard enforced in DB trigger or service-layer assertion.

### Migration

```sql
ALTER TABLE money_transaction
  MODIFY COLUMN status ENUM('INITIATED','PENDING','POSTED','FAILED','REVERSED')
  NOT NULL DEFAULT 'POSTED';
```

Existing 21,917 rows are all `POSTED` — safe.

Add trigger `check_money_transaction_status_transition` that rejects e.g. `POSTED → PENDING`. Keep the SP using `'POSTED'` as the success terminal status; new code paths (third-party deposit two-phase commit) use `INITIATED → PENDING → POSTED`.

### Why deferred

The current code paths don't use `INITIATED`. Adding it means changing every external-deposit flow (bank, crypto, card, gift code, AWC, GSC) to start at `INITIATED` and transition. Coordinated change across 10+ services.

---

## Fix #9 — Kill `users.vin` as truth

**Effort:** quarter-scale. **Status:** not started. **Highest impact.**

### Current

`users.vin` is the live wallet column. 100+ code paths read it. Every game server, payment service, agency commission flow, withdrawal flow, refund flow reads or writes `users.vin` directly. The new ledger (`money_account.balance` + `money_entry`) is a parallel dual-write that drifts (we have 80 drifted rows).

### Target

Per the spec: balance = `SUM(money_entry.amount × direction-sign) FOR account WHERE …`. `money_account.balance` is a cache. `users.vin` becomes a derived cache, NOT a source of truth.

### Phased plan (quarter)

**Phase A — Read-side cutover (4-6 weeks)**
1. Build `BalanceService.get(userId, currency)` that reads from `money_account.balance` (cache) with `SUM(money_entry)` as fallback.
2. Replace ALL `users.vin` reads with `BalanceService.get` calls. Audit by `grep "users.vin" backend-master`.
3. Shadow-mode metric: emit `cache_vs_ledger_diff_total` every read. Watch for non-zero in production.

**Phase B — Write-side cutover (4-6 weeks)**
1. Every game/payment that writes `users.vin` must now POST through the ledger SP first. The SP updates `money_account.balance`.
2. Add a trigger on `money_account` UPDATE that mirrors the change to `users.vin` for any code that hasn't migrated yet. (Backwards compat layer.)
3. Once shadow-mode shows zero diff for 2 weeks, remove the trigger.

**Phase C — Cleanup (2-4 weeks)**
1. `users.vin` becomes derived (or removed entirely). Old code paths that still write directly fail.
2. Reconcile historical drift in a one-off batch fix.
3. Remove `money_gateway_log` (it becomes redundant — `money_entry` is the audit trail).

### Why deferred

Touching every money path in the system simultaneously is the textbook recipe for silent money loss. This needs:
- A dedicated person for the quarter.
- Shadow-mode for 2+ weeks per phase.
- Per-game playtest sign-off.
- A rollback plan that doesn't lose live wallet state.

---

## Postgres migration question

**Verdict:** don't migrate the whole stack. Migrate ONLY the ledger when you do Fix #9.

**If we migrate everything:** 5-7 months, 4 MySQL DBs + Java + PHP + Node + .NET clients, schema differences (backticks → quotes, `ON DUPLICATE KEY UPDATE` → `INSERT … ON CONFLICT`, AUTO_INCREMENT → SEQUENCE, MySQL JSON → JSONB, ENUM type, partition syntax, time-zone semantics).

**If we migrate ONLY the ledger** (`money_account`, `money_entry`, `money_transaction`, `money_idempotency`, `money_outbox`):
- 4-6 weeks.
- Java code that touches these tables: `MoneyLedger.java` + the SP `post_money_transaction` (rewrite as plpgsql).
- Gain: real `SERIALIZABLE` isolation, real `CHECK` constraints with subqueries, deferred constraints, `LISTEN/NOTIFY` instead of polling for outbox.
- Lose: nothing in user-facing paths (they keep reading from MySQL `users.vin` until Fix #9 happens).

Recommended pairing: start ledger Postgres migration as **Phase A.5** of Fix #9 — once read-side is on `BalanceService`, swap the ledger backend transparently.

---

## Open questions for the team (carried over from spec §16)

1. **Currency model.** Single VND, or multi-currency from day one? Affects Money type and `money_account.currency` handling.
2. **Account ownership.** One user → one wallet, or split (`PLAYER_VIN` / `PLAYER_BONUS_LOCKED` / `PLAYER_SAFE`)? We currently have multiple per user already (PLAYER_VIN, PLAYER_XU, PLAYER_SAFE, PLAYER_VP) — but bonus locking isn't a separate account.
3. **Bonus locking.** Are bonuses immediately spendable or wager-locked?
4. **Reversal policy.** Can confirmed transactions be reversed? Current schema has `REVERSED` status + `reversal_transaction_id`, but service-layer reversal flow is unwritten.
5. **Audit retention.** How long? KR jurisdictions for virtual currency typically 5+ years.
6. **Failure SLA.** When ledger is down, do we reject new bets or queue them? Affects gameserver UX.

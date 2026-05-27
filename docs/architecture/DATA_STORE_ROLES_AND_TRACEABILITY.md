# Data store roles, transaction ordering, and cross-store traceability

**Authored:** 2026-05-02
**Status:** snapshot of current state + open architectural question (Postgres consolidation)

This document captures (1) what each datastore actually does in the
Sunwinkr backend today, (2) how transaction ordering is enforced, and
(3) the cross-store traceability gap between MySQL and MongoDB — which
motivates considering a future move to Postgres.

---

## 1. Common misconceptions worth correcting up front

Two things people sometimes assume that are NOT true today:

### Misconception A — "Transactions go through RabbitMQ FIFO and a serial consumer updates the balance in order"

**False.** RabbitMQ is NOT in the wallet-write path. Wallet writes are
synchronous SQL inside the request-handling thread. RabbitMQ carries
two kinds of messages, neither of which is a wallet update:

- `queue_action_minigame` / `queue_action_portal` — balance-push
  notifications. Fire-and-forget. Tells game servers "user X has new
  balance Y, refresh their session." If RMQ is down, the wallet UPDATE
  still committed.
- `queue_log_money_user_extra` — async commission attribution for
  analytics. The `vbee` consumer drains it and writes to MongoDB
  `log_money_user_extra`.

**What actually serializes wallet updates is MySQL row-level locking.**
Two concurrent calls to `MoneyGateway.creditUser(userId=8763, ...)`
serialize at the row lock acquired by `UPDATE users WHERE id=8763`.
Whichever thread arrives first holds the lock; the other waits until
commit. Combined with `money_gateway_log.uk_tx_source UNIQUE (tx_id,
source, user_id)`, retries with the same `(tx_id, source)` are
rejected at INSERT — duplicates never apply.

This was a deliberate fix. Pre-SUN-1200 the system DID try to do
wallet writes via cache-first + async RMQ sync, and that broke: RMQ
messages were lost, cache stayed updated, DB didn't, and the
"KwonUSD" player gained 300k vin from 3 reject loops where the cache
debited but the DB never did. The fix made wallet writes synchronous
to MySQL, with cache + RMQ as best-effort post-write notifications.

### Misconception B — "MongoDB is the audit store"

**False.** Audit lives in MySQL, in two tables:

| Table | Role |
|---|---|
| `vinplay.money_gateway_log` | Legacy audit. One row per `MoneyGateway.creditUser/debitUser/transferBetweenUsers` call. UNIQUE on `(tx_id, source, user_id)` makes it race-safe (audit #17). |
| `vinplay.money_transaction` + `vinplay.money_entry` | New double-entry ledger (Phase 0). Every wallet movement = 1 transaction + 2 entries. Idempotent on `external_ref` via `money_idempotency` PK. |

Why MySQL and not Mongo for audit:

1. **ACID.** Audit row INSERT is in the same SQL transaction as the
   wallet UPDATE. Either both happen or neither. Mongo can't
   participate in a SQL transaction.
2. **UNIQUE constraints for dedup.** That's the race-safety mechanism.
   Mongo has unique indexes too, but loses the cross-table atomicity.
3. **Joins.** The 4 reconciliation views
   (`v_money_unbalanced_transactions`, `v_money_account_drift`,
   `v_money_negative_player`, `v_money_orphan_reversed`) are SQL
   queries that prove the ledger balances. Equivalent Mongo
   aggregations would be much slower and more code.

What MongoDB actually holds:

| Mongo collection | Role |
|---|---|
| `log_gsc_bets` | One doc per GSC wager. **The reconciler scans `settled=false` rows.** Bridges Mongo → MySQL when GSC's settle webhook is missed. |
| `log_money_user_extra` | Async commission-attribution rows for dashboards. |
| Per-game round logs (`binh`, `tlmn`, `poker`, `slot`, …) | Game state per round (cards dealt, hand outcome). Written by game servers. |
| `chatbox`, notifications | Out of money path. |

So Mongo IS in the money flow — but as the **unsettled-bet ledger for
the GSC reconciler**, not as the wallet audit. The wallet audit is in
MySQL, period.

---

## 2. Three-store contract — what you can rely on

| Question | Answer | Mechanism |
|---|---|---|
| Can two simultaneous credits to the same user double-apply? | **No** | MySQL row lock + `uk_tx_source` UNIQUE |
| Can a deposit retry from a webhook double-credit? | **No** | `(tx_id, source, user_id)` UNIQUE — second INSERT fails 1062, transaction rolls back |
| If RabbitMQ crashes, does money go missing? | **No** | RMQ only carries notifications + analytics. Wallet writes already committed in MySQL before RMQ publish. |
| If Hazelcast crashes, does money go missing? | **No** | Hazelcast is a read-cache. MySQL is source of truth. Stale reads possible until cache refills. |
| If MongoDB crashes, does money go missing? | **No, but** | The GSC reconciler can't run; missed GSC settles wait until Mongo recovers, then reconciler catches up. |
| If MySQL crashes mid-transaction, does money go inconsistent? | **No** | `setAutoCommit(false)` + `commit/rollback` makes wallet UPDATE + audit INSERT atomic. |
| Can two concurrent threads see a mid-flight wallet state? | **No** | Row lock holds until commit; the second reader sees post-commit value. |
| Is there a global ordering across all users? | **No, and we don't need one** | Two players in different games are independent. Per-user ordering is the only invariant; row locks give it. |

---

## 3. Transaction ordering — MySQL row locks vs RMQ FIFO consumer

### Today: MySQL row locks (synchronous in request thread)

```
Thread A                          Thread B
─────────                         ─────────
setAutoCommit(false)              setAutoCommit(false)
UPDATE users WHERE id=8763  ─┐
                              │  blocks on row lock for id=8763
INSERT money_gateway_log ...  │
COMMIT                       ─┘
                                  UPDATE users WHERE id=8763 (now sees A's value)
                                  INSERT money_gateway_log ...
                                  COMMIT
```

Pros:
- Lowest possible latency (~5-10ms per call, one DB roundtrip)
- Per-user serialization without a coordinator
- Different users run fully parallel
- Audit is the SQL row itself — no coordination required

Cons:
- Cross-user serialization is hard (no global queue) — but we don't need it
- Long-running transactions can block each other (mitigated by short transactions and the atomic floor-check pattern)

### Alternative: RMQ FIFO + serial consumer

Some payment systems do this. Pros and cons against the current design:

| Property | MySQL row locks (current) | RMQ FIFO + serial consumer |
|---|---|---|
| Per-user concurrency | serial automatically (row lock) | serial only if you partition by user (which… is back to row-level reasoning) |
| Cross-user concurrency | parallel | serial for the whole queue (slow), or partition (back to per-user logic) |
| Failure mode | DB crash → both threads retry, same end-state | Consumer crash → backlog grows, real-time UX breaks |
| Where is the audit? | The DB row itself | Need to also write a DB row after consume — two systems to keep in sync |
| User-visible latency | ~5-10ms | ~50-200ms (RMQ hop + consumer scheduling) |
| Recovery on outage | Just retry the request | Reprocess the queue; risk of duplicate consume unless idempotent |

For an interactive casino where the player is waiting on the game UI
to refresh, **synchronous DB-with-row-locks gives lower latency AND
equivalent correctness**. RMQ FIFO would only help if we needed
cross-user serialization (a global rate limiter or single-threaded
audit-log writer), which we don't.

### When RMQ FIFO would actually be the right call

- **Multi-region / sharded MySQL** — if writes had to traverse a
  cross-region replica, a global-ordering queue at the edge could
  reduce write contention.
- **Outbox pattern for downstream systems** — if external APIs (e.g.
  bookkeeping, regulator reporting) need every wallet movement in
  order, the outbox-via-RMQ pattern is standard.
- **High-volume bulk operations** — if a single operator action has
  to fan out to 10K users (e.g. a global VIP rebate distribution),
  enqueueing 10K small events lets the worker pool catch up over
  minutes instead of one big multi-row transaction.

None of those apply today. Worth a re-evaluation if any change in the
future (e.g. expansion to a regulated market that requires real-time
reporting).

---

## 4. The cross-store traceability gap (the real problem)

This is the open architectural question. **MySQL and MongoDB cannot
join.** There are no foreign keys between them. To trace a player's
GSC bet end-to-end today, you need to:

1. Find the wager in MongoDB: `db.log_gsc_bets.findOne({"wager_code": X})`
2. Find the audit row in MySQL: `SELECT * FROM gsc_event_log WHERE gsc_wager_code = X`
3. Find the wallet movement in MySQL: `SELECT * FROM money_gateway_log WHERE tx_id = X`
4. Find the ledger entries in MySQL: `SELECT * FROM money_transaction WHERE external_ref = X` (joins money_entry)

Step 1 is in Mongo, steps 2-4 are in MySQL. There's no JOIN, no
foreign-key integrity, no transactional consistency between them.
Concretely:

- A wager can exist in `log_gsc_bets` with no matching `gsc_event_log`
  row (and we have to chase whether the webhook was lost or the
  reconciler is behind).
- A `money_gateway_log` row can have `tx_id` pointing at a wager that
  doesn't exist in Mongo yet (Mongo write is async and lagged).
- The reconciler bridges the two stores by application code; if its
  logic is wrong (which is exactly the audit #18/#19 class of bug), no
  DB-level integrity catches it.

This is the source of most of the operational pain in the money flow.
Today's reconciler RECEIVED-state guard (commit `28d53563`) and the
manual GSC trace (`docs/MONEY_TRACE_SCENARIOS.md`) are workarounds
that exist BECAUSE we can't just `JOIN log_gsc_bets ON
gsc_event_log.gsc_wager_code = log_gsc_bets.wager_code`.

---

## 5. Postgres consolidation — the option worth evaluating

If the cross-store gap is the root pain, the cleanest fix is to put
both kinds of data in one ACID-capable store. Postgres can hold the
relational ledger AND the variable-shape game logs (via JSONB), with
real foreign keys between them.

### What Postgres would unify

| Today's MySQL | Today's Mongo | After Postgres |
|---|---|---|
| `vinplay.users` | — | `users` (unchanged) |
| `vinplay.money_account` / `money_transaction` / `money_entry` | — | same tables (unchanged) |
| `vinplay.money_gateway_log` | — | same (unchanged) |
| `vinplay.gsc_event_log` (request/response audit) | — | same (raw JSON in JSONB column instead of `json` column — minor) |
| — | `log_gsc_bets` | new table with FK → `money_transaction.external_ref` |
| — | `log_money_user_extra` | new table with FK → `money_gateway_log.id` |
| — | per-game round logs (`tlmn`, `poker`, slot, …) | per-game tables with `payload JSONB` and FK → `users.id` |
| — | `chatbox`, notifications | irrelevant to money path; can stay in Mongo or migrate later |

### Benefits

1. **Real cross-table tracing.** `SELECT * FROM money_transaction t
   JOIN log_gsc_bets b ON b.wager_code = t.external_ref WHERE
   t.user_id = ?` — one query, fully consistent, no application-level
   bridging.
2. **Foreign keys catch broken references at write time.** Today, a
   bug that writes a `money_gateway_log` row pointing at a
   non-existent wager is silent until someone runs a manual cross-DB
   audit. With FK, the INSERT fails immediately.
3. **Reconciler complexity collapses.** The whole `GscWagerReconciler`
   exists because Mongo's `settled` flag is the only way to know
   whether the missing settle has been recovered. With Postgres FKs,
   the reconciler becomes "rows where settled=false older than grace
   window" — same query, but no two-DB consistency to manage. Audit
   #18, #19, #20, and the RECEIVED-state guard would mostly evaporate.
4. **JSONB gives flexibility where it's actually needed.** Per-game
   round payloads can stay schema-flexible (cards, hand state, dice
   rolls) but live in tables that JOIN with the rest of the system.
   GIN indexes on JSONB give Mongo-equivalent ad-hoc query
   performance.
5. **Stronger consistency for partial failures.** Today, a "deposit +
   GSC bet + commission attribution" sequence touches both MySQL and
   Mongo. If Mongo write fails after MySQL commit, we have an audit
   gap. Postgres puts it all in one transaction.
6. **Backup and restore is one operation.** Today we have to
   dump+restore both `mysqldump` and `mongorestore`, and a partial
   restore (one but not the other) leaves the system inconsistent.
   Postgres `pg_dump` covers it all.
7. **One query language for ops.** SQL only, no `db.collection.find`
   ad-hoc Mongo. Operators don't have to context-switch between two
   query syntaxes when investigating a bug.

### Costs

1. **Migration effort.** Both DBs are populated. Need to:
   - Stand up Postgres alongside MySQL+Mongo
   - Replicate live data (MySQL → Postgres for relational, Mongo →
     Postgres JSONB for game logs)
   - Cut over read traffic, then write traffic, then decommission old
     stores
   - Verify reconciliation invariants hold across the migration
   - Realistic timeline: 3-6 months for a phased migration on a busy
     production system

2. **Driver/library churn.** Java code uses `mysql-connector-java` and
   `mongo-driver`. Postgres needs `postgresql-jdbc` (drop-in for
   SQL-only code) plus probably a JSONB helper library for the
   variable-shape paths. Hibernate/MyBatis layers might need
   adjustment.

3. **Schema redesign for game logs.** Today's Mongo collections evolve
   per-game without coordination. Moving to Postgres with JSONB still
   allows that, but adopting consistent column conventions (e.g. every
   game-log table has `user_id BIGINT NOT NULL`, `played_at
   TIMESTAMPTZ NOT NULL`, `payload JSONB NOT NULL`) is a discipline
   shift. Worth doing, but it's work.

4. **Hazelcast story.** The `users` IMap is keyed by nickname and
   carries a `UserCacheModel`. Hazelcast doesn't care which DB backs
   it — Postgres is fine — but the `MoneyGateway.updateCacheAndPush`
   sequence uses `mysqlpoolname` connection-pool name; that needs to
   be renamed and reconfigured.

5. **Operational learning curve.** MySQL ops knowledge (replication,
   binlog, `SHOW PROCESSLIST`, etc.) doesn't translate 1:1 to Postgres
   (`pg_stat_activity`, WAL, `pg_basebackup`). The team needs to
   learn the new tooling.

6. **Risk profile during migration.** A bug during cutover is
   high-blast-radius. Mitigations: feature-flag every table's read
   destination; dual-write into both stores during the bridge period
   (analogous to the Phase 1 ledger dual-write); verify reconciliation
   invariants daily.

### Alternative middle paths

If full Postgres consolidation is too big, smaller wins exist:

- **Move `log_gsc_bets` from Mongo to MySQL.** This single table is
  the entire cross-store gap for the money flow. Migrating it to
  MySQL (as a regular table with FK to `money_transaction`) closes
  audit #18, #19, and most of the reconciler complexity, without
  touching the per-game round logs. Costs: ~1-2 weeks. Risk:
  meaningful but bounded. **Recommended interim if Postgres is too
  far out.**

- **Treat MySQL+Mongo as is, add an event bus that mirrors both
  into a third analytics store.** Doesn't help the operational pain;
  worse, adds yet another store.

- **Mongo Atlas's `$lookup` or Trino/Presto across both DBs.** Lets
  you JOIN ad-hoc. But it's read-only and adds another runtime
  component. Doesn't help the write-path consistency problem.

### Decision framework

The choice depends on three factors:

1. **How much pain is the cross-store gap actually causing?** If most
   ops debugging is "find the wager" → "find the wallet movement" →
   "verify reconcile fired," and that takes hours per incident, the
   migration ROI is high. Today this is moderate pain (handled by
   `MONEY_TRACE_SCENARIOS.md`); it would become high pain at higher
   transaction volume.

2. **What's the cost of the audit #18, #19, #20 fixes if we don't
   migrate?** Audit #18, #19 are addressed by the
   SeamlessWalletAggregator refactor (in flight). #20 is a single SQL
   transaction. Postgres would have made all three structural; without
   it, they're each separate fixes maintained forever.

3. **Are we adding new providers / regulatory obligations / regional
   expansion that would intersect with the migration?** If we're
   stable and the current architecture is "good enough," migration is
   forecast (12+ months out). If a regulator requires audit
   traceability with strict consistency guarantees, migration is
   immediate (6 months).

### Recommended next step

Don't migrate yet. **Do a 1-week spike** to:

1. Stand up Postgres locally with the relational ledger schema
   (Phase 0 migrations port mostly mechanically).
2. Define the JSONB shape for `log_gsc_bets` and 1-2 game-log
   collections.
3. Write the cross-table queries that were previously two-DB:
   `JOIN money_transaction ⋈ log_gsc_bets`, `JOIN money_gateway_log ⋈
   log_money_user_extra`, etc. Confirm they're cleaner and faster.
4. Estimate migration timeline more precisely — what's the
   binlog-replication story, dual-write pattern, etc.

The output is a 5-page "yes/no/when" recommendation with concrete
data. Then the decision is informed by reality, not speculation.

---

## 6. Open follow-ups (separate from migration decision)

These are improvements that fit within the current MySQL+Mongo
architecture, useful regardless of whether Postgres lands:

- Unify the `awc_transactions` and `gsc_event_log` semantics — both
  are raw-payload audit, but with different schemas and naming. A
  single `seamless_wallet_audit` table would simplify operator queries
  (in flight via the SeamlessWalletAggregator refactor).
- Add a `money_gateway_log.transaction_id` FK to
  `money_transaction.transaction_id` once Phase 1 dual-write is
  complete and the ledger is the source of truth (Phase 2). Closes
  the within-MySQL gap between legacy audit and ledger audit.
- Document the cross-store reconciliation contract — what's the
  expected lag between MySQL `gsc_event_log` and Mongo `log_gsc_bets`?
  What's the operator's runbook if they disagree? Today this is
  implicit; making it explicit prevents 3am head-scratching.

---

## Summary

| Layer | Today's primary role | Strength | Weakness |
|---|---|---|---|
| MySQL | Source of truth for money (wallet + ledger + audit). Atomic, race-safe. | ACID, joins, UNIQUE constraints, mature ops. | Schema rigidity for game logs. |
| MongoDB | High-volume game history + GSC unsettled-bet ledger. | Schema flexibility, write throughput. | No FK to MySQL — application-level bridging required. |
| Hazelcast | Read-cache for users + auth tokens. | Sub-ms reads, no DB load. | No longer authoritative; outage just means stale reads. |
| RabbitMQ | Notifications + analytics. | Decoupled, async, durable enough. | Not in the money write path; would only matter if downstream consumer falls behind. |

The architecture is **correct and race-safe today**. The gap is
**cross-store traceability**. Postgres consolidation is the cleanest
fix, but it's a 3-6 month migration. Worth a 1-week design spike
before committing.

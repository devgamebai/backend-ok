# Money-Ledger Long-Term Plan

**Status:** active, started 2026-05-09
**Owner:** operator + Claude (sub-agent driven)
**Target:** M3 milestone in 3.5-4 months (strict ledger contract enforced in DDL)
**Safety baseline:** git SHA `b33d0bbe` + DB backup `/root/sunwinkr/backups/2026-05-09/`

## Problem statement

The money-ledger pattern is **declared** (every write goes through `MoneyGateway.creditUserWithCumulative`) but not **enforced** in the schema. We have 5 layers of money state — the canonical ledger plus 4 derived/parallel layers — that drift unless explicitly synced. Today's full-reset operator action took 3 cleanup passes because each layer had to be cleaned independently.

### The 5 layers

```
1. LEDGER (canonical):    money_gateway_log, money_entry           ← truth
2. MATERIALIZED state:    users.vin/vin_total/recharge_money/...   ← cache of ledger sum
3. WORKFLOW intent:       deposit_transactions/bank_withdrawals... ← parallel record
4. PER-GAME audits:       log_gsc_bets/log_KhoBau/...              ← parallel record (game-specific)
5. ROLL-UPS:              log_report_user/rebate_daily_rollup/...  ← derived but stored
+ CACHE:                  Hazelcast users IMap, Redis balance keys ← projection of layer 2
```

**Why we have layers 2-5:** read latency. Game-tick balance reads hit `users.vin` (sub-ms). Computing `SUM(ledger)` on every read would be O(N) — unfeasible at scale.

**Why they drift:** writes are coordinated by *convention* (everyone calls MoneyGateway), not enforced by *DDL* (anyone with UPDATE rights can bypass).

## Goal

Achieve **strict ledger contract** without sacrificing read latency:

| Operation | Latency budget |
|---|---|
| Game tick balance read | <1 ms (Hazelcast) |
| Portal WS balance push | <10 ms |
| Admin user-list page (c=9910, 100 rows) | <50 ms |
| Admin top-up (c=100) | <100 ms |
| Audit drift check (per user) | <200 ms (off-path) |
| Operator full-reset cleanup | <30 s for 595 users (single pass) |

These budgets are **non-negotiable**. Any phase that regresses any number does not ship.

## Architecture (target end-state)

```
                       WRITES                                    READS

  c=100 admin / game settle / approve deposit                   game tick reads vin
              │                                                          ↑
              ▼                                                  ┌───────┴────────┐
   ┌─────────────────────┐                                      │  L1 Hazelcast  │  ~0.1 ms
   │  MoneyGateway       │  ── single transaction ──            └───────┬────────┘
   │  (ONLY write path)  │                                              │ miss
   │                     │                                              ▼
   │  1. insert ledger   │                                      ┌────────────────┐
   │  2. update projection│                                     │  L2 MySQL      │  ~1 ms
   │  3. publish event   │                                      │  projections   │
   └─────────┬───────────┘                                      │  (users,       │
             │                                                  │   user_aggr.)  │
             ├──→ money_gateway_log (append-only, IMMUTABLE)    └───────┬────────┘
             ├──→ users.vin / user_aggregates (projections)             │ DR / reconcile
             ├──→ Redis Stream (fan-out to consumers)                   ▼
             └──→ Hazelcast invalidate                          ┌────────────────┐
                                                                │  L3 Ledger     │  ~50 ms
                                                                │  SUM (audit)   │
                                                                └────────────────┘
                                                                  (audit-bot only)
```

**Contract:**
- Truth lives at L3, but is read from L1/L2 in hot path.
- Writes update L1+L2+L3 in ONE transaction.
- Audit-bot continuously verifies L1≈L2≈L3.

## Phased roadmap

| Phase | Deliverable | Calendar | Cumulative |
|---|---|---|---|
| **P1** | Operationalize existing audit-bot (already deployed). Extend to new aggregates. Fix `tbl_cashback_logs` reference. Tune thresholds. Runbook. | 3-5 days | **M1 — drift visible** |
| **P2** | `ledger_entry_id` FK on 8 workflow tables; writer hooks dual-link in approve paths; backfill existing rows. | 2-3 weeks | **M2 — single-pass cleanup possible** |
| **P3** | Drop `users.t_nap/t_rut/recharge_money/...` from being write-targets. `c=9910` derives on read. | 1.5-2 weeks | M2 (continued) |
| **P4** | Stored-procedure-only writes for `users.vin` etc. DDL: revoke direct UPDATE on `users.vin` from app role. | 3-4 weeks | **M3 — ledger contract enforced in DDL** |
| **P5** | `BEFORE DELETE` trigger on `money_gateway_log` blocks deletes (append-only DDL). Cleanup runbook switches to compensating entries. | 2-3 weeks | M3 (continued) |
| **P6** *(optional, longer-term)* | `user_money_state` projection table replaces sprawl. Old columns become VIEW for compat. | 6-8 weeks | M4 — zero-drift guaranteed |
| **P7** *(optional)* | Redis Stream projection for cross-service event distribution. Blocked on RMQ→Redis S3 done. | 3 weeks | M5 |
| **P8** *(optional)* | Per-game audits as ledger refs (15+ integrations). | 10-12 weeks | M5 — long-term done |

**Recommended scope: M3 (P1-P5).** Captures 80% of operational benefit. Re-evaluate P6+ at M3.

## Sub-agent execution model

Every task in every phase goes through this pipeline:

```
┌──────────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Planner  │ ──► │ Implementer │ ──► │ Spec Reviewer│ ──► │ Module-Impact│ ──► │ Code Quality │
│ (Plan    │     │ (executor)  │     │ (Plan agent  │     │  Reviewer    │     │  Reviewer    │
│  agent)  │     │             │     │  in review)  │     │              │     │              │
└──────────┘     └─────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

| Agent | Role | Protects against |
|---|---|---|
| Planner | Per phase: enumerate files, callers, test surface. Output: markdown spec with discrete tasks. | Underestimating scope; missing callers |
| Implementer | One task at a time. Implements + tests + commits. Reports DONE / BLOCKED / NEEDS_CONTEXT. | Reinventing or skipping tests |
| Spec Reviewer | Diff vs spec. Flags missing AND extras. | Scope creep; missing acceptance criteria |
| Module-Impact Reviewer | Maps changed APIs/DAL → consumer modules. Builds + smokes each consumer. | **Refactor breaking some module's function** |
| Code Quality Reviewer | Reuse, abstractions, edge cases. Final gate. | Hacky fixes that pass tests but rot |

**Hard rule:** no phase ships until all 4 review agents approve.

## Safety nets

- ✅ **Audit-bot in production** = catches drift within 5 min after any phase ships
- ✅ **Staging soak** = 3-7 days per high-risk phase before production
- ✅ **Per-phase revert path** = ≤5 min rollback (designed in, not retrofit)
- ✅ **Module-Impact Reviewer** = consumer breakage caught before deploy
- ✅ **Today's safety checkpoint** = git `b33d0bbe` + DB backup `/root/sunwinkr/backups/2026-05-09/`

## Non-goals

- ❌ Full event sourcing (Kafka/EventStore as primary truth) — overkill for our scale
- ❌ Strong consistency in Hazelcast — eventual + audit-bot is right shape
- ❌ Schema rewrite all at once — every phase is independently shippable
- ❌ Drop materialized `users.vin` — read-frequency is too high
- ❌ Backfill historical drift — going forward only

## Operating notes

- Every commit during this work pushes directly to `production` (per CLAUDE.md operator preference). No MRs.
- Each phase has its own design doc under `docs/architecture/money-ledger/PX-name.md`.
- Phase completion is when audit-bot is green for 1 week post-deploy + module-impact reviewer signed off all consumers.
- Pause/abort any phase if audit-bot drift rate spikes >2x baseline.

## Status log

- **2026-05-09**: plan written, safety checkpoint at `b33d0bbe`. P1 task 1 dispatched (Planner producing operationalize-audit-bot spec).

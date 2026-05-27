# Phase 0 Money-Ledger Migrations

## Why these are NOT in `install/flyway/sql/`

These migrations are **manually applied** (Phase 0 policy).  They contain
backfill logic that is safe to run exactly once, but the staging database
already has them applied via `docker exec`.  Porting them into Flyway would
require inserting synthetic checksum rows into `flyway_schema_history` — that
work is deferred to Task #TBD.

Until that Flyway port happens, the canonical apply mechanism is the
`docker exec` pattern documented below.

---

## Run Order (alphabetical = dependency order)

| File | Purpose |
|------|---------|
| `2026_05_02a_money_ledger_schema.sql` | Create `money_account`, `money_ledger`, `money_idempotency` tables and `post_money_transaction` stored procedure |
| `2026_05_02b_money_ledger_seed_system.sql` | Insert system accounts (HOUSE, CASINO, FEE_POOL) into `money_account` |
| `2026_05_02c_money_ledger_seed_users.sql` | Seed one `money_account` row per existing player/agent from `users` / `agency_wallet` / `credit_wallet` |
| `2026_05_02d_money_ledger_backfill_initial_balances.sql` | Post synthetic INITIAL_BALANCE ledger entries so every account's running total reconciles from zero |
| `2026_05_02e_money_idempotency_dedup.sql` | Add dedup index on `money_idempotency(idempotency_key)` + remove duplicate rows if any exist |
| `2026_05_02f_money_ledger_backfill_deposits.sql` | Replay `deposit_transactions` into `money_ledger` as a PoC (Phase 1 preview) |
| `2026_05_02g_users_negative_balance_trigger.sql` | `BEFORE UPDATE` trigger on `users` that blocks negative `main_balance` writes |
| `20260511_wallet_unify_phase0_views.sql` | Phase 0 — derived views (`v_derived_player_balance`, `v_derived_player_pnl`, `v_derived_deposit_total`, `v_wallet_drift`) + `wallet_drift_snapshot` table + currency CHECK constraint |
| `20260511_wallet_unify_drift_analyzer.sql` | Phase 0 — per-source drift root-cause views |
| `20260512_wallet_unify_option_b_baseline_reset.sql` | Phase 0 — `do_baseline_reset_v1` SP that aligns ledger PLAYER_VIN to `users.vin` |
| `20260512_phase1_update_money_db_v2.sql` | Phase 1 — additive `update_money_db_v2(user_id, money, money_type)` SP that writes ONLY `vin`/`xu` (no `_total` writes). Legacy SP stays installed; routing is via `UNIFIED_WALLET_PHASE_1` env var at the Java DAO. |
| `20260601_phase4_drop_legacy_sp_and_total_columns.sql` | Phase 4 — DROP `update_money_db` + `ALTER TABLE users DROP vin_total, DROP xu_total`. **Only apply after 14 consecutive days of `wallet_drift_snapshot.drifting_users=0` and Phase 1 flag pinned to `on` in production.** |

**g (trigger) is independent** — it only touches `users`, not the ledger tables.
It can be applied any time after **a** if you need to defer the backfill steps.

### Phase 1 cutover playbook (UserDaoImpl.updateMoney)

`UNIFIED_WALLET_PHASE_1` env var on every container that links VinPlayUserCore
(every game server + portal/backend API):

| Value | DAO behavior | Use for |
|---|---|---|
| `off` (default) | calls `update_money_db` (legacy — writes vin + vin_total) | rollback / pre-cutover |
| `shadow` | calls legacy, but bumps `UPDATE_MONEY_V2_SHADOW_HITS` counter | staging dry run, compare counter against legacy SP call rate |
| `on` | calls `update_money_db_v2` (writes ONLY vin/xu) | post-cutover steady state |

Smoke validation: `bash tests/wallet-unification/phase1_smoke.sh`.
Gate to Phase 4: 14 consecutive daily `wallet_drift_snapshot.drifting_users=0`
AND all Phase 1 callers verified migrated (search code for
`update_money_db` (not v2) before applying Phase 4 migration).

---

## All migrations are idempotent

Every file is safe to re-run on an already-applied database:

- Schema files use `CREATE TABLE IF NOT EXISTS` / `CREATE PROCEDURE … DROP IF EXISTS`.
- Seed files use `INSERT … ON DUPLICATE KEY UPDATE` or `INSERT IGNORE`.
- Backfill files skip rows that already exist (`INSERT IGNORE` / `WHERE NOT EXISTS`).
- The trigger file drops and recreates the trigger unconditionally.

Re-running any single file will not corrupt data.

---

## Apply Command

```bash
# Replace <file> with the filename you want to apply.
# Run from the repo root or any directory — the path just needs to resolve.

docker exec -i sunwinkr-mysql \
  mysql -uroot -p$MYSQL_ROOT_PASSWORD vinplay \
  < install/config/mysql/migrations/<file>
```

For a fresh database, apply a–g in order:

```bash
MIGRATIONS=install/config/mysql/migrations
for f in \
  2026_05_02a_money_ledger_schema.sql \
  2026_05_02b_money_ledger_seed_system.sql \
  2026_05_02c_money_ledger_seed_users.sql \
  2026_05_02d_money_ledger_backfill_initial_balances.sql \
  2026_05_02e_money_idempotency_dedup.sql \
  2026_05_02f_money_ledger_backfill_deposits.sql \
  2026_05_02g_users_negative_balance_trigger.sql
do
  echo "==> Applying $f"
  docker exec -i sunwinkr-mysql \
    mysql -uroot -p$MYSQL_ROOT_PASSWORD vinplay \
    < "$MIGRATIONS/$f"
done
```

---

## Verification

After applying, run the reconciliation script and confirm all 4 invariants pass:

```bash
bash bin/reconcile-money.sh
```

Expected output (all OK):

```
[OK] Invariant 1: ledger debits == ledger credits (net zero)
[OK] Invariant 2: account balances match ledger running totals
[OK] Invariant 3: no account has a negative balance
[OK] Invariant 4: every money_account row has a matching users/system entry
```

Any `[FAIL]` line means a migration was skipped or applied out of order.

---

## Future Work

- Task #TBD: Port these migrations into `install/flyway/sql/` as `V20`–`V26` with
  synthetic `flyway_schema_history` rows so fresh databases are fully automated.

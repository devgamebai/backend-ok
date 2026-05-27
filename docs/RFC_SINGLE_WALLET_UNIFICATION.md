# RFC: Single-Wallet Unification (SUN-13xx)

**Status:** Draft
**Owner:** Backend team
**Created:** 2026-05-11
**Target completion:** 8–10 weeks calendar

---

## 1. Problem

Player balance is fragmented across 12+ columns and 2 schemas:

| Column / Field | Schema | Purpose | Authoritative? |
|---|---|---|---|
| `vinplay.users.vin` | main | Primary in-game balance (VND) | yes |
| `vinplay.users.xu` | main | Secondary/promo balance | partial |
| `vinplay.users.vin_total` | main | Cumulative game P&L | derived but written |
| `vinplay.users.xu_total` | main | Cumulative xu P&L | derived but written |
| `vinplay.users.safe` | main | Frozen / vault | yes (game lock) |
| `vinplay.users.recharge_money` | main | Cumulative deposits | analytics |
| `vinplay.users.money_vp` | main | VIP-derived money | partial |
| `vinplay.users.vip_point` | main | VIP level points | yes |
| `vinplay.users.vip_point_save` | main | Saved VIP points | yes |
| `vinplay.users.gift_total` | main | Cumulative gifts | analytics |
| `cgame.users.cash` | banca | BanCa in-game gold | yes |
| `cgame.users.cash_safe` | banca | BanCa vault | yes |
| `cgame.users.cash_silver` | banca | BanCa silver | yes |

Plus separate wallets in their own tables (out of scope for this RFC, but must remain consistent):
- `agency_wallet` / `agency_wallet_transactions` — agent commission balance
- `credit_wallet` / `credit_wallet_transactions` — agent credit line

### Problems caused

1. **No single source of truth for "player balance".** Every consumer reads a different column. Past incidents: KwonUSD2 (admin deduct touched `vin_total`), 11 BanCa players stuck mid-transfer (vin debited, cgame.cash not credited).
2. **Ledger violations.** `cgame.users.cash*` and `vinplay.users.safe` are mutated outside `MoneyGateway`, so no audit row hits `money_gateway_log` / `money_ledger`. Reconciliation across systems is impossible.
3. **Brittle invariants.** SP `update_money_db` writes both `vin` and `vin_total` together. Any caller that needs current balance without affecting cumulative P&L (admin deduct, refund, comp) is forced into a special branch — invariant kept by convention, not constraint.
4. **No multi-currency story.** Crypto deposits exist (`crypto_deposits` table, `CRYPTO_INBOX/OUTBOX` ledger accounts) but everything converts to VND at deposit. We have no path to hold a USDT-denominated balance.

---

## 2. Goal

**Single authoritative balance per player per currency, written only through a typed double-entry ledger.**

End state per player:
```
money_account
├── account_type=PLAYER_WALLET, currency=VND     ← replaces vin, xu, safe, money_vp, banca.cash
├── account_type=PLAYER_WALLET, currency=USDT    ← future crypto-native (optional)
└── account_type=PLAYER_VIP_POINTS, currency=VP  ← replaces vip_point/vip_point_save
```

Drop columns: `xu`, `xu_total`, `vin_total`, `safe`, `recharge_money`, `money_vp`, `vip_point`, `vip_point_save`, `gift_total`, `cgame.users.cash`, `cash_safe`, `cash_silver`.

Keep `vinplay.users.vin` **only as a denormalized read-cache** updated by the ledger trigger. Long-term: drop it too; for v1 keep so hot-path SELECT u.vin queries (60+ call sites) continue without rewrite.

---

## 3. Double-Entry Correctness Rules (non-negotiable)

Every wallet movement MUST satisfy:

1. **Atomic transaction.** `money_transaction` (header) + ≥2 `money_entry` rows in one SQL transaction.
2. **Entries sum to zero per currency.** `SUM(DEBIT) − SUM(CREDIT) = 0` for each currency in the transaction.
3. **Balance integrity.** `balance_after = balance_before ± amount` enforced by app, validated nightly by reconciliation job.
4. **Idempotency.** `(external_ref, transaction_type)` UNIQUE in `money_idempotency`. Same admin click cannot debit twice.
5. **Reversibility.** Every transaction can be reversed by emitting an opposite transaction with `correlated_transaction_id` set + `status=REVERSED` on the original.
6. **No raw UPDATE on wallet columns.** Lint at build time. Already enforced via `Dockerfile` allowlist — extend to new schema.

### Account categories

- **Asset accounts (player money)**: increase via CREDIT, decrease via DEBIT. Examples: PLAYER_WALLET, AGENCY_WALLET.
- **Liability accounts (system pools)**: increase via DEBIT, decrease via CREDIT. Examples: HOUSE_GAME_POT, BANK_OUTBOX, CRYPTO_OUTBOX, PROMO_POOL, GIFT_CODE_POOL, JACKPOT_POOL, SUSPENSE.

(Current `money_account.is_system` flag distinguishes. Confirm sign convention in code matches.)

### Transaction types (canonical, frozen)

| transaction_type | Meaning | Entries |
|---|---|---|
| `DEPOSIT_BANK` | Player deposits VND via bank | DEBIT BANK_INBOX, CREDIT PLAYER_WALLET |
| `DEPOSIT_CRYPTO` | Player deposits USDT (converted to VND at rate) | DEBIT CRYPTO_INBOX, CREDIT PLAYER_WALLET |
| `WITHDRAW_BANK` | Player cashout to bank | DEBIT PLAYER_WALLET, CREDIT BANK_OUTBOX |
| `WITHDRAW_CRYPTO` | Player cashout to USDT wallet | DEBIT PLAYER_WALLET, CREDIT CRYPTO_OUTBOX |
| `WAGER_DEBIT` | Player bets on a game | DEBIT PLAYER_WALLET, CREDIT HOUSE_GAME_POT |
| `WAGER_CREDIT` | Game payout to player | DEBIT HOUSE_GAME_POT, CREDIT PLAYER_WALLET |
| `JACKPOT_PAYOUT` | Jackpot hit | DEBIT JACKPOT_POOL, CREDIT PLAYER_WALLET |
| `JACKPOT_CONTRIB` | Per-bet contribution to jackpot | DEBIT HOUSE_GAME_POT, CREDIT JACKPOT_POOL |
| `ADMIN_TOPUP` | Admin manual credit | DEBIT PROMO_POOL, CREDIT PLAYER_WALLET |
| `ADMIN_DEDUCT` | Admin manual debit | DEBIT PLAYER_WALLET, CREDIT PROMO_POOL |
| `PROMO_CLAIM` | Player redeems giftcode/bonus | DEBIT GIFT_CODE_POOL, CREDIT PLAYER_WALLET |
| `REBATE_PAYOUT` | Differential commission to player | DEBIT HOUSE_REVENUE, CREDIT PLAYER_WALLET |
| `AGENT_COMMISSION` | Downline commission to agent wallet | DEBIT HOUSE_REVENUE, CREDIT AGENCY_WALLET |
| `INTERNAL_TRANSFER` | Player A → Player B | DEBIT PLAYER_WALLET_A, CREDIT PLAYER_WALLET_B |
| `LOCK_FUND` | Move into player vault (replaces `safe`) | DEBIT PLAYER_WALLET, CREDIT PLAYER_VAULT |
| `UNLOCK_FUND` | Vault back to wallet | DEBIT PLAYER_VAULT, CREDIT PLAYER_WALLET |
| `REVERSAL_*` | Mirror of any of the above with correlated_transaction_id | swapped DEBIT/CREDIT |

Any new movement type requires:
- Adding the entry pattern here
- Adding the typed constant to `MoneyGateway`
- Adding to the dual-write lint allowlist if direct SQL is required

---

## 4. Multi-Currency Story

`money_account.currency` already exists. Conventions:
- `VND` — game currency (default everything today)
- `USDT` — crypto-native balance (NOT implemented yet; reserved)
- `VP` — VIP points (treat as currency for ledger; no FX with VND)

FX between currencies (e.g. crypto deposit → VND wallet):
- Two transactions correlated:
  1. `DEPOSIT_CRYPTO_USDT`: DEBIT CRYPTO_INBOX(USDT), CREDIT PLAYER_WALLET(USDT)
  2. `INTERNAL_FX_USDT_VND`: DEBIT PLAYER_WALLET(USDT) X usdt, CREDIT FX_HOUSE(USDT) X usdt; DEBIT FX_HOUSE(VND) Y vnd, CREDIT PLAYER_WALLET(VND) Y vnd
- FX rate captured in `money_transaction.metadata` (json) at posting time, immutable.
- Sum-to-zero invariant is per-currency-per-transaction. Two-currency FX needs an FX bridging account or two correlated single-currency transactions.

For v1 we keep crypto deposits converting at gateway boundary (current behaviour). USDT-denominated wallet is a future extension and **not in scope** for this RFC.

---

## 5. Migration Constraints

**Functional rules (non-negotiable, every wave gated on):**

1. **No regression in any user-visible balance.** For every user, `derived_balance = SUM(CREDIT) - SUM(DEBIT)` from `money_entry` MUST equal `users.vin` ± 0 at the cut-over moment.
2. **No regression in any reconciliation report.** Agency commission, deposit reports, withdraw reports, rebate logs must produce identical numbers ±0 before and after each wave.
3. **No new banned operations.** Lint check enforces.
4. **All currently-passing integration tests pass.** Including `MoneyGatewayDualWriteTest`.
5. **Zero downtime.** Reads and writes both work during each wave via feature-flag gating and dual-write.

**Safety procedures (every wave):**

- **Snapshot.** `mysqldump --single-transaction users money_account money_entry money_transaction agency_wallet credit_wallet cgame.users` → `snapshots/wave-{N}-pre-{date}.sql.gz`. Keep 90 days.
- **Feature flag.** Each behavior change gated by `UNIFIED_WALLET_PHASE_{N}={on|off|shadow}`. `shadow` = write new, read old. `on` = write new, read new.
- **Drift monitor.** Hourly cron: for each player, compare `users.vin` vs `SUM(money_entry WHERE account_type='PLAYER_WALLET' AND currency='VND')`. Alert if drift > 1 VND for >1% of players.
- **Per-wave rollback runbook.** Listed at end of each wave below.

---

## 6. Phased Plan

### Phase 0 — Hardening (1 week, parallel)

No user-visible change. Pure groundwork.

**Tasks:**
1. Add `derived_player_balance(user_id, currency)` SQL view summing `money_entry` joined to `money_account` filtered by `account_type='PLAYER_WALLET'`. Test on read replica.
2. Add `derived_player_pnl(user_id, from, to)` view summing WAGER_CREDIT − WAGER_DEBIT for replacing `vin_total` reads.
3. Add `derived_deposit_total(user_id)` view replacing `recharge_money`.
4. Wire all three views into a new internal admin endpoint `c=9990` and verify outputs match current column reads on 100 random users.
5. Add Prometheus metric `money_drift_users{currency, severity}` updated by hourly drift job.
6. Lint extension: add `cgame.users.cash`, `cash_safe`, `cash_silver`, `users.safe`, `users.vin_total`, `users.xu`, `users.xu_total` to canonical banned-update set in `Dockerfile` lint scan. Adding new allowlist files requires SUN-XXXX ticket.

**Gate to advance:** Drift metric reads zero for 7 consecutive days.

**Rollback:** Pure additive — nothing to roll back.

---

### Phase 1 — Stop writing `vin_total` / `xu_total` (1 week)

**Reads still work — we keep the columns frozen in place, just stop updates. Future reads come from the derived view (replaced earlier in callers).**

**Tasks:**
1. Create `update_money_db_v2(user_id, amount, money_type)`:
   ```sql
   IF money_type='vin' THEN UPDATE users SET vin=vin+amount WHERE id=user_id;
   ELSE UPDATE users SET xu=xu+amount WHERE id=user_id;
   END IF
   ```
   Old `update_money_db` keeps writing `vin_total` until killed in phase 4.
2. Add Hazelcast flag `UNIFIED_WALLET_PHASE_1` checked in `UserDaoImpl.updateMoneyDB`. When `on`: call `update_money_db_v2`. When `off`: legacy. `shadow`: call both, write `vin_total += 0` in v1 (still consistent).
3. Replace every read of `users.vin_total` / `xu_total` with `derived_player_pnl` view. Audit:
   - `c=9985` (TodayReport4Agency)
   - `c=9986` (ReportGeneral4Agency)
   - `c=9910` (admin user list)
   - `ListAllAgentsUnderAgentProcessor.java:337`
   - `BalanceGuard.clamp` (the "display flipped to negative" guard — should become a no-op once `vin_total` is decoupled)
4. Snapshot + drift monitor armed.
5. Flip flag to `on` in staging. Run for 7 days. Drift must stay zero (ledger writes from MoneyGateway already produce same numbers).

**Gate to advance:** 7 days zero drift in staging + agency reports byte-identical on 30-day backfill.

**Rollback:** Flip flag to `off`. SP `update_money_db_v2` reverts to v1 path; `vin_total` resumes incrementing. Drift detected (since old code did increment) — accept and document the recovery window.

#### Phase 1 implementation checklist (completed pre-soak)

- [x] SQL migration `install/config/mysql/migrations/20260512_phase1_update_money_db_v2.sql` ships the additive v2 SP (writes vin/xu only). Legacy `update_money_db` left in place.
- [x] Java DAO `UserDaoImpl.updateMoney` reads `UNIFIED_WALLET_PHASE_1` env var (`off`/`shadow`/`on`) and routes the `CALL` accordingly. Shadow mode bumps `UPDATE_MONEY_V2_SHADOW_HITS` for staging validation.
- [x] Reconciliation query `UserDaoImpl.GetNickNameFreeze()` rewritten from `WHERE vin != vin_total` → JOIN against `v_wallet_drift` view (addendum H4 / runbook Phase 1 special-care item).
- [x] Phase 4 migration `install/config/mysql/migrations/20260601_phase4_drop_legacy_sp_and_total_columns.sql` staged (DO NOT APPLY until 14-day soak passes).
- [x] Smoke test `tests/wallet-unification/phase1_smoke.sh` exercises game win, game loss, admin topup, and verifies `v_derived_player_pnl` agrees with the ledger.
- [ ] **PENDING:** flip `UNIFIED_WALLET_PHASE_1=shadow` on staging game servers + APIs; observe `UPDATE_MONEY_V2_SHADOW_HITS` rises in step with legacy SP call rate (≥7 days).
- [ ] **PENDING:** flip to `on` in staging; run smoke daily for 14 days; wallet_drift_snapshot.drifting_users must stay 0.
- [ ] **PENDING (deferred to in-soak audit):** rewrite remaining `vin_total` READ sites listed in the audit log (admin reports, agent listings, user-detail). Read sites are safe during Phase 1 — column holds the frozen pre-cutover snapshot; values gradually become stale relative to ledger PnL until Phase 4 drops the column entirely.

#### Read-site audit (vin_total / xu_total references found in tree)

40 read sites in 16 files. Critical (write-after-Phase-1 breakage) is exactly one — already fixed:

| File | Risk | Action this phase |
|---|---|---|
| `UserDaoImpl.java` `GetNickNameFreeze()` | CRITICAL — query returns every user after Phase 1 starts | **Rewritten to use `v_wallet_drift`** |
| `MoneyGateway.systemRecoveryReset()` lines 1230, 1248 | CRITICAL — boot-time bulk `UPDATE users SET vin = vin_total` | Will fail after Phase 4 column drop. Must be retired or reworked to read from ledger before Phase 4 ships. **Flagged for follow-up.** |
| `ReportDaoImpl.getTotalMoney`, `getTotalPnl` | Admin agency dashboard reads — stale post-cutover | Deferred to in-soak: rewrite to `v_derived_player_pnl` once staging is stable |
| `ListAllAgentsUnderAgentProcessor.java:262/326/341` | Agent "profit" column | Deferred to in-soak |
| `AgentDAOImpl.java:991/1269/1300/1358` | Agent CRUD, listUserOfAgent reports | Deferred to in-soak |
| `CreateUserProcessor.java:121` `INSERT … vin_total` | Writes 0 to vin_total on user create | Pre-Phase-4 cleanup: drop column from INSERT |
| `BulkLoadCacheProcessor.java` | Boot-time Hazelcast `users` cache population | Stale but coherent during soak. Pre-Phase-4: drop column from SELECT. |
| `GetUserDetailProcessor.java`, `ListUsersProcessor.java`, `MoneyInGameDaoImpl.java`, `AgentDAOImpl.AddNewUser` (INSERT) | Admin UI reads + user-create INSERT | Pre-Phase-4: drop column references |
| `UserUtil.parseResultSetToUserModel`, `DBFields.VIN_TOTAL`, `UserModel.getTotalPnl/setTotalPnl`, `BalanceGuard` | Model accessors / banned-method constants | Keep through Phase 4; remove with the column drop |
| `MoneyGatewayDualWriteTest.java` | Test that asserts vin_total never moves | Already aligned with Phase 1 invariant — leave |

---

### Phase 2 — Retire `safe` → `LOCK_FUND` / `UNLOCK_FUND` (1 week)

**Today:** `MoneyInGameServiceImpl.subSafeMoney` moves vin↔safe directly with raw UPDATE.

**Target:** Replace with two-entry ledger transactions:
- LOCK_FUND: DEBIT PLAYER_WALLET, CREDIT PLAYER_VAULT
- UNLOCK_FUND: opposite

`PLAYER_VAULT` is a new `account_type` per player.

**Tasks:**
1. Create `MoneyGateway.lockFunds(userId, amount, source, txId)` and `unlockFunds(userId, amount, source, txId)`. Standard CreditResult signature.
2. Migrate `MoneyInGameServiceImpl.subSafeMoney` to call gateway methods. Keep public method signature.
3. One-shot migration script: for every user with `safe > 0`, post a single `LEGACY_SAFE_MIGRATION` transaction that DEBITs LEGACY_RECONCILIATION (existing system account) and CREDITs PLAYER_VAULT for the safe amount. Reset `users.safe = 0` in same tx.
4. Add lint allowlist for the migration script only (`SUN-XXXX-safe-migration` allow-pat entry).
5. After 14 days zero traffic on legacy path, drop `users.safe` column.

**Gate to advance:** Zero callers of legacy `safe`-touching code; sum of all PLAYER_VAULT credits = pre-migration sum of `users.safe`.

**Rollback:** Re-add `users.safe` column (restore from snapshot if needed). Revert MoneyGateway changes. `LEGACY_SAFE_MIGRATION` transactions stay logged for audit.

---

### Phase 3 — Retire `xu` / `xu_total` (1 week)

xu is test/promo currency. Two options confirmed with PM:

**Option A (preferred):** xu → vin at 1:1, retire xu entirely.
**Option B:** xu becomes a separate `PLAYER_PROMO_WALLET` currency='VND' account, segregated from main wallet.

**Tasks (Option A):**
1. Find all uses of xu in production. Migrate balances: for every user with `xu > 0`, post `XU_TO_VIN_MIGRATION` transaction crediting PLAYER_WALLET by the xu amount, debit LEGACY_RECONCILIATION.
2. Remove xu branch from `update_money_db_v2`.
3. Update all xu-aware response models to return 0.
4. After 14 days, drop `users.xu` and `users.xu_total`.

**Gate to advance:** 30 days production traffic without any xu-aware processor invocation; balance reconciliation OK.

**Rollback:** Restore from snapshot; revert xu-aware code.

---

### Phase 4 — Kill `update_money_db` legacy SP (1 week)

After phase 1 has run successfully on the v2 SP, fully remove the legacy.

**Tasks:**
1. Drop `update_money_db` stored procedure.
2. Drop `users.vin_total` and `users.xu_total` columns.
3. Remove `BalanceGuard` since the bug it protects against no longer exists.
4. Verify all 17 game servers + 4 APIs build and pass smoke.

**Gate to advance:** Build green + smoke pass for every container.

**Rollback:** Recreate SP from snapshot; restore columns. Heavy rollback — only consider in first 48h.

---

### Phase 5 — Retire BanCa `cash` / `cash_safe` / `cash_silver` (2 weeks) — HIGHEST RISK

**Today:** BanCa .NET writes `cgame.users.cash` directly. Vin↔cash transfer is the only ledger touchpoint.

**Target:** BanCa reads/writes `PLAYER_WALLET` via Java MoneyGateway HTTP calls. BanCa cash columns disappear.

**Tasks:**
1. Extend MoneyGateway with `WAGER_DEBIT_BANCA` / `WAGER_CREDIT_BANCA` endpoints (already supported as WAGER_DEBIT/CREDIT, just need BanCa to call them). Per-shot HTTP latency must be ≤5ms p99.
2. BanCa `RedisManager.IncEpicCash` is replaced by an in-memory session balance synced from PLAYER_WALLET at session start; every shot debits via HTTP.
3. Per-shot reconciliation: every 60s the BanCa server reconciles its in-memory cache vs PLAYER_WALLET. Mismatch → kick player + alert.
4. Migration: snapshot cgame.users. For every active session, post `BANCA_CASH_MIGRATION` transaction crediting PLAYER_WALLET by cash amount, debit LEGACY_RECONCILIATION. Cash_safe / cash_silver: convert 1:1 to VND or drop per PM decision.
5. Blue/green deploy. Run old + new path side-by-side for 7 days; gradually shift % traffic via Hazelcast flag.
6. After 14 days zero traffic on old code, drop `cgame.users.cash*` columns.

**Gate to advance:** Per-session reconciliation drift zero for 7 days; latency p99 ≤5ms.

**Rollback:** Hazelcast flag flips all traffic back to legacy path. BANCA_CASH_MIGRATION transactions stay logged.

---

### Phase 6 — Retire VIP columns (3 days)

Low-risk analytics-style refactor.

**Tasks:**
1. Create `PLAYER_VIP_POINTS` account_type with currency='VP'. Migrate `vip_point` balances 1:1.
2. Create `vip_point_log` (transaction header for VIP points, separate from money). Or reuse `money_transaction` with currency='VP'.
3. Switch reads/writes via `VipService` wrapper.
4. Drop `vip_point`, `vip_point_save`, `money_vp`.

**Gate to advance:** VIP-related processors return identical values pre/post.

**Rollback:** Restore columns from snapshot; revert wrapper.

---

### Phase 7 — Retire analytics columns (1 day)

`recharge_money` / `gift_total`:
1. Build `derived_deposit_total` view (already done in Phase 0).
2. Switch reads to view.
3. Drop columns.

**No-rollback needed** (pure analytics).

---

## 7. Validation Per Wave

Each wave's gate runs these checks. Test artifacts committed to `tests/wallet-unification/`:

1. **Balance invariant:** `for each user_id: derived_balance == users.vin`. Tolerance 0 VND.
2. **Sum invariant:** `SUM(derived_balance for all players) + SUM(system account balances) = 0` per currency.
3. **Replay determinism:** Take last 24h of production transactions, replay in test DB. Final account state must match production.
4. **Idempotency:** Re-post all transactions a second time. Each one must error with `DUPLICATE_TRANSACTION` and no balance change.
5. **Reversibility:** Pick 100 random transactions, post their reversals, verify accounts return to pre-tx state.
6. **Smoke pack:** 
   - Player deposit + withdraw round-trip → vin unchanged
   - Player place bet + cash out → vin = original − bet + payout
   - Admin topup + deduct → vin unchanged, no `vin_total` movement
   - BanCa transfer in + out → vin unchanged
   - Crypto deposit + withdraw → vin unchanged after FX rate stable
   - Agent commission earn + claim → agency_wallet + player_wallet balance correct
   - Giftcode redeem → vin += code value, no other column changed

All 6 must pass before flipping each wave's flag from shadow to on.

---

## 8. Open Questions for PM Sign-off

1. **xu retirement option** — A (collapse to vin) vs B (separate promo wallet). Decision needed before Phase 3.
2. **BanCa cash_safe / cash_silver semantics** — keep or drop. Decision needed before Phase 5.
3. **Multi-currency wallet** — defer (current scope) or include USDT-denominated wallet in this RFC. Defer recommended.
4. **VIP point ledger** — full double-entry with money_transaction (heavy) or simple append-only log (light). Light recommended; VIP points are not real money.
5. **`agency_wallet` / `credit_wallet`** — leave separate or fold into `money_account`. Recommendation: fold as `account_type=AGENCY_WALLET` / `AGENT_CREDIT_LINE` to get unified reconciliation. New ticket if accepted; not blocking this RFC.

---

## 9. Out of Scope

- Agency wallet / credit wallet unification (separate RFC if approved).
- Cross-tenant FX between VND and other fiat currencies.
- Real-time hot path optimization beyond current MoneyGateway P99.
- Frontend rendering changes (FE keeps reading `vin` until BE confirms removal — staged with FE team in Phase 4).

---

## 10. Acceptance Criteria

System is "single-wallet unified" when:

1. `vinplay.users` contains exactly these money-related columns: `vin` (denormalized read cache only).
2. Every wallet movement in production reads from `money_account` / `money_entry`.
3. Sum of all PLAYER_WALLET balances + system account balances = 0 per currency, validated hourly.
4. `cgame.users.cash*` columns dropped.
5. Lint scan finds zero raw UPDATEs against retired columns.
6. All 6 smoke tests pass for 30 consecutive days.
7. Drift monitor metric reads zero for 30 consecutive days.

---

## 11. Estimated Timeline

| Phase | Duration | Risk |
|---|---|---|
| 0 — Hardening | 1 week | Low |
| 1 — Stop vin_total writes | 1 week | Medium |
| 2 — Retire safe | 1 week | Medium |
| 3 — Retire xu | 1 week | Low |
| 4 — Kill legacy SP | 1 week | Medium |
| 5 — BanCa cash migration | 2 weeks | **High** |
| 6 — VIP retire | 3 days | Low |
| 7 — Analytics retire | 1 day | Low |
| **Total** | ~8 weeks | — |

Add 2 weeks contingency for surprise discoveries → 10 weeks calendar.

---

## 12. Required Reviews Before Build

- **Architect** (Opus): does the ledger model survive future requirements (multi-jurisdiction, regulatory audit, multi-currency)?
- **Security**: can we prove no fund creation/destruction outside the gateway?
- **Operations**: rollback procedures correct for each wave?
- **Product**: open questions in §8 answered.

Sign-off from all four required before starting Phase 1.

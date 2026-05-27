# RFC v2 Addendum — Single-Wallet Unification

**Supersedes blocking issues in v1.**
**Status:** Required reading before Phase 0 starts.
**Created:** 2026-05-11 after architect + security + critic review.

---

## Verdict

Original RFC verdict from reviewers: **REVISE before any execution past Phase 0.**

Phase 0 (hardening only — pure additive groundwork, no behavior change) is safe to start while the rest is revised. Phases 1+ blocked until the items below are resolved.

---

## 1. Critical Blockers (must fix before Phase 1)

### B1. SafeBox MongoDB shadow ledger
**Source:** architect review
**Found at:** `backend-master/VinPlayDAL/src/main/java/com/vinplay/safebox/dao/impl/SafeBoxDaoImpl.java:23-75` — writes to MongoDB collection `safe_box` (NOT `users.safe`). Phase 2 migration as drafted would miss every balance in MongoDB.
**Fix:** Phase 2 reconciles BOTH `users.safe` AND `mongo.safe_box.amount`. Pre-migration step: cross-reference, resolve discrepancies, then post `LEGACY_SAFE_MIGRATION`. Update Phase 2 scope to include MongoDB drain.

### B2. SP legacy bypass via `UserServiceImpl.updateMoney`
**Source:** security review
**Found at:** `backend-master/VinPlayUserCore/src/main/java/com/vinplay/usercore/service/impl/UserServiceImpl.java` — 30+ callers route through `UserServiceImpl.updateMoney` → `UserDaoImpl.updateMoney` → SP `update_money_db` (v1, unconditional). Phase 1 flag check at the DAO is a different method (`updateMoneyDB`) and does not intercept these callers.
**Fix:** Move flag check up to `UserServiceImpl.updateMoney`. Alternative: rename SP to `update_money_db_DEPRECATED` and swap callers to `update_money_db_v2`. Phase 1 cannot ship until the bypass is closed.

### B3. GSC + AWC seamless wallet third-party integrations not in plan
**Source:** critic review
**Found at:** `GscDepositAggregator`, `GscWithdrawAggregator`, `GscRollbackAggregator`, `GscCancelAggregator`, `GscTransferAggregator`, `AwcCallbackProcessor` (~1215 lines). Process ~846 events/day from third-party game providers. Each callback writes via MoneyGateway.
**Fix:** Add explicit "Third-party seamless wallet compatibility" section to RFC. Every phase gate adds "GSC + AWC callback smoke test passes" as a validation item. Provider-specific debit/credit sources (`SOURCE_GSC_DEBIT`, `SOURCE_AWC_DEBIT`) explicitly documented in §3 transaction-type table.

### B4. Phase 5 (BanCa) timeline is fiction
**Source:** architect + critic (both flagged)
**Issue:** BanCa is .NET 5 → Java HTTP across Docker networks. 5ms p99 per fish shot is physically implausible. BanCa has 12 C# files touching cash (`GameBanCa.cs`, `LobbyService.cs`, `BanCaServer.cs`, `RedisManager.cs`, `LotoGame.cs`, `OneTwoThreeBoard.cs`, etc.) plus sub-games (Loto, OneTwoThree, Slot5).
**Fix:** Phase 5 rebudgeted to 6 weeks. Split into:
- **5a (1wk):** Build session-batch design + latency benchmark (`p99 ≤ batched 100ms` not per-shot 5ms)
- **5b (1wk):** Migrate main BanCa game loop
- **5c (1wk):** Migrate Loto / OneTwoThree / Slot5 sub-games
- **5d (1wk):** Shadow mode under traffic
- **5e (2wk):** Soak + cleanup
Per-shot HTTP replaced with **session-batch**: BanCa holds in-memory balance, settles every 5s or session-end. Trade-off: brief window of in-memory divergence (acceptable; reconciliation drains on session close).

---

## 2. High-Severity Fixes

### H1. Sign convention prose vs SP behavior (architect)
SP `post_money_transaction` computes `balance_after = balance_before + amount` for CREDIT and `... - amount` for DEBIT uniformly. System (liability) accounts go negative by design. RFC prose says "increase via DEBIT" for liabilities — true accounting-wise but **the SP doesn't implement that flip**. Document explicitly: **global invariant is `SUM(all account balances) = 0`, system accounts naturally hold negative balances.**

### H2. Lint scan coverage gaps (security)
Current Dockerfile lint scans `VinPlayDAL VinPlayUserCore VinPlayCardLib VbeeCommon CardCoreLib api/`. **Missing:** `game/` (17 game servers) and `banca/` (.NET, needs separate C# lint).
**Fix:** Add `game/` to Java lint. Add C# lint step to BanCa Dockerfile for `UPDATE cgame.users` patterns.

### H3. Cache-vs-ledger race + silent failure swallowing (security)
MoneyGateway commits MySQL first, then ledger dual-write in catch-all try-catch. Failure here = legacy column updated, ledger missing entry → silent drift.
**Fix:** Failed dual-write writes to `failed_dual_write` table (new). Hourly drift job picks up + retries. Alert if retry queue grows.

### H4. `vin_total` callsite audit incomplete (critic)
Phase 1 lists 5 callers. Actual count: 30+ files / 40+ lines.
**Fix:** Run full grep audit before Phase 1 starts. Commit inventory to RFC. Particularly check `UserDaoImpl.java:1113` `WHERE vin != vin_total` — reconciliation query that breaks silently once writes stop.

### H5. Rollback dishonesty after Phase 2 (critic)
"Restore from snapshot" loses every transaction since snapshot.
**Fix:** Rewrite §6 rollback per phase. Phases 3–4–6–7 explicitly labeled **fix-forward only**. Snapshot restore = DR, not rollback.

### H6. Zero-tolerance balance invariant (critic)
Concurrent in-flight game sessions cause apparent drift until settled.
**Fix:** Drift invariant scoped: "0 VND drift for users with no active game session; users with active session reconcile within 1h of session close." Drift monitor excludes active sessions.

### H7. BanCa session reconnect double-credit (security)
Phase 5 migrates "per active session" — reconnect creates new session, new migration credit.
**Fix:** Idempotency `external_ref = banca_migration:{userId}` (not per-session). Drain (kick) sessions first, migrate from DB snapshot.

---

## 3. Medium-Severity Fixes

### M1. REVERSAL_* idempotency invariant
**Fix:** SP enforces: any `transaction_type LIKE 'REVERSAL_%'` MUST have non-null `correlated_transaction_id`. Add CHECK constraint or trigger.

### M2. Phase 4 rollback window
**Fix:** Phase 4 gate matches phase 1–3: 7-day soak. Keep SP + columns alive (unused) for 14 days before drop.

### M3. Agency/credit wallet permanent split
**Fix:** Phase 0 seeds `AGENCY_WALLET` and `AGENT_CREDIT_LINE` account_types in `money_account` so commission flows get ledger rows from day 1. Full migration deferred to follow-up RFC.

### M4. Allowlist abuse
**Fix:** Move money-lint allowlist to `backend-master/config/money-lint-allowlist.txt`. CODEOWNERS requires 2-reviewer approval. CI validates SUN-XXXX reference points to open Jira ticket.

### M5. Snapshot encryption
**Fix:** Snapshots encrypted with `gpg --symmetric`. Key from `.env`. Stored `chmod 600`, owned root. 90-day retention enforced via cron.

### M6. Migration audit metadata
**Fix:** Mandate metadata schema for all LEGACY_* / BACKFILL_* transactions: `{operator, jira_ticket, batch_id, run_timestamp, host}`. CHECK constraint or SP validation.

### M7. xu→vin 1:1 phantom money (architect)
**Fix:** Phase 3 Option A sources from `PROMO_POOL` (not `LEGACY_RECONCILIATION`). House P&L correctly reflects cost of converting promotional balance.

---

## 4. Low-Severity / Hardening

### L1. Currency CHECK constraint
**Fix:** Add `CHECK (currency IN ('VND','VP'))` on `money_account` now. Expand when USDT support ships.

### L2. Phase 0 gate window
**Fix:** Phase 0 gate is 14-day (not 7-day) zero drift. First-time drift monitor needs longer baseline.

---

## 5. Open Questions Promoted to Blockers

These were "for PM signoff" in v1 — promoted to **block Phase 1**:

1. **xu retirement option** (A=collapse to vin, B=separate promo wallet) — scope-defining
2. **BanCa cash_safe / cash_silver semantics** — keep or drop, must know before Phase 5 design
3. **USDT gateway DB access** — does `sunkr-usdt-gateway/` read `users.vin` directly bypassing MoneyGateway? Check before Phase 1
4. **admin-php (sunkr-admin) raw SQL** — sibling repo may have direct queries on retired columns
5. **agency_wallet / credit_wallet unification** — fold in or stay separate (see M3)

---

## 6. Phase 0 — Safe Work Authorized Now

The following can ship while v2 RFC bakes — pure additive, no behavior change:

1. SQL views: `v_derived_player_balance`, `v_derived_player_pnl`, `v_derived_deposit_total`
2. Dockerfile lint: add `game/` to scan directories
3. `money_account` CHECK constraint: `currency IN ('VND','VP')`
4. Drift monitor scaffolding (table + cron stub, alerts disabled)
5. Seed `AGENCY_WALLET` + `AGENT_CREDIT_LINE` account types for active agents

Phase 0 ships under feature flag `UNIFIED_WALLET_PHASE_0=enabled` (default on; nothing depends on it yet so safe).

Everything past Phase 0 stays blocked until:
- B1, B2, B3, B4 fixed
- 5 open questions resolved with PM
- Updated phase-by-phase plan reviewed by same 3 reviewers (architect, security, critic) again

---

## 7. Timeline Revision

| Phase | Original | Revised | Notes |
|---|---|---|---|
| 0 — Hardening | 1 week | 2 weeks | 14-day gate + reviewer items |
| 1 — Stop vin_total writes | 1 week | 2 weeks | Full callsite audit before flip + B2 fix |
| 2 — Retire safe | 1 week | 2 weeks | MongoDB safe_box drain + reconcile |
| 3 — Retire xu | 1 week | 1 week | Option A only after PROMO_POOL source fix |
| 4 — Kill legacy SP | 1 week | 2 weeks | 7-day soak + 14-day column-keep |
| 5 — BanCa migration | 2 weeks | 6 weeks | sub-phases 5a–5e |
| 6 — VIP retire | 3 days | 1 week | account_type seeding + lint |
| 7 — Analytics retire | 1 day | 3 days | |
| **Total** | 8 weeks | **~16 weeks** | + 2wk contingency = ~18 weeks |

Original "8 weeks" was a planning fiction. Realistic floor: 14 weeks. Calendar with surprises: 18 weeks.

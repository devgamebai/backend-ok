# Wallet Drift Analysis — 2026-05-11

**Run from:** Phase 0 drift analyzer views.
**Scope:** 13.8M `money_gateway_log` rows vs 20,330 `money_transaction` rows.
**Bottom line:** Ledger covers **0.15%** of historical money movements. Not yet authoritative.

---

## Coverage by source (gateway_log vs ledger)

| Source | Log rows | Gross VND | Users affected | Ledger coverage |
|---|---|---|---|---|
| **USERSERVICE_GAME** | 13,839,074 | 2,289,193,048,719 | 5,418 | **0** ← legacy SP, no dual-write |
| AWC_DEBIT | 4,222 | 630,305,850 | 24 | ~partial via WAGER_DEBIT |
| AWC_CREDIT | 2,082 | 603,138,797 | 24 | **0** ← no WAGER_CREDIT txs |
| VIPPOINT_UPDATE | 117 | 354,410,745 | 39 | 117 ✓ full coverage |
| GSC_DEBIT | 62 | 231,200 | 3 | ~partial |
| PROMO_BONUS | 43 | 7,796,500 | 42 | **0** |
| DEPOSIT_TELEGRAM | 42 | 33,276,000 | 38 | **0** |
| ADMIN_TOPUP | 30 | 137,400,000 | 20 | 11 partial |
| GSC_CREDIT | 25 | 153,660 | 3 | **0** |
| DEPOSIT_BANK | 6 | 11,665,000 | 5 | 1,306 (more in ledger than log!) |
| CONVERT_AGENCY_TO_VIN | 5 | 4,669 | 2 | 0 |
| CASHBACK_PAYOUT | 1 | 10,000 | 1 | 0 |
| REFUND_WITHDRAW | 1 | 111,111 | 1 | 0 |
| WITHDRAW_BANK | 1 | 9,000,000 | 1 | 1 ✓ |

---

## Drift breakdown — top 10 affected users

| user_id | nickname | users.vin | ledger | drift | userservice_game | other | backfill? |
|---|---|---|---|---|---|---|---|
| 50002 | laviai | 34,918,310 | 77,184,182 | **-42,265,872** | -15,002,000 | -16,978,355 | yes |
| 50072 | conthoanco | 2,508,418 | 0 | +2,508,418 | 0 | +2,508,418 | no |
| 50078 | casmoif | 1,317,887 | 0 | +1,317,887 | 0 | +1,317,887 | no |
| 50083 | vqgbachma | 1,302,731 | 0 | +1,302,731 | 0 | +1,302,731 | no |
| 50077 | camattrang | 1,300,950 | 0 | +1,300,950 | 0 | +1,300,950 | no |
| 50068 | vuonquocgia | 1,300,191 | 0 | +1,300,191 | 0 | +1,300,191 | no |
| 50073–50076 | conthoiadun, etc. | 1,300,000 | 0 | +1,300,000 | 0 | +1,300,000 | no |

**Drift patterns:**

1. **`laviai` (legacy player):** Has backfill row. Ledger over-credits by 42M because dual-write missed `USERSERVICE_GAME` losses (-15M) AND some `other_sources` events (-17M). Net: ledger thinks balance is 77M, actual vin is 35M.

2. **Users 50068–50083 (newer accounts):** No `BACKFILL_INITIAL_BALANCE` ever ran for them. They were created after the one-time backfill cutoff. All their balance comes from non-game sources (probably admin top-ups + promo) that bypassed dual-write.

---

## Coverage gaps requiring fix (in priority order)

### CRITICAL — `USERSERVICE_GAME` (99.85% of all log rows)
Source: `UserServiceImpl.updateMoney` → SP `update_money_db` (the legacy stored procedure).
Reason: SP touches `users.vin` directly; MoneyGateway is never invoked.
Impact: every game win/loss across all 17 game servers misses the ledger.
Fix scope: **THE central blocker.** Either route the SP through MoneyGateway or have MoneyGateway dual-write to ledger from SP context.

### HIGH — `AWC_CREDIT` (2082 rows, 603M)
Source: `AwcCallbackProcessor.handleSettle / handleBetNSettle`.
Reason: AWC settle path likely calls `creditUser` but with a source that maps to WAGER_DEBIT (or fails dual-write). The 0 WAGER_CREDIT ledger txs proves nothing is landing.
Fix scope: audit AWC code path, fix source mapping.

### HIGH — `PROMO_BONUS`, `DEPOSIT_TELEGRAM`, `GSC_CREDIT` (combined 110 rows, ~41M)
All zero ledger coverage. Each needs a code-path audit + fix to invoke MoneyGateway with the correct typed source.

### MEDIUM — `CASHBACK_PAYOUT`, `REFUND_WITHDRAW`, `CONVERT_AGENCY_TO_VIN` (7 rows)
Low volume but principled — every money type must reach ledger.

### LOW — Reverse asymmetry on `DEPOSIT_BANK`
Gateway log has 6 rows, ledger has 1,306. Investigate: ledger may have entries from sources OTHER than DepositApprovalService (manual ops? schema?).

---

## Strategic decision required from operator/PM

The ledger cannot become authoritative until coverage gaps close. Three paths:

### Option A — Mass replay backfill (most rigorous)
1. Replay every `money_gateway_log` row chronologically into `money_transaction` + `money_entry`.
2. Idempotency key: `(source, gateway_log.id)`.
3. Validate per-user `vin == SUM(money_entry)` after replay.
4. Estimated cost: ~6h batch run, ~28M new entry rows, ~14M new transaction rows.
5. **Result:** Full historical replay possible. Ledger becomes authoritative.

**Pros:** Complete audit trail back to 2026-04-19. Phase 1 onwards is honest.
**Cons:** Heavy DB write; storage grows ~5GB; legacy `USERSERVICE_GAME` is a single-source event (not double-entry-natural) — need to choose HOUSE_GAME_POT as counter-account for every game tx.

### Option B — Reset baseline (most pragmatic)
1. For every drifting user, post a `BACKFILL_RESET` transaction debiting LEGACY_RECONCILIATION and crediting PLAYER_VIN by `users.vin - ledger.balance`.
2. Ledger snaps to current `users.vin` as truth.
3. Fix coverage gaps going forward.
4. Estimated cost: ~5K user adjustments, <1h.
5. **Result:** Ledger matches `users.vin` now. Historical era is closed.

**Pros:** Fast. Phase 1 unblocked in days.
**Cons:** Pre-baseline period has no per-tx audit. Auditors must rely on `money_gateway_log`. Loses replay determinism property the RFC promised.

### Option C — Forward-fix only (most surgical)
1. Don't touch existing drift.
2. Fix dual-write coverage for new transactions (close all gaps listed above).
3. Run analyzer daily — drift count must trend down, never up.
4. After 30 days clean coverage, decide A or B.

**Pros:** No big bang. Each fix is its own ticket.
**Cons:** Drift persists for 30+ days. Phase 1 gate timeline extends.

---

## Recommendation

**Option C → then Option B once C stabilizes.**

Rationale:
- Closing coverage gaps (C) is required regardless of A/B choice. Without it, Option A would replay 13.8M rows then immediately accumulate new drift from un-fixed paths.
- Option B is simpler than A and matches operator preference for forward-momentum (per recent MRs and CLAUDE.md).
- Mass replay (A) is only worth it if regulatory audit demands full chronological reconstruction. For Vietnamese casino ops, source-of-truth is sufficient.

**Concrete next steps if you accept the recommendation:**

1. SUN-1320 — Route `update_money_db` through MoneyGateway (the 99.85% blocker)
2. SUN-1321 — Audit & fix `AWC_CREDIT` dual-write
3. SUN-1322 — Audit & fix `PROMO_BONUS`, `DEPOSIT_TELEGRAM`, `GSC_CREDIT`
4. SUN-1323 — Audit `CASHBACK_PAYOUT`, `REFUND_WITHDRAW`, `CONVERT_AGENCY_TO_VIN`
5. SUN-1324 — Investigate `DEPOSIT_BANK` reverse asymmetry
6. SUN-1325 — Hourly drift cron + Prometheus metric
7. SUN-1326 — After 7 days clean coverage, Option B reset baseline

Open with operator before any code changes.

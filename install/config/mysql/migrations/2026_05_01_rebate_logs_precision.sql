-- SUN-1209: increase rebate_logs amount-column precision from
-- DECIMAL(20,2) → DECIMAL(20,4) so per-bet commission can be stored
-- losslessly. With 2 decimals, `volume × rate / 100` is rounded once
-- per bet and the rounding errors accumulate over many bets — QC
-- reports total commission in LSR sums drifting from the one-shot
-- expected value (e.g. 343.17 stored vs 343.14 expected, +0.03 drift
-- across 15 bets). Commission rates go to 2 decimals (1.25%, 1.15%,
-- 1.05%), so 4 decimals on volume × rate / 100 is always lossless,
-- closing the drift entirely. Display layer rounds back to 2 decimals
-- when rendering — SUMs are exact at storage time, so the rounded
-- display matches the one-shot mathematical expectation.
--
-- Wallet (`agency_wallet.balance` BIGINT, whole vin) stays unchanged —
-- per-bet wallet credit still rounds to whole vin via amountForWallet.
-- The fractional-vin drift between LSR display and wallet sum is
-- bounded (max 1 vin per bet) and intrinsic to whole-vin payouts.
--
-- Compatibility: DECIMAL widening is in-place metadata-only on InnoDB;
-- existing rows are read with their original 2-decimal value padded
-- with zeros (211.26 → 211.2600). No data loss. New rows from the
-- updated writer will populate the extra digits.

ALTER TABLE vinplay.rebate_logs
  MODIFY COLUMN rebate_amount DECIMAL(20,4) NOT NULL DEFAULT 0.0000,
  MODIFY COLUMN share_amount  DECIMAL(20,4) DEFAULT NULL,
  MODIFY COLUMN net_rebate    DECIMAL(20,4) DEFAULT NULL;

-- The daily rollup table aggregates rebate_logs into per-day commission
-- totals. Widen its sum column too so the rollup pipeline doesn't
-- re-introduce per-day rounding drift on top of the lossless per-bet
-- storage. Existing rows pad zeros (e.g. 343.17 → 343.1700).
ALTER TABLE vinplay.rebate_daily_rollup
  MODIFY COLUMN sum_commission DECIMAL(20,4) NOT NULL DEFAULT 0.0000;

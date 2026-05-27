-- SUN-1150 — widen rebate money columns to DECIMAL(20,2) so fractional
-- KRW commission amounts (e.g. 0.80 from a small bet × differential pct)
-- are no longer silently truncated.
--
-- Before: rebate_amount BIGINT — every fractional value lost twice
--   (compute used .divide(100, 0, FLOOR), then BIGINT storage truncated).
-- After: DECIMAL(20,2) — 2-decimal precision preserved through write+read.
--
-- Also widening share_amount and net_rebate (same units, same problem).
--
-- DECIMAL(20,2) holds values up to ~10^18 with 2 decimal places — way
-- beyond any realistic rebate amount even after years of accumulation.
-- BIGINT → DECIMAL widening is metadata-only on InnoDB; minimal cost.

ALTER TABLE vinplay.rebate_logs
    MODIFY COLUMN rebate_amount DECIMAL(20,2) NOT NULL DEFAULT '0.00',
    MODIFY COLUMN share_amount  DECIMAL(20,2) NULL DEFAULT NULL,
    MODIFY COLUMN net_rebate    DECIMAL(20,2) NULL DEFAULT NULL;

-- Cashback tables share the same writer path. Widen them too so
-- fractional cashback amounts aren't silently truncated.
ALTER TABLE vinplay.tbl_cashback_logs
    MODIFY COLUMN rebate_amount DECIMAL(20,2) NOT NULL DEFAULT '0.00';
ALTER TABLE vinplay.tbl_cashback_log_game_detail
    MODIFY COLUMN rebate_amount DECIMAL(20,2) NOT NULL DEFAULT '0.00';

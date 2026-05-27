-- SUN-1150 Phase 2a — widen rebate_daily_rollup.sum_commission to DECIMAL(20,2)
-- so the daily aggregate preserves the same precision as rebate_logs.rebate_amount.
-- Without this, even after the read-side fix the rollup-served summaries still
-- truncate fractional KRW.
--
-- Metadata-only on InnoDB; minimal cost.

ALTER TABLE vinplay.rebate_daily_rollup
    MODIFY COLUMN sum_commission DECIMAL(20,2) NOT NULL DEFAULT '0.00';

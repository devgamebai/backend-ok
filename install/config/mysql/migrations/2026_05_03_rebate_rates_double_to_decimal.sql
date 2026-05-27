-- SUN-1248: switch all rebate/cashback RATE columns from DOUBLE to
-- DECIMAL(7,4) so percentage × volume math is exact.
--
-- Why DOUBLE is wrong here
-- ────────────────────────
-- Money in this platform is stored as fixed-point (BIGINT subunit for
-- wallet, DECIMAL(20,4) for rebate amounts after SUN-1209). But the
-- *rate* columns the writers read back to recompute commission were
-- still IEEE-754 DOUBLE. That meant:
--
--   1. Writing 1.05 from Java's `setDouble(1.05)` → MySQL stores
--      0x3FF0CCCCCCCCCCCD (1.0500000000000000444…) — already not
--      exactly 1.05.
--   2. Re-reading via `getDouble` and re-multiplying produced drift
--      that grew with the bet count. SUN-1209 fixed the AMOUNT side
--      by widening rebate_logs.rebate_amount to DECIMAL(20,4); this
--      migration finishes the job on the RATE side so the source
--      value of every commission calculation is exact.
--
-- Why DECIMAL(7,4)
-- ────────────────
-- All rate use cases are 0–100% with at most 2 decimal places today
-- (1.05, 1.15, 1.25, 0.50…). DECIMAL(5,2) is what useragent.commission_rate
-- and game_commission_rate.rate already use. We pick (7,4) here because:
--   - 4 decimals matches SUN-1209's rebate_amount scale, so
--     volume * rate / 100 is lossless when both operands and result
--     share the same scale.
--   - 7 total digits = up to 999.9999, leaving headroom if a future
--     promo program ever needs a >100% rebate (e.g. 150% boost weeks).
--
-- Compatibility
-- ─────────────
-- DOUBLE → DECIMAL is a TYPE change, so MySQL requires ALGORITHM=COPY
-- (it rejects INPLACE with error 1846). InnoDB rewrites the table
-- under shared-lock on read, allowing concurrent reads but blocking
-- writes for the duration. Affected table sizes (2026-05-03 prod):
--   rebate_logs:           ~10K rows  → seconds
--   rebate_config:         dozens     → instant
--   tbl_cashback_config:   dozens     → instant
--   tbl_cashback_game_config: dozens  → instant
-- Net rebate writes blocked for at most a few seconds during the
-- rebate_logs rewrite — acceptable since LogMoneyUserExtraProcessor /
-- RealTimeCommission inserts are queued via RabbitMQ and will simply
-- backlog and drain.
--
-- All values currently stored fit losslessly: rates are stored as 0,
-- 1.05, 1.15, 1.25, etc.  DOUBLE→DECIMAL truncation only affects the
-- "fake" trailing digits that DOUBLE can't represent exactly anyway —
-- e.g. DOUBLE 1.0500000000000000444 → DECIMAL(7,4) 1.0500. That's the
-- desired direction.
--
-- Companion fix: SUN-1209 partially landed (rebate_daily_rollup.sum_commission
-- is DECIMAL(20,4) but rebate_logs.{rebate_amount,share_amount,net_rebate}
-- still DECIMAL(20,2) on prod). We finish that here so writers and
-- readers agree on the storage scale.

-- ─────────────────────────────────────────────────────────────────
-- 1) rebate_config — admin-set per-agent rates
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE vinplay.rebate_config
    MODIFY COLUMN rebate_percentage DECIMAL(7,4) NULL DEFAULT 0,
    MODIFY COLUMN share_percentage  DECIMAL(7,4) NULL DEFAULT 0,
    ALGORITHM=COPY, LOCK=SHARED;

-- ─────────────────────────────────────────────────────────────────
-- 2) rebate_logs — per-bet commission rows (HOT WRITE PATH)
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE vinplay.rebate_logs
    MODIFY COLUMN rebate_percentage DECIMAL(7,4) NULL DEFAULT 0,
    MODIFY COLUMN share_percentage  DECIMAL(7,4) NULL DEFAULT 0,
    ALGORITHM=COPY, LOCK=SHARED;

-- Finish SUN-1209: rebate_logs amount columns to DECIMAL(20,4).
-- 2026_05_01_rebate_logs_precision.sql widened rebate_daily_rollup.sum_commission
-- but the rebate_logs side never got applied to prod (SHOW COLUMNS
-- 2026-05-03 confirms: rebate_amount=decimal(20,2), share_amount=decimal(20,2),
-- net_rebate=decimal(20,2)). Writers (LogMoneyUserExtraProcessor /
-- AutoCommissionPipeline) already pass scale-4 BigDecimal to setBigDecimal,
-- so prior writes silently truncated to scale=2 and accumulated drift.
ALTER TABLE vinplay.rebate_logs
    MODIFY COLUMN rebate_amount DECIMAL(20,4) NOT NULL DEFAULT 0.0000,
    MODIFY COLUMN share_amount  DECIMAL(20,4) NULL DEFAULT NULL,
    MODIFY COLUMN net_rebate    DECIMAL(20,4) NULL DEFAULT NULL,
    ALGORITHM=COPY, LOCK=SHARED;

-- ─────────────────────────────────────────────────────────────────
-- 3) tbl_cashback_config — global cashback program rate
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE vinplay.tbl_cashback_config
    MODIFY COLUMN rebate_percent DECIMAL(7,4) NULL DEFAULT 0,
    ALGORITHM=COPY, LOCK=SHARED;

-- ─────────────────────────────────────────────────────────────────
-- 4) tbl_cashback_game_config — per-game cashback override
--    (read by RealTimeCommission.getPlayerCashbackRate every bet)
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE vinplay.tbl_cashback_game_config
    MODIFY COLUMN rebate_percent DECIMAL(7,4) NULL DEFAULT 0,
    ALGORITHM=COPY, LOCK=SHARED;

-- ─────────────────────────────────────────────────────────────────
-- Verification
-- ─────────────────────────────────────────────────────────────────
-- After apply, all five columns must report decimal(7,4) (rates) or
-- decimal(20,4) (amounts). Run as a sanity check:
--
--   SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
--   FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA='vinplay'
--     AND COLUMN_NAME IN ('rebate_percentage','share_percentage',
--                         'rebate_percent','rebate_amount',
--                         'share_amount','net_rebate')
--   ORDER BY TABLE_NAME, COLUMN_NAME;
--
-- Drift check (no row should exceed 1 minor unit of recompute drift):
--
--   SELECT id, total_f1_volume, rebate_percentage, rebate_amount,
--          ROUND(total_f1_volume * rebate_percentage / 100, 4) AS expected,
--          rebate_amount - ROUND(total_f1_volume * rebate_percentage / 100, 4) AS drift
--   FROM vinplay.rebate_logs
--   WHERE total_f1_volume > 0
--     AND ABS(rebate_amount - ROUND(total_f1_volume * rebate_percentage / 100, 4)) >= 1
--   ORDER BY id DESC LIMIT 50;

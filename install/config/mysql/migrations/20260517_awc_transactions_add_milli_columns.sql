-- =============================================================================
-- 20260517_awc_transactions_add_milli_columns.sql
--
-- Add missing milli-VND precision columns to awc_transactions.
--
-- WHY THIS EXISTS
-- ----------------
-- 2026_05_03_awc_transactions.sql uses CREATE TABLE IF NOT EXISTS, which is
-- a no-op when the table already existed from the original AWC integration
-- in late April. The two new columns (bet_milli, win_milli) added to that
-- script never landed on environments where the table was pre-created.
--
-- AwcCallbackProcessor reads both columns in handleVoidSettle / handleCancel
-- / handleResettle clawback paths, and writes them on every bet/settle row.
-- Without the columns, saveTxnCustomFull throws SQLSyntaxErrorException
-- "Unknown column 'bet_milli' in 'field list'" — every AWC bet/settle
-- audit row silently drops, and (worse) handleSettle gates credit application
-- on the audit-insert returning isNew=true, so a settle whose audit insert
-- throws never credits the player. Caught in prod 2026-05-17 03:02 KR on
-- laviai's first SEXY win post-rebuild; player was missing the win credit
-- and no rolling/bet history showed.
--
-- IDEMPOTENT
-- ----------
-- Pre-checks via information_schema; ALTERs only the missing columns. Safe
-- to re-run on environments where one or both columns already exist.
-- =============================================================================

USE vinplay_minigame;

-- bet_milli
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay_minigame'
      AND TABLE_NAME   = 'awc_transactions'
      AND COLUMN_NAME  = 'bet_milli'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE awc_transactions ADD COLUMN bet_milli BIGINT NOT NULL DEFAULT 0 AFTER tip_amount, ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT "bet_milli already present — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- win_milli
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay_minigame'
      AND TABLE_NAME   = 'awc_transactions'
      AND COLUMN_NAME  = 'win_milli'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE awc_transactions ADD COLUMN win_milli BIGINT NOT NULL DEFAULT 0 AFTER bet_milli, ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT "win_milli already present — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Post-flight: confirm both columns exist.
--   SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
--    WHERE TABLE_SCHEMA='vinplay_minigame' AND TABLE_NAME='awc_transactions'
--      AND COLUMN_NAME IN ('bet_milli','win_milli');
--   -- expected: 2 rows, both bigint NOT NULL DEFAULT 0

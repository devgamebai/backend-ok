-- =============================================================================
-- 20260518_rebate_logs_round_id.sql
--
-- SUN-1252 follow-up — add round_id to rebate_logs so the LS Rolling
-- renderer can resolve per-table game names (Sexy Baccarat C07 etc.)
-- instead of the generic "Sexy Baccarat" stub.
--
-- WHY
-- ----
-- rebate_logs.source_key carries the AWC platform_tx_id (BAC-…) for
-- bet attribution, but SEXYBCRT's table identity lives in the round_id
-- prefix (Mexico-C07-GA…). The agency renderer
-- (GetRebateLogs4AgencyProcessor) calls AwcGameNameResolver.displayName
-- WITHOUT a round_id, so it never reaches the per-table catalog row
-- — every Sexy Baccarat rebate row in LS Rolling shows the same
-- generic label regardless of which table. Per-table differentiation
-- already works in LS Cược (bet history) because GameHistoryService
-- passes round_id from log_awc_bets.
--
-- Adding the column closes that gap: writers (RealTimeCommission,
-- AutoCommissionPipeline) stamp round_id at insert, the renderer reads
-- it back, and the resolver routes to the right vinplay.games row.
--
-- IDEMPOTENT
-- ----------
-- Guarded by information_schema lookup. Adds the column + index only
-- when absent. Backfill ETL runs separately
-- (20260518_rebate_logs_round_id_etl.sql / via mongo→MySQL script).
-- =============================================================================

USE vinplay;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'rebate_logs'
      AND COLUMN_NAME  = 'round_id'
);
-- ALGORITHM=INPLACE, LOCK=NONE — rebate_logs is write-hot (every AWC settle
-- inserts a row + the LogMoneyUserExtra consumer streams in via vbee). A
-- copy-table ALTER would block DML for the duration; INPLACE/LOCK=NONE
-- keeps the table writable.
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE rebate_logs ADD COLUMN round_id VARCHAR(128) NULL AFTER game_action, ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT "rebate_logs.round_id already present — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Idx for the renderer's typical filter (source_key ranges + round_id
-- lookback). Keep low-cardinality friendly; round_id alone is high
-- cardinality.
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'rebate_logs'
      AND INDEX_NAME   = 'idx_rebate_round'
);
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE rebate_logs ADD INDEX idx_rebate_round (round_id), ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT "idx_rebate_round already present — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

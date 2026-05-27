-- SUN-1311 perf follow-up: speed up "latest redemption per code" lookup
-- used by AdminCampaignDetailProcessor. Without this index the derived-table
-- MAX(created_at) GROUP BY giftcode_id scans every redemption row.
USE vinplay;

-- idempotent add (MySQL 8+ supports IF NOT EXISTS on index)
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics
       WHERE table_schema = 'vinplay'
         AND table_name   = 'gift_code_useds'
         AND index_name   = 'idx_gcused_code_time') = 0,
    'CREATE INDEX idx_gcused_code_time ON gift_code_useds (giftcode_id, created_at)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

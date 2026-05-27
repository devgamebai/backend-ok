-- SUN-1252 / SUN-1258 / SUN-1259: extend the unified games catalog so
-- per-table identity is part of the row (Sexy Live SEXYBCRT shares one
-- AWC game_code MX-LIVE-001 across 50+ baccarat tables; the table id
-- only lives in the round_id prefix). Without per-table rows the LS
-- Cược / LS Rolling / admin Game Catalog views all collapse to one
-- generic name, defeating QC's expectation that each table reads as
-- "Sexy Baccarat M01", "Sexy Baccarat C05", etc.
--
-- Schema change is additive:
--   - new nullable VARCHAR(16) column `table_tag`, default '' so
--     existing rows stay unique (no NULL ambiguity in the composite key).
--   - composite UNIQUE drop+re-add to include table_tag.
--
-- Existing single-row-per-game-code semantics still work: the
-- AwcGameNameResolver falls back to the (platform, game_code,
-- table_tag='') row when the round_id has no parseable suffix or the
-- per-table row is not yet seeded.
--
-- Re-runnable: every step is a no-op when applied twice.

USE vinplay;

-- 1. Column.
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'games' AND COLUMN_NAME = 'table_tag');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE games ADD COLUMN table_tag VARCHAR(16) NOT NULL DEFAULT '''' AFTER game_code',
  'SELECT ''games.table_tag already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Composite UNIQUE — drop the old (provider, vendor_platform, game_code)
--    constraint if present, replace with one that includes table_tag.
SET @uk_old := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'games'
    AND INDEX_NAME = 'uk_provider_platform_code'
    AND NON_UNIQUE = 0);
SET @sql := IF(@uk_old > 0, 'ALTER TABLE games DROP INDEX uk_provider_platform_code',
                            'SELECT ''uk_provider_platform_code absent'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk_new := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'games'
    AND INDEX_NAME = 'uk_provider_platform_code_tag'
    AND NON_UNIQUE = 0);
SET @sql := IF(@uk_new = 0,
  'ALTER TABLE games ADD UNIQUE KEY uk_provider_platform_code_tag (provider, vendor_platform, game_code, table_tag)',
  'SELECT ''uk_provider_platform_code_tag already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Verify.
SELECT 'after' AS phase, COUNT(*) AS total, COUNT(DISTINCT table_tag) AS distinct_tags FROM games;

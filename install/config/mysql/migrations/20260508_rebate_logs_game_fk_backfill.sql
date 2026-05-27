-- SUN-1252 follow-up: backfill rebate_logs.game_id + category_id for
-- AWC rows so the FK link to vinplay.games is populated for history.
--
-- Why
-- ---
-- Per the SUN-GAME-FK Phase 2 dual-write, every NEW rebate_logs INSERT
-- carries game_id + category_id resolved by GameLookup. Existing AWC
-- rows predate that resolver path having SEXYBCRT per-table coverage,
-- so most legacy AWC rows have game_id = NULL or pointing at the
-- generic Sexy Baccarat row.
--
-- Strategy
-- --------
-- For every AWC rebate_logs row with source_key starting "awc:" we
--   1. extract vendor_platform from game_action (awc_<platform>_<code>)
--   2. extract game_code from game_action
--   3. parse table_tag from wager_code:
--        Mexico-XNN-GA<digits>  → XNN  (X may be empty)
--        R<NN>-<digits>         → CNN
--        else                   → ''
--   4. look up the matching games row (provider='AWC', vendor_platform,
--      game_code, table_tag) — case-insensitive collation match.
--   5. UPDATE rebate_logs.game_id + category_id.
--
-- Re-runnable: WHERE clause skips already-populated rows.

USE vinplay;

-- Step 1: temp lookup map keyed on (lower(platform), lower(game_code), tag).
DROP TEMPORARY TABLE IF EXISTS games_awc_lookup;
CREATE TEMPORARY TABLE games_awc_lookup (
    platform_lc   VARCHAR(64) NOT NULL,
    game_code_lc  VARCHAR(128) NOT NULL,
    table_tag     VARCHAR(16) NOT NULL,
    game_id       BIGINT      NOT NULL,
    category_id   INT         NOT NULL,
    KEY idx_lookup (platform_lc, game_code_lc, table_tag)
);
INSERT INTO games_awc_lookup
SELECT LOWER(vendor_platform), LOWER(game_code), table_tag, id, category_id
FROM games WHERE provider = 'AWC' AND is_active = 1;

-- Step 2: backfill rows that have a parseable wager_code
-- (Mexico-XNN-GA<digits>) and a matching catalog row.
UPDATE rebate_logs r
JOIN games_awc_lookup g
  ON g.platform_lc  = LOWER(SUBSTRING_INDEX(SUBSTRING_INDEX(r.game_action, '_', 2), '_', -1))
 AND g.game_code_lc = LOWER(SUBSTRING(r.game_action, LENGTH(SUBSTRING_INDEX(r.game_action, '_', 2)) + 2))
 AND g.table_tag    = COALESCE(REGEXP_REPLACE(r.wager_code, '^Mexico-([A-Z]?[0-9]+)-GA[0-9]+$', '\\1'),
                              REGEXP_REPLACE(r.wager_code, '^R([0-9]+)-[0-9]+$', 'C\\1'))
SET r.game_id = g.game_id, r.category_id = g.category_id
WHERE r.source_key LIKE 'awc:%' AND r.game_id IS NULL
  AND r.wager_code REGEXP '^(Mexico-[A-Z]?[0-9]+-GA[0-9]+|R[0-9]+-[0-9]+)$';

-- Step 3: backfill rows whose wager_code does not match the table-id
-- pattern (lobby auto-rotation, R-<unix> synthetic ids, etc.) — point
-- them at the no-tag catalog row so the FK is at least populated.
UPDATE rebate_logs r
JOIN games_awc_lookup g
  ON g.platform_lc  = LOWER(SUBSTRING_INDEX(SUBSTRING_INDEX(r.game_action, '_', 2), '_', -1))
 AND g.game_code_lc = LOWER(SUBSTRING(r.game_action, LENGTH(SUBSTRING_INDEX(r.game_action, '_', 2)) + 2))
 AND g.table_tag    = ''
SET r.game_id = g.game_id, r.category_id = g.category_id
WHERE r.source_key LIKE 'awc:%' AND r.game_id IS NULL;

-- Step 4: verification
SELECT 'awc_total'      AS metric, COUNT(*) AS value FROM rebate_logs WHERE source_key LIKE 'awc:%'
UNION ALL
SELECT 'awc_with_game_id', COUNT(*) FROM rebate_logs WHERE source_key LIKE 'awc:%' AND game_id IS NOT NULL
UNION ALL
SELECT 'awc_missing_game_id', COUNT(*) FROM rebate_logs WHERE source_key LIKE 'awc:%' AND game_id IS NULL;

DROP TEMPORARY TABLE games_awc_lookup;

-- Rollback: restore pre-curate state for AI Live Casino (1149) from the
-- 2026-04-23 archive.

USE vinplay;

UPDATE vinplay.gsc_game_catalog c
JOIN vinplay._archive_ailive_curate_20260423 a
  ON a.product_code = c.product_code AND a.game_code = c.game_code
SET c.is_available = a.is_available
WHERE c.product_code = 1149;

SELECT ROW_COUNT() AS rows_restored;

-- Rollback: rehydrate gsc_game_catalog.is_available for product_code=1091
-- from the 2026-04-23 pre-curate archive.

USE vinplay;

UPDATE vinplay.gsc_game_catalog c
JOIN vinplay._archive_jili_curate_20260423 a
  ON a.product_code = c.product_code AND a.game_code = c.game_code
SET c.is_available = a.is_available
WHERE c.product_code = 1091;

SELECT ROW_COUNT() AS rows_restored;

-- Rollback: restore game_code=667 (Circus Jackpot) back to is_available=0
-- using the 2026-04-23 archive.

USE vinplay;

UPDATE vinplay.gsc_game_catalog c
JOIN vinplay._archive_jili_667_enable_20260423 a
  ON a.product_code = c.product_code AND a.game_code = c.game_code
SET c.is_available = a.is_available
WHERE c.product_code = 1091 AND c.game_code = '667';

SELECT ROW_COUNT() AS rows_restored;

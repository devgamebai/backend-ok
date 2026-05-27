-- SUN-965 rollback — restore the 6 Jili slot games to the player lobby.

UPDATE vinplay.gsc_game_catalog
SET is_available = 1
WHERE product_code = 1091
  AND game_code IN ('49', '103', '35', '110', '77', '461');

-- Verification — expect exactly 6 rows, all is_available = 1.
SELECT product_code, game_code, game_name, is_available
FROM vinplay.gsc_game_catalog
WHERE product_code = 1091
  AND game_code IN ('49', '103', '35', '110', '77', '461')
ORDER BY game_code;

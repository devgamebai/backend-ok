-- GitLab #36: Pragmatic Play (product_code=1006) — curate to 3-game whitelist
-- Per product (2026-04-23): only Pig Farm, Lucky Monkey, Lucky Tiger visible.
-- Archive pre-change state, flip is_available, verify.

USE vinplay;

-- Archive before change so rollback is trivial
CREATE TABLE IF NOT EXISTS _archive_pp_curate_20260423 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog WHERE product_code = 1006;

-- Mark the 3 whitelist games as available
UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1006
  AND game_code IN ('vs25pfarmfp','vs5luckymly','vs5luckytigly');

-- Disable everything else in 1006
UPDATE gsc_game_catalog SET is_available = 0
WHERE product_code = 1006
  AND game_code NOT IN ('vs25pfarmfp','vs5luckymly','vs5luckytigly');

-- Verify (expect: 3 available, 712 disabled)
SELECT SUM(is_available=1) AS available, SUM(is_available=0) AS disabled
FROM gsc_game_catalog WHERE product_code = 1006;

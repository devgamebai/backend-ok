-- GitLab #36: JILI (product_code=1091) — curate to single-game whitelist
-- Per product (2026-04-23): only game_code='49' (Super Ace) visible.
-- Note: SUN-965 previously hid 6 specific JILI games via is_available=0.
-- This curate subsumes that — anything outside the whitelist becomes hidden.

USE vinplay;

-- Archive before change so rollback is trivial
CREATE TABLE IF NOT EXISTS _archive_jili_curate_20260423 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog WHERE product_code = 1091;

-- Mark the single whitelist game as available
UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1091 AND game_code = '49';

-- Disable everything else in 1091
UPDATE gsc_game_catalog SET is_available = 0
WHERE product_code = 1091 AND game_code <> '49';

-- Verify (expect: 1 available, 160 disabled)
SELECT SUM(is_available=1) AS available, SUM(is_available=0) AS disabled
FROM gsc_game_catalog WHERE product_code = 1091;

-- GitLab #36: JILI (1091) — add game_code=667 (Circus Jackpot) to whitelist
-- Per product (2026-04-23): JILI visible list is now 2 games —
-- Super Ace (49) + Circus Jackpot (667).

USE vinplay;

-- Archive pre-flip state (just the one row — non-invasive add)
CREATE TABLE IF NOT EXISTS _archive_jili_667_enable_20260423 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog WHERE product_code=1091 AND game_code='667';

UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1091 AND game_code = '667';

-- Verify: expect 2 available, 160 disabled
SELECT SUM(is_available=1) AS available, SUM(is_available=0) AS disabled
FROM gsc_game_catalog WHERE product_code=1091;

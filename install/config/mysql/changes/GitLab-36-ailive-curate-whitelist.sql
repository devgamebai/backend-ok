-- GitLab #36: AI Live Casino / Hash Gaming (product_code=1149) — curate to
-- a 4-game whitelist. Per product (2026-04-23), rows #2/6/7/8 from the
-- stakeholder doc table: Baccarat, Lucky Numbers, Roulette, Sic Bo.
--
-- Also requires backend env update so c=3091 actually calls GSC for 1149:
--   GSC_WHITELIST_PRODUCTS must include 1149
--   GSC_CURRENCY_PER_PRODUCT should include 1149:VND
-- Both updated in prod .env on 2026-04-23 alongside this migration;
-- portal-api was recreated to pick them up.

USE vinplay;

-- Archive current (pre-curate) state — first-time curate for 1149
CREATE TABLE IF NOT EXISTS _archive_ailive_curate_20260423 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog WHERE product_code=1149;

-- Mark the 4 whitelist games as available
UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1149
  AND game_code IN ('HASH_BACCARAT','HASH_LUCKY_NUMBERS','HASH_ROULETTE','HASH_SICBO');

-- Disable everything else in 1149 (Andar Bahar, Bingo Frenzy, Color Game,
-- Craps, Speed Frenzy — the other 5 upstream games)
UPDATE gsc_game_catalog SET is_available = 0
WHERE product_code = 1149
  AND game_code NOT IN ('HASH_BACCARAT','HASH_LUCKY_NUMBERS','HASH_ROULETTE','HASH_SICBO');

-- Verify: expect 4 available, 5 disabled
SELECT SUM(is_available=1) AS available, SUM(is_available=0) AS disabled
FROM gsc_game_catalog WHERE product_code=1149;

-- SUN-857 follow-up:
-- Allow one GSC provider (single product_code) to map to different internal
-- game_keys depending on the game_type sent per bet — e.g. Evolution Sicbo
-- should inherit our `sicbo` commission rate, Evolution Baccarat our `baccarat`,
-- anything else Evolution falls back to the provider-level `ag`.
--
-- Backward compatible: default game_type = '*' keeps existing (product_code)
-- rows working as the wildcard/fallback. Service looks up exact (pc, gt) first,
-- then falls back to (pc, '*').

USE vinplay;

-- 1. Add the game_type column with a safe default so existing rows work as fallback.
ALTER TABLE gsc_product_map
    ADD COLUMN game_type VARCHAR(64) NOT NULL DEFAULT '*' AFTER product_code;

-- 2. Rekey: primary key becomes (product_code, game_type).
ALTER TABLE gsc_product_map DROP PRIMARY KEY;
ALTER TABLE gsc_product_map ADD PRIMARY KEY (product_code, game_type);

-- 3. Example per-game overrides. Uncomment + edit when ops knows which GSC
--    product_code + game_type combos should map to our internal game_keys.
--    Without these rows, behaviour is unchanged — all bets resolve to the
--    provider-level row (game_type='*').
--
-- INSERT INTO gsc_product_map (product_code, game_type, provider_name, game_key, game_label_vi, category, is_active)
-- VALUES
--     (1002, 'Sicbo',    'Evolution', 'sicbo',    'Evolution Sicbo',    'CASINO', 1),
--     (1002, 'Baccarat', 'Evolution', 'baccarat', 'Evolution Baccarat', 'CASINO', 1),
--     (1002, 'Roulette', 'Evolution', 'roulette', 'Evolution Roulette', 'CASINO', 1)
-- ON DUPLICATE KEY UPDATE
--     provider_name = VALUES(provider_name),
--     game_key      = VALUES(game_key),
--     game_label_vi = VALUES(game_label_vi),
--     category      = VALUES(category),
--     is_active     = VALUES(is_active);

-- Verification
SELECT product_code, game_type, provider_name, game_key, category, is_active
  FROM gsc_product_map
  ORDER BY product_code, game_type;

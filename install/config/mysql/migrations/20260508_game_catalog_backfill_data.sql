-- =============================================================================
-- SUN-GAME-FK Phase 1 backfill — populate the new unified `games` catalog
-- and FK columns on game_commission_rate / rebate_logs from existing data.
-- Run AFTER 20260508_game_catalog_schema_unification.sql.
-- Idempotent (INSERT IGNORE / WHERE IS NULL).
-- =============================================================================

-- 1. GSC games from gsc_game_catalog
INSERT IGNORE INTO vinplay.games (provider, vendor_platform, game_code, game_name, category_id)
SELECT 'GSC', CAST(g.product_code AS CHAR), g.game_code, g.game_name,
       COALESCE(c.id, (SELECT id FROM vinplay.game_categories WHERE name='Other'))
FROM vinplay.gsc_game_catalog g
LEFT JOIN vinplay.game_categories c
    ON c.name COLLATE utf8mb4_unicode_ci = g.category COLLATE utf8mb4_unicode_ci;

-- 2. AWC per-game from awc_game_catalog (normalize coarse → granular)
INSERT IGNORE INTO vinplay.games (provider, vendor_platform, game_code, game_name, category_id)
SELECT 'AWC', a.platform, a.game_code, a.game_name,
       (SELECT id FROM vinplay.game_categories WHERE name COLLATE utf8mb4_unicode_ci = (CASE
           WHEN UPPER(a.category)='CASINO' THEN 'Baccarat'
           WHEN UPPER(a.category)='SLOT'   THEN 'Slot'
           WHEN UPPER(a.category)='FISH'   THEN 'Fishing'
           WHEN UPPER(a.category)='SPORT'  THEN 'Sports'
           WHEN UPPER(a.category)='EGAME'  THEN 'GameShow'
           WHEN UPPER(a.category)='OTHER'  THEN 'Other'
           ELSE a.category
       END) COLLATE utf8mb4_unicode_ci)
FROM vinplay_minigame.awc_game_catalog a;

-- 3. AWC platform stubs (game_code='*' = "any game on this platform — fallback").
INSERT IGNORE INTO vinplay.games (provider, vendor_platform, game_code, game_name, category_id)
SELECT 'AWC', m.platform, '*', CONCAT(m.platform, ' (default)'),
       (SELECT id FROM vinplay.game_categories WHERE name = (CASE
           WHEN UPPER(m.category)='CASINO' THEN 'Baccarat'
           WHEN UPPER(m.category)='SLOT'   THEN 'Slot'
           WHEN UPPER(m.category)='FISH'   THEN 'Fishing'
           WHEN UPPER(m.category)='SPORT'  THEN 'Sports'
           WHEN UPPER(m.category)='EGAME'  THEN 'GameShow'
           WHEN UPPER(m.category)='OTHER'  THEN 'Other'
           ELSE 'Other'
       END))
FROM vinplay_minigame.awc_platform_map m;

-- 4. game_commission_rate.category_id from string game_key (live_cat_<X>).
UPDATE vinplay.game_commission_rate gcr
JOIN vinplay.game_categories c
  ON CONCAT('live_cat_', c.name) COLLATE utf8mb4_unicode_ci = gcr.game_key
SET gcr.category_id = c.id
WHERE gcr.category_id IS NULL;

-- 5. rebate_logs.game_id + category_id — exact game match.
UPDATE vinplay.rebate_logs rl
JOIN vinplay.games g
  ON g.provider='AWC'
  AND CONCAT('awc_', LOWER(g.vendor_platform), '_', LOWER(g.game_code)) COLLATE utf8mb4_unicode_ci = rl.game_action
SET rl.game_id = g.id, rl.category_id = g.category_id
WHERE rl.game_id IS NULL AND rl.game_action LIKE 'awc\\_%';

-- 6. rebate_logs fallback to AWC platform stub for un-catalogued games.
UPDATE vinplay.rebate_logs rl
JOIN vinplay.games g
  ON g.provider='AWC' AND g.game_code='*'
  AND CONCAT('awc_', LOWER(g.vendor_platform), '_') COLLATE utf8mb4_unicode_ci
      = LEFT(rl.game_action, LENGTH(g.vendor_platform)+5)
SET rl.game_id = g.id, rl.category_id = g.category_id
WHERE rl.category_id IS NULL AND rl.game_action LIKE 'awc\\_%';

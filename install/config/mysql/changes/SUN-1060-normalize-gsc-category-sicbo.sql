-- GitLab #37 / SUN-1060 follow-up — normalize 2 Evolution rows whose
-- gsc_game_catalog.category column holds the Vietnamese display label
-- (mojibake-decoded from the upstream catalog CSV) instead of the
-- canonical English enum value.
--
-- Symptom: admin CMS shows TWO "Sic Bo" rows in the agent commission
-- form — one for `live_cat_SicBoDice` (correct) and one for
-- `live_cat_Sic Bo & Dice (Tài xỉu, xóc đĩa)` (garbage). Bets on the
-- 2 affected Evolution games resolve to the garbage bucket at commit
-- time and miss whatever rate the admin configured on SicBoDice.
--
-- Affected rows (product_code=1002):
--   SpeedSicBo000001 — Siêu tài xỉu tốc độ
--   SuperSicBo0000001 — Siêu tài xỉu
--
-- Fix: flip category → 'SicBoDice'. Idempotent — no-op on environments
-- where the rows are already correct.
--
-- Rollback: install/config/mysql/changes/SUN-1060-normalize-gsc-category-sicbo-rollback.sql

USE vinplay;

-- 1. Archive pre-change state for rollback.
CREATE TABLE IF NOT EXISTS _archive_sun1060_catalog_category_20260424 AS
SELECT product_code, game_code, game_name, category, NOW() AS archived_at
FROM gsc_game_catalog
WHERE category = 'Sic Bo & Dice (Tài xỉu, xóc đĩa)';

SELECT COUNT(*) AS rows_to_fix
FROM gsc_game_catalog
WHERE category = 'Sic Bo & Dice (Tài xỉu, xóc đĩa)';
-- Expected on prod: 2. On staging: likely 0 (already fixed via earlier ad-hoc DB work).

-- 2. Flip the category.
UPDATE gsc_game_catalog
SET category = 'SicBoDice'
WHERE category = 'Sic Bo & Dice (Tài xỉu, xóc đĩa)';

-- 3. Verify.
SELECT category, COUNT(*) AS cnt
FROM gsc_game_catalog
WHERE product_code = 1002
  AND (game_name LIKE '%Sic Bo%' OR game_name LIKE '%tài xỉu%')
GROUP BY category;
-- Expected after fix: only SicBoDice in the result set.

-- Global sanity — no mojibake category should remain:
SELECT category, COUNT(*) AS cnt
FROM gsc_game_catalog
GROUP BY category
HAVING category REGEXP '[^A-Za-z0-9 _-]'
ORDER BY cnt DESC;
-- Expected: empty set (no Vietnamese / parenthesis / punctuation in category codes).

-- 4. After applying, restart portal-api to bust the 5-minute lobby cache:
--      docker restart sunwinkr-portal-api

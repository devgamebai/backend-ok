-- SUN-965 (GitLab issue #18) — hide 6 Jili slot games per CR from Wukong QC (2026-04-19).
--
-- Prerequisite: the is_available column must exist (migration
-- 2026_04_21_sun_1002_gsc_is_available_sort_order.sql). Production MUST run
-- that migration before this UPDATE.
--
-- Idempotent: re-running sets the flag back to 0 on already-hidden rows (no-op).
-- Rollback: SUN-965-rollback.sql in this folder.

UPDATE vinplay.gsc_game_catalog
SET is_available = 0
WHERE product_code = 1091
  AND game_code IN ('49', '103', '35', '110', '77', '461');

-- Verification — expect exactly 6 rows, all is_available = 0.
SELECT product_code, game_code, game_name, is_available
FROM vinplay.gsc_game_catalog
WHERE product_code = 1091
  AND game_code IN ('49', '103', '35', '110', '77', '461')
ORDER BY game_code;

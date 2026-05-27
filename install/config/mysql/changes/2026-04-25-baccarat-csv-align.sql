-- 2026-04-25 — Align gsc_game_catalog Baccarat category to the CSV
-- (docs/ref/Game Evolution - Game Evolution.csv)
--
-- Required state per CSV: 11 games active in Baccarat category for product 1002.
-- Current state: 10 of those 11 already correct, BacBo is in GameShow category,
-- and 7 English-named duplicates are also active in Baccarat → 17 tiles show
-- when QC expects 11.
--
-- Two corrections, idempotent (re-running finds 0 rows):
--   A. Move BacBo00000000001 from GameShow → Baccarat
--   B. Deactivate 7 English-variant duplicates whose Vietnamese counterparts
--      are explicitly listed in the CSV
--
-- Rollback path: archive table _archive_baccarat_csv_align_20260425 keeps
-- pre-change snapshot of every affected row.

USE vinplay;

-- ─── Archive pre-change state ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS _archive_baccarat_csv_align_20260425 AS
SELECT product_code, game_code, game_name, game_name_vi, category, active, sort_order, NOW() AS archived_at
  FROM gsc_game_catalog
 WHERE product_code = 1002
   AND (
     game_code = 'BacBo00000000001'
     OR game_code IN (
        'LightningBac0001',
        'XXXtremeLB000001',
        'pv2y4kmsanvdvwgy',
        'srsnxlybuz4rrqr6',
        'srsp4ai6uz4sfq4z',
        'lv2kzclunt2qnxo5',
        'ndgvwvgthfuaad3q'
     )
   );

SELECT COUNT(*) AS rows_archived FROM _archive_baccarat_csv_align_20260425;
-- Expected on prod (first run): 8 rows.

-- ─── A. Move Bac Bo → Baccarat category ──────────────────────────────
UPDATE gsc_game_catalog
   SET category = 'Baccarat'
 WHERE product_code = 1002
   AND game_code   = 'BacBo00000000001'
   AND category   <> 'Baccarat';

-- ─── B. Deactivate 7 English duplicates ──────────────────────────────
UPDATE gsc_game_catalog
   SET active = 0
 WHERE product_code = 1002
   AND active = 1
   AND game_code IN (
       'LightningBac0001',  -- duplicate of LightningBacc001 (Baccarat Sét)
       'XXXtremeLB000001',  -- duplicate of XXXtremeLightBac (XXXtreme Baccarat sét)
       'pv2y4kmsanvdvwgy',  -- duplicate of LotusSpeedBac001
       'srsnxlybuz4rrqr6',  -- duplicate of LotusSpeedBac002
       'srsp4ai6uz4sfq4z',  -- duplicate of LotusSpeedBac003
       'lv2kzclunt2qnxo5',  -- duplicate of 90w0v3h2o52rjknf (Baccarat tốc độ B)
       'ndgvwvgthfuaad3q'   -- duplicate of 90w1u5e3o52rlmn6 (Baccarat tốc độ C)
   );

-- ─── Verify ──────────────────────────────────────────────────────────
SELECT 'active baccarat after change' AS label, COUNT(*) AS cnt
  FROM gsc_game_catalog
 WHERE product_code = 1002 AND category = 'Baccarat' AND active = 1;
-- Expected: 11.

SELECT game_code, game_name_vi, active
  FROM gsc_game_catalog
 WHERE product_code = 1002 AND category = 'Baccarat' AND active = 1
 ORDER BY game_name_vi;

-- ─── After applying, restart portal-api to bust the 5-min lobby cache:
--      docker restart sunwinkr-portal-api

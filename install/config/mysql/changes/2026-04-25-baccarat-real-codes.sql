-- 2026-04-25 round-2 — replace phantom CSV game_codes with real Evolution
-- game_codes confirmed by user (cross-checked against
-- docs/GSC_GAME_CATALOG_stakeholder.md).
--
-- Net change: still exactly 11 active baccarat tiles in product 1002,
-- but the 7 phantom rows (which Evolution upstream doesn't recognise →
-- silently dropped by c=3091's intersection step) get swapped for their
-- real-code counterparts. Vietnamese display names from the CSV are
-- preserved by overwriting the real rows' `game_name_vi`.
--
-- Idempotent: re-running finds nothing to change.

USE vinplay;

-- ─── Archive every row touched by this script ────────────────────────
CREATE TABLE IF NOT EXISTS _archive_baccarat_real_codes_20260425 AS
SELECT product_code, game_code, game_name, game_name_vi, category, active, sort_order, NOW() AS archived_at
  FROM gsc_game_catalog
 WHERE product_code = 1002
   AND game_code IN (
       -- 7 phantoms to deactivate
       'LotusSpeedBac001', 'LotusSpeedBac002', 'LotusSpeedBac003',
       '90w0v3h2o52rjknf', '90w1u5e3o52rlmn6',
       'XXXtremeLightBac', 'LightningBacc001',
       -- 7 real codes to activate + rename
       'pv2y4kmsanvdvwgy', 'srsnxlybuz4rrqr6', 'srsp4ai6uz4sfq4z',
       'lv2kzclunt2qnxo5', 'ndgvwvgthfuaad3q',
       'XXXtremeLB000001', 'LightningBac0001'
   );

SELECT COUNT(*) AS rows_archived FROM _archive_baccarat_real_codes_20260425;
-- Expected on prod (first run): 14 rows.

-- ─── A. Deactivate 7 phantom CSV game_codes ──────────────────────────
-- Evolution upstream doesn't recognise these — c=3091's intersection
-- step silently drops them, which is why QC saw "only 2 baccarat tiles"
-- after the first alignment. Deactivating to clean up the catalog and
-- match the user-confirmed canonical 11.
UPDATE gsc_game_catalog
   SET active = 0
 WHERE product_code = 1002
   AND active = 1
   AND game_code IN (
       'LotusSpeedBac001',  -- phantom for "Baccarat Tốc Độ Lotus Tiếng Việt A" — real is pv2y4kmsanvdvwgy
       'LotusSpeedBac002',  -- phantom (B) — real is srsnxlybuz4rrqr6
       'LotusSpeedBac003',  -- phantom (C) — real is srsp4ai6uz4sfq4z
       '90w0v3h2o52rjknf',  -- phantom for "Baccarat tốc độ B" — real is lv2kzclunt2qnxo5
       '90w1u5e3o52rlmn6',  -- phantom for "Baccarat tốc độ C" — real is ndgvwvgthfuaad3q
       'XXXtremeLightBac',  -- phantom for "XXXtreme Baccarat sét" — real is XXXtremeLB000001
       'LightningBacc001'   -- phantom for "Baccarat Sét" — real is LightningBac0001
   );

-- ─── B. Activate 7 real Evolution game_codes ─────────────────────────
UPDATE gsc_game_catalog
   SET active = 1
 WHERE product_code = 1002
   AND active = 0
   AND game_code IN (
       'pv2y4kmsanvdvwgy',
       'srsnxlybuz4rrqr6',
       'srsp4ai6uz4sfq4z',
       'lv2kzclunt2qnxo5',
       'ndgvwvgthfuaad3q',
       'XXXtremeLB000001',
       'LightningBac0001'
   );

-- ─── C. Stamp Vietnamese display names from CSV onto real rows ───────
-- FE shows game_name_vi when present, falls back to game_name. Setting
-- game_name_vi here keeps "Baccarat Sét" / "XXXtreme Baccarat sét" /
-- "Baccarat Tốc Độ Lotus Tiếng Việt A/B/C" / "Baccarat tốc độ B/C" on
-- the lobby tiles even though the underlying game_code is Evolution's
-- canonical English-named one.
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat Tốc Độ Lotus Tiếng Việt A' WHERE product_code=1002 AND game_code='pv2y4kmsanvdvwgy';
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat Tốc Độ Lotus Tiếng Việt B' WHERE product_code=1002 AND game_code='srsnxlybuz4rrqr6';
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat Tốc Độ Lotus Tiếng Việt C' WHERE product_code=1002 AND game_code='srsp4ai6uz4sfq4z';
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat tốc độ B'                  WHERE product_code=1002 AND game_code='lv2kzclunt2qnxo5';
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat tốc độ C'                  WHERE product_code=1002 AND game_code='ndgvwvgthfuaad3q';
UPDATE gsc_game_catalog SET game_name_vi = 'XXXtreme Baccarat sét'              WHERE product_code=1002 AND game_code='XXXtremeLB000001';
UPDATE gsc_game_catalog SET game_name_vi = 'Baccarat Sét'                       WHERE product_code=1002 AND game_code='LightningBac0001';

-- ─── D. Ensure BacBo stays in Baccarat category ──────────────────────
UPDATE gsc_game_catalog
   SET category = 'Baccarat'
 WHERE product_code = 1002
   AND game_code   = 'BacBo00000000001'
   AND category   <> 'Baccarat';

-- ─── Verify ──────────────────────────────────────────────────────────
SELECT 'baccarat_active_count' AS label, COUNT(*) AS cnt
  FROM gsc_game_catalog
 WHERE product_code=1002 AND category='Baccarat' AND active=1;
-- Expected: 11.

SELECT game_code, game_name_vi, active
  FROM gsc_game_catalog
 WHERE product_code=1002 AND category='Baccarat' AND active=1
 ORDER BY game_name_vi;

-- ─── After applying, restart portal-api to bust the 5-min lobby cache:
--      docker restart sunwinkr-portal-api

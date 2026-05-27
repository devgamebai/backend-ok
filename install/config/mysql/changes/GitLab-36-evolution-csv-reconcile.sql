-- GitLab #36: Evolution CSV reconciliation (2026-04-23)
-- Evolution rotated 11 of the 23 CSV game_codes; the CSV was stale.
-- Also 1 game ("Baccarat C" / 8v9f4xhuhd005xlb) was fully delisted by Evolution.
-- Plus: MarbleRace000001 was leaking through the curate because it was in
-- upstream GSC's response but missing from our gsc_game_catalog (overlay
-- defaulted to active=true). Insert-with-is_available=0 plugs the leak.
--
-- Net effect on player lobby: 12 → 22 visible Evolution games, matching the
-- CSV intent of 23 minus the 1 delisted game.

USE vinplay;

-- Archive current state before the flip
CREATE TABLE IF NOT EXISTS _archive_evo_csv_reconcile_20260423 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog WHERE product_code=1002 AND game_code IN (
  'pv2y4kmsanvdvwgy','srsnxlybuz4rrqr6','srsp4ai6uz4sfq4z',
  'lv2kzclunt2qnxo5','ndgvwvgthfuaad3q',
  'LightningBac0001','XXXtremeLB000001',
  'SuperSicBo000001','InstantSSB000001',
  'pv2zgy42anvdwk3l','SpeedAutoRo00001'
);

-- Flip the 11 renamed codes to visible
UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1002 AND game_code IN (
  'pv2y4kmsanvdvwgy',    -- Lotus Speed Baccarat A (was LotusSpeedBac001)
  'srsnxlybuz4rrqr6',    -- Lotus Speed Baccarat B (was LotusSpeedBac002)
  'srsp4ai6uz4sfq4z',    -- Lotus Speed Baccarat C (was LotusSpeedBac003)
  'lv2kzclunt2qnxo5',    -- Speed Baccarat B        (was 90w0v3h2o52rjknf)
  'ndgvwvgthfuaad3q',    -- Speed Baccarat C        (was 90w1u5e3o52rlmn6)
  'LightningBac0001',    -- Lightning Baccarat      (was LightningBacc001)
  'XXXtremeLB000001',    -- XXXtreme Lightning Baccarat (was XXXtremeLightBac)
  'SuperSicBo000001',    -- Super Sic Bo            (was SuperSicBo0000001 — one extra zero)
  'InstantSSB000001',    -- Speed Super Sic Bo      (was SpeedSicBo000001)
  'pv2zgy42anvdwk3l',    -- Lotus Roulette          (was LotusRoulette001)
  'SpeedAutoRo00001'     -- Speed Auto Roulette     (was SpeedAutoRou0001)
);
-- Expect: 11 rows updated.

-- Plug MarbleRace leak (uncataloged upstream game was fail-open defaulting to active=true)
INSERT INTO gsc_game_catalog
  (product_code, game_code, game_name, game_type, category, image_url, is_available, sort_order)
VALUES
  (1002, 'MarbleRace000001', 'Marble Race', 'LIVE_CASINO', 'GameShow', '', 0, 0)
ON DUPLICATE KEY UPDATE is_available = 0;

-- Note: the 12 stale codes in the CSV (LotusSpeedBac001..003, 90w0v3h2o52rjknf,
-- 90w1u5e3o52rlmn6, 8v9f4xhuhd005xlb, LightningBacc001, LotusRoulette001,
-- LotusSpeedBac001, SpeedAutoRou0001, SpeedSicBo000001, SuperSicBo0000001,
-- XXXtremeLightBac) remain at is_available=1 from the original seed. Since they
-- are not in upstream GSC's response anymore, they never surface in the lobby.
-- Leaving them flipped on doesn't affect behavior — not worth the extra churn.

-- Verify (expect 34 rows at is_available=1 in DB; 22 visible in player lobby)
SELECT SUM(is_available=1) AS available_in_db, SUM(is_available=0) AS disabled_in_db
FROM gsc_game_catalog WHERE product_code=1002;

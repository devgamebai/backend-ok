-- SUN-1108 Phase B — gsc_game_catalog Vietnamese encoding repair
-- Source of truth: docs/ref/Game Evolution.xlsx (Evolution / product_code=1002)
-- Strategy:
--   1) ALTER table to utf8mb4 (stops future inserts from being mojibake)
--   2) UPDATE the 23 active Evolution codes from Excel as authoritative source
--   3) Repair any remaining mojibake phantom rows with the binary-cast trick

START TRANSACTION;

-- Step 1: convert table charset
ALTER TABLE vinplay.gsc_game_catalog
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Step 2: authoritative names from Excel for the 23 active Evolution codes
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat Tốc Độ Lotus Tiếng Việt A', game_name='Lotus Speed Baccarat A'
  WHERE product_code=1002 AND game_code='pv2y4kmsanvdvwgy';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat Tốc Độ Lotus Tiếng Việt B', game_name='Lotus Speed Baccarat B'
  WHERE product_code=1002 AND game_code='srsnxlybuz4rrqr6';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat Tốc Độ Lotus Tiếng Việt C', game_name='Lotus Speed Baccarat C'
  WHERE product_code=1002 AND game_code='srsp4ai6uz4sfq4z';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat tốc độ B', game_name='Speed Baccarat B'
  WHERE product_code=1002 AND game_code='lv2kzclunt2qnxo5';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat tốc độ C', game_name='Speed Baccarat C'
  WHERE product_code=1002 AND game_code='ndgvwvgthfuaad3q';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat A', game_name='Baccarat A'
  WHERE product_code=1002 AND game_code='oytmvb9m1zysmc44';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat B', game_name='Baccarat B'
  WHERE product_code=1002 AND game_code='60i0lcfx5wkkv3sy';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat C', game_name='Baccarat C'
  WHERE product_code=1002 AND game_code='8v9f4xhuhd005xlb';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='XXXtreme Baccarat sét', game_name='XXXtreme Lightning Baccarat'
  WHERE product_code=1002 AND game_code='XXXtremeLB000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Baccarat Sét', game_name='Lightning Baccarat'
  WHERE product_code=1002 AND game_code='LightningBac0001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Bac Bo', game_name='Bac Bo'
  WHERE product_code=1002 AND game_code='BacBo00000000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Tài xỉu Lotus', game_name='Lotus Sic Bo'
  WHERE product_code=1002 AND game_code='LotusSicBo000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Siêu tài xỉu', game_name='Super Sic Bo'
  WHERE product_code=1002 AND game_code='SuperSicBo000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Siêu tài xỉu tốc độ', game_name='Speed Super Sic Bo'
  WHERE product_code=1002 AND game_code='InstantSSB000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Tài xỉu trung quốc', game_name='Emperor Sic Bo'
  WHERE product_code=1002 AND game_code='EmperorSB0000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Rồng hổ', game_name='Dragon Tiger'
  WHERE product_code=1002 AND game_code='DragonTiger00001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Roulette Việt Nam', game_name='Lotus Roulette'
  WHERE product_code=1002 AND game_code='pv2zgy42anvdwk3l';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='z', game_name='Auto-Roulette'
  WHERE product_code=1002 AND game_code='48z5pjps3ntvqc1b';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Roulette tốc độ tự động', game_name='Speed Auto Roulette'
  WHERE product_code=1002 AND game_code='SpeedAutoRo00001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Blackjack vô cực', game_name='Infinite Blackjack'
  WHERE product_code=1002 AND game_code='mrfykemt5slanyi5';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Casino Hold’em', game_name='Casino Hold''em'
  WHERE product_code=1002 AND game_code='HoldemTable00001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Câu cá sông băng', game_name='Ice Fishing'
  WHERE product_code=1002 AND game_code='IceFishing000001';
UPDATE vinplay.gsc_game_catalog SET game_name_vi='Thời gian điên rồ', game_name='Crazy Time'
  WHERE product_code=1002 AND game_code='CrazyTime0000001';

-- Step 3: repair any remaining phantom mojibake rows (rows whose game_code is
-- not in the Excel source but still has double-encoded UTF-8). Uses the standard
-- binary-cast trick: CONVERT(BINARY ... USING utf8mb4) reverses Latin-1-as-UTF-8.
-- Pattern Ã or á» reliably identifies double-encoded UTF-8 — clean ASCII or
-- proper utf8mb4 will not match.
UPDATE vinplay.gsc_game_catalog
  SET game_name_vi = CONVERT(BINARY CONVERT(game_name_vi USING latin1) USING utf8mb4),
      game_name    = CONVERT(BINARY CONVERT(game_name    USING latin1) USING utf8mb4)
WHERE (game_name_vi IS NOT NULL AND game_name_vi REGEXP 'Ã|á»')
   OR (game_name    IS NOT NULL AND game_name    REGEXP 'Ã|á»');

-- Sanity check: verify zero mojibake remains
SELECT COUNT(*) AS remaining_mojibake
FROM vinplay.gsc_game_catalog
WHERE (game_name_vi REGEXP 'Ã|á»') OR (game_name REGEXP 'Ã|á»');

COMMIT;

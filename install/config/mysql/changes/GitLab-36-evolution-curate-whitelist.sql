-- GitLab #36: Evolution (product_code=1002) — curate to 23-game whitelist.
-- Source of truth: docs/ref/Game Evolution - Game Evolution.csv
-- Pairs with backend default-filter in GSCGameListProcessor (c=3091 hides
-- is_available=0 unless ?show_disabled=1 is set).
--
-- Rollback:
--   SEE install/config/mysql/changes/GitLab-36-evolution-curate-whitelist-rollback.sql

USE vinplay;

-- ---------------------------------------------------------------------------
-- 1. Archive pre-change state so rollback is trivial.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _archive_evo_curate_20260422 AS
SELECT product_code, game_code, is_available, NOW() AS archived_at
FROM gsc_game_catalog
WHERE product_code = 1002;

-- ---------------------------------------------------------------------------
-- 2. Mark the 23 CSV games as available.
-- ---------------------------------------------------------------------------
UPDATE gsc_game_catalog SET is_available = 1
WHERE product_code = 1002
  AND game_code IN (
    'LotusSpeedBac001','LotusSpeedBac002','LotusSpeedBac003',
    '90w0v3h2o52rjknf','90w1u5e3o52rlmn6','oytmvb9m1zysmc44',
    '60i0lcfx5wkkv3sy','8v9f4xhuhd005xlb','XXXtremeLightBac',
    'LightningBacc001','BacBo00000000001','LotusSicBo000001',
    'SuperSicBo0000001','SpeedSicBo000001','EmperorSB0000001',
    'DragonTiger00001','LotusRoulette001','48z5pjps3ntvqc1b',
    'SpeedAutoRou0001','mrfykemt5slanyi5','HoldemTable00001',
    'IceFishing000001','CrazyTime0000001'
  );

-- ---------------------------------------------------------------------------
-- 3. Disable everything else in product_code=1002.
-- ---------------------------------------------------------------------------
UPDATE gsc_game_catalog SET is_available = 0
WHERE product_code = 1002
  AND game_code NOT IN (
    'LotusSpeedBac001','LotusSpeedBac002','LotusSpeedBac003',
    '90w0v3h2o52rjknf','90w1u5e3o52rlmn6','oytmvb9m1zysmc44',
    '60i0lcfx5wkkv3sy','8v9f4xhuhd005xlb','XXXtremeLightBac',
    'LightningBacc001','BacBo00000000001','LotusSicBo000001',
    'SuperSicBo0000001','SpeedSicBo000001','EmperorSB0000001',
    'DragonTiger00001','LotusRoulette001','48z5pjps3ntvqc1b',
    'SpeedAutoRou0001','mrfykemt5slanyi5','HoldemTable00001',
    'IceFishing000001','CrazyTime0000001'
  );

-- ---------------------------------------------------------------------------
-- 4. Verify.
-- ---------------------------------------------------------------------------
SELECT product_code,
       SUM(is_available = 1) AS visible,
       SUM(is_available = 0) AS hidden,
       COUNT(*) AS total
FROM gsc_game_catalog
WHERE product_code = 1002
GROUP BY product_code;
-- Expected: visible=23, hidden=339, total=362.

-- ---------------------------------------------------------------------------
-- 5. Restart portal-api to bust the 5-min in-memory cache:
--    docker restart sunwinkr-portal-api
-- ---------------------------------------------------------------------------

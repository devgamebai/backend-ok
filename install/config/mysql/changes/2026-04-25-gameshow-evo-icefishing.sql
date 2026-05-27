-- 2026-04-25 — Move Ice Fishing from "Other" → "GameShow" for Evolution (1002)
-- Per QC's CSV / user confirmation: GameShow category for Evolution should
-- contain exactly 2 games: IceFishing000001 + CrazyTime0000001.
-- CrazyTime was already there; IceFishing was mis-categorized as Other.
--
-- Idempotent. Archive: _archive_gameshow_evo_20260425

USE vinplay;

CREATE TABLE IF NOT EXISTS _archive_gameshow_evo_20260425 AS
SELECT product_code, game_code, game_name, game_name_vi, category, active, sort_order, NOW() AS archived_at
  FROM gsc_game_catalog
 WHERE product_code=1002 AND game_code='IceFishing000001';

UPDATE gsc_game_catalog
   SET category='GameShow'
 WHERE product_code=1002
   AND game_code='IceFishing000001'
   AND category<>'GameShow';

-- After applying, restart portal-api: docker restart sunwinkr-portal-api

-- Rollback for GitLab #36 — Evolution curate whitelist.
-- Restores gsc_game_catalog.is_available for product_code=1002 from
-- the archive table created at migration time.

USE vinplay;

UPDATE gsc_game_catalog g
JOIN _archive_evo_curate_20260422 a
  ON g.product_code = a.product_code AND g.game_code = a.game_code
SET g.is_available = a.is_available
WHERE g.product_code = 1002;

-- Verify counts match pre-migration state:
SELECT product_code,
       SUM(is_available = 1) AS visible,
       SUM(is_available = 0) AS hidden
FROM gsc_game_catalog
WHERE product_code = 1002
GROUP BY product_code;

-- Optional: drop the archive once verified
-- DROP TABLE _archive_evo_curate_20260422;

-- Restart portal-api to bust the 5-min in-memory cache:
--   docker restart sunwinkr-portal-api

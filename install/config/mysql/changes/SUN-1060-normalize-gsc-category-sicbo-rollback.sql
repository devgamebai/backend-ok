-- Rollback for SUN-1060-normalize-gsc-category-sicbo.sql
-- Restores the 2 Evolution rows' garbage category values from archive.

USE vinplay;

UPDATE gsc_game_catalog g
JOIN _archive_sun1060_catalog_category_20260424 a
  ON g.product_code = a.product_code AND g.game_code = a.game_code
SET g.category = a.category;

SELECT category, COUNT(*) FROM gsc_game_catalog
WHERE product_code=1002 AND game_name LIKE '%Sic Bo%'
GROUP BY category;

-- Optional once satisfied:
-- DROP TABLE _archive_sun1060_catalog_category_20260424;

-- Restart portal-api to bust lobby cache:
--   docker restart sunwinkr-portal-api

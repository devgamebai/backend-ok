-- SUN-980 rollback — restore commission_eligible=1 on fish games.

UPDATE vinplay.gsc_game_catalog
SET commission_eligible = 1
WHERE product_code IN (1009, 1091, 1085)
  AND (category = 'Fishing' OR game_type = 'FISHING');

SELECT product_code, COUNT(*) AS n_eligible
FROM vinplay.gsc_game_catalog
WHERE commission_eligible = 1 AND product_code IN (1009, 1091, 1085)
GROUP BY product_code;

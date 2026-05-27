-- SUN-980 — seed the initial "not commission-eligible" rows on gsc_game_catalog.
--
-- Targets the seamless-transfer fishing providers we currently know about:
--   1009 CQ9 (7 games)
--   1091 JILI (15 fishing games out of 176 total — slots stay eligible)
--   1085 unknown provider (9 fishing games)
-- Only games with category='Fishing' OR game_type='FISHING' are flipped.
-- Slot games on the same providers keep commission_eligible=1 (they send
-- real per-bet webhooks with valid_bet_amount > 0).
--
-- Rollback: SUN-980-seed-fish-ineligible-rollback.sql in this folder.

UPDATE vinplay.gsc_game_catalog
SET commission_eligible = 0
WHERE product_code IN (1009, 1091, 1085)
  AND (category = 'Fishing' OR game_type = 'FISHING');

-- Verification — expect ~31 rows flipped (7 + 15 + 9).
SELECT product_code, COUNT(*) AS n_ineligible
FROM vinplay.gsc_game_catalog
WHERE commission_eligible = 0
GROUP BY product_code
ORDER BY product_code;

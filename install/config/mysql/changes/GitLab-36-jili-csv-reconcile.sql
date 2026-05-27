-- GitLab #36: JILI catalog reconciliation (2026-04-23)
-- JILI game_code '696' (Fortune Garuda 500) existed in upstream GSC but was
-- missing from gsc_game_catalog — overlay defaulted to active=true and the
-- game leaked through the curate whitelist (which intends to show only
-- game_code='49' / Super Ace). Insert-with-is_available=0 plugs the leak.
--
-- Net effect on player lobby: 2 → 1 visible JILI game, matching intent.

USE vinplay;

INSERT INTO gsc_game_catalog
  (product_code, game_code, game_name, game_type, category, image_url, is_available, sort_order)
VALUES
  (1091, '696', 'Fortune Garuda 500', 'SLOT', 'Slot', '', 0, 0)
ON DUPLICATE KEY UPDATE is_available = 0;

-- Verify: only game_code='49' should be available in 1091
SELECT SUM(is_available=1) AS available, SUM(is_available=0) AS disabled
FROM gsc_game_catalog WHERE product_code=1091;

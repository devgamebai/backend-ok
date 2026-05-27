-- Add missing providers discovered via GSC /api/operators/provider-games scan (1001-1300)
-- Only providers with >0 active games on operator G7A1

INSERT INTO gsc_product_map (product_code, game_type, provider_name, game_key, game_label_vi, category, is_active) VALUES
  (1006, '*', 'Pragmatic Play',   'wm',  'Pragmatic Play',  'SLOT',    1),
  (1009, '*', 'Fachai Fishing',   'wm',  'Fachai Fishing',  'FISH',    1),
  (1052, '*', 'Dream Gaming',     'ag',  'Dream Gaming',    'CASINO',  1),
  (1085, '*', 'JDB',              'wm',  'JDB',             'SLOT',    1),
  (1091, '*', 'JILI',             'wm',  'JILI',            'SLOT',    1),
  (1149, '*', 'Hash Gaming',      'ag',  'Hash Gaming',     'CASINO',  1),
  (1185, '*', 'SA Gaming',        'ag',  'SA Gaming',       'CASINO',  1),
  (1194, '*', 'WM Live',          'ag',  'WM Live',         'CASINO',  1),
  (1291, '*', 'MX Live',          'ag',  'MX Live',         'CASINO',  1)
ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  game_key = VALUES(game_key),
  game_label_vi = VALUES(game_label_vi),
  category = VALUES(category),
  is_active = 1;

SELECT product_code, provider_name, game_key, category FROM gsc_product_map ORDER BY product_code;

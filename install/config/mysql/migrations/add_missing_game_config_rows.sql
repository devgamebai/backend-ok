-- Silence startup warnings from VinPlayUserCore GameCommon.init() by adding empty
-- JSON placeholders for missing optional config sections. These features (esms,
-- vtc_vcoin, mini_prizes, revenue_config) aren't wired up on this stack but the
-- init code tries to parse them anyway; empty {} lets parse succeed, features
-- stay disabled by default.

INSERT IGNORE INTO vinplay.game_config (name, value) VALUES
  ('esms',            '{}'),
  ('vtc_vcoin',       '{}'),
  ('mini_prizes',     '{}'),
  ('revenue_config',  '{}');

-- =============================================================================
-- 20260518_awc_sexybcrt_baccarat_per_table_names_phaseC.sql
--
-- SUN-1252 Phase C — Mexico-numbered Sexy Baccarat tables observed in prod.
--
-- Phase A (20260508_awc_sexybcrt_per_game_names.sql) covered the
-- non-Baccarat variants (Dragon Tiger / Roulette / Sicbo / Sedie).
-- Phase B (20260517_awc_sexybcrt_baccarat_per_table_names.sql) covered
-- the Macau hall C01..C15.
--
-- Phase C — scan of log_awc_bets on prod showed 30 distinct table_tags
-- in active play. 15 were already in catalog from Phase B; the other 16
-- below are added here:
--   * Mexico-numbered halls (plain digits NN/NNN): 01, 05, 06, 09, 10,
--     31, 32, 71, 72, 131, 151 — Sexy's "Mexico" branded studio.
--   * Extended Macau hall (C16, C17, C18) — beyond the standard C01-C15.
--   * Three-digit C-prefixed halls (C132, C151) — newer Sexy private
--     halls; preserve the parser's `[A-Z]?\d{1,3}` capture as the
--     table_tag (so the parseTableSuffix + catalog lookup stay
--     symmetric).
--
-- All rows reuse `game_code='MX-LIVE-001'` (Sexy ships one game_code per
-- game-type; table differentiation lives in `table_tag` parsed from the
-- round_id by SexyBcrtAdapter.parseTableSuffix).
--
-- Idempotent: INSERT IGNORE on UNIQUE
-- (provider, vendor_platform, game_code, table_tag).
-- =============================================================================

INSERT IGNORE INTO vinplay.games
    (provider, vendor_platform, game_code, table_tag, game_name, category_id, is_active)
VALUES
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '01', 'Sexy Baccarat 01', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '05', 'Sexy Baccarat 05', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '06', 'Sexy Baccarat 06', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '09', 'Sexy Baccarat 09', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '10', 'Sexy Baccarat 10', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '131', 'Sexy Baccarat 131', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '151', 'Sexy Baccarat 151', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '31', 'Sexy Baccarat 31', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '32', 'Sexy Baccarat 32', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '71', 'Sexy Baccarat 71', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '72', 'Sexy Baccarat 72', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C132', 'Sexy Baccarat C132', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C151', 'Sexy Baccarat C151', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C16', 'Sexy Baccarat C16', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C17', 'Sexy Baccarat C17', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C18', 'Sexy Baccarat C18', 1, 1);

-- Verification:
--   SELECT COUNT(*) FROM vinplay.games
--    WHERE provider='AWC' AND vendor_platform='SEXYBCRT'
--      AND game_code='MX-LIVE-001';
--   -- expected: 32 rows (1 fallback + 15 Phase B + 16 Phase C)

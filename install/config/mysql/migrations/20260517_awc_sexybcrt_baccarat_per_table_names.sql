-- =============================================================================
-- 20260517_awc_sexybcrt_baccarat_per_table_names.sql
--
-- SUN-1252 Phase B — per-table names for Sexy Baccarat.
--
-- WHY THIS EXISTS
-- ----------------
-- 20260508_awc_sexybcrt_per_game_names.sql covered the non-Baccarat
-- variants (Dragon Tiger / Roulette / Sicbo / Sedie — distinct game_codes)
-- but explicitly deferred Baccarat tables: every Sexy Baccarat table
-- reuses game_code MX-LIVE-001, differentiated only by table_tag parsed
-- from round_id ("Mexico-C07-GA..." → "C07"). AwcGameNameResolver
-- supports the per-(platform, gameCode, table_tag) lookup but vinplay.games
-- had no rows with table_tag set for Baccarat — every Sexy Baccarat bet
-- displayed as "SEXYBCRT (default)" in LS Cược / LS Rolling.
--
-- Verified in prod 2026-05-17: log_awc_bets shows tables C01, C06, C15
-- already in active play.
--
-- WHAT
-- ----
-- C01..C15 — the full Macau casino-style hall (Sexy's standard offering).
-- Plus a fallback row (table_tag='') so MX-LIVE-001 with an unparseable
-- round_id falls back to "Sexy Baccarat" instead of the generic platform
-- stub.
--
-- Mexico-numbered tables (M01-M99) and pure-numeric Mexico halls are NOT
-- added here — they're a different studio variant. If those start showing
-- up in log_awc_bets, add a Phase C migration with the observed tags.
--
-- IDEMPOTENT
-- ----------
-- INSERT IGNORE keyed on UNIQUE (provider, vendor_platform, game_code,
-- table_tag). Safe to re-run.
-- =============================================================================

INSERT IGNORE INTO vinplay.games
    (provider, vendor_platform, game_code, table_tag, game_name, category_id, is_active)
VALUES
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', '',    'Sexy Baccarat',     1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C01', 'Sexy Baccarat C01', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C02', 'Sexy Baccarat C02', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C03', 'Sexy Baccarat C03', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C04', 'Sexy Baccarat C04', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C05', 'Sexy Baccarat C05', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C06', 'Sexy Baccarat C06', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C07', 'Sexy Baccarat C07', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C08', 'Sexy Baccarat C08', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C09', 'Sexy Baccarat C09', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C10', 'Sexy Baccarat C10', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C11', 'Sexy Baccarat C11', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C12', 'Sexy Baccarat C12', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C13', 'Sexy Baccarat C13', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C14', 'Sexy Baccarat C14', 1, 1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-001', 'C15', 'Sexy Baccarat C15', 1, 1);

-- Verification: should now show 22 SEXYBCRT rows (4 variants + 16 Baccarat + 1 stub + 1 fallback)
SELECT provider, vendor_platform, game_code, table_tag, game_name
  FROM vinplay.games
 WHERE provider='AWC' AND vendor_platform='SEXYBCRT'
 ORDER BY game_code, table_tag;

-- SUN-1252 / SUN-1258 / SUN-1259: AWC Sexy Live tables show wrong names
-- in agency LS Cược / LS Rolling.
--
-- Root cause: vinplay.games only had 2 rows for AWC SEXYBCRT — the
-- platform stub (game_code='*' → "SEXYBCRT (default)") and a single
-- baccarat row. Every other Sexy variant (Dragon Tiger, Roulette,
-- Extra Sicbo, Sedie) fell back to the stub label.
--
-- Phase A — backfill per-game_code rows for the variants seen on
-- staging in log_awc_bets (verified 2026-05-08 against win123club mongo):
--   MX-LIVE-001 BaccaratClassic | MX-LIVE-006 DragonTiger
--   MX-LIVE-009 Roulette        | MX-LIVE-016 Extra Sicbo
--   MX-LIVE-017 Sedie
--
-- SUN-1252 (Baccarat C01-C15 collapse) is NOT addressed here — every
-- Sexy Baccarat table reuses game_code MX-LIVE-001 and only differs by
-- round_id prefix. Per-table differentiation is Phase B.
--
-- Idempotent: INSERT IGNORE on (provider, vendor_platform, game_code).

INSERT IGNORE INTO vinplay.games
    (provider, vendor_platform, game_code, game_name, category_id, is_active)
VALUES
    ('AWC', 'SEXYBCRT', 'MX-LIVE-006', 'Sexy Dragon Tiger', 2,  1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-009', 'Sexy Roulette',     3,  1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-016', 'Sexy Extra Sicbo',  4,  1),
    ('AWC', 'SEXYBCRT', 'MX-LIVE-017', 'Sexy Sedie',        11, 1);

-- Verification: should now show 6 rows for AWC SEXYBCRT.
SELECT provider, vendor_platform, game_code, game_name, category_id
  FROM vinplay.games
 WHERE provider = 'AWC' AND vendor_platform = 'SEXYBCRT'
 ORDER BY game_code;

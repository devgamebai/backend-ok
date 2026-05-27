-- Rollback for SUN-1076-zero-legacy-commission-rates.sql
-- Restores rate=1.25 rows from the archive table.

USE vinplay;

UPDATE game_commission_rate g
JOIN _archive_sun1076_legacy_rates_20260423 a
  ON g.id = a.id
SET g.rate = a.rate
WHERE a.rate = 1.25;

-- Verify
SELECT rate, COUNT(*) FROM game_commission_rate GROUP BY rate ORDER BY COUNT(*) DESC;

-- Optional once verified:
-- DROP TABLE _archive_sun1076_legacy_rates_20260423;

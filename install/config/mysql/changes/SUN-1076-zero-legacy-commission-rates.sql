-- SUN-1076 — zero out legacy 1.25% commission rates written by the
-- pre-fix AdminMakeAgentProcessor (fixed in e3e4b873, 2026-04-22).
--
-- Background:
--   The old seedDefaultGameCommissionRates() helper inserted rate=1.25
--   for taixiu, taixiu_st, wm, ag, ebet, ibc, cmd, sbo when a new agent
--   was created. The helper now inserts 0 for every game, but rows
--   written under the old code still have rate=1.25 and continue to
--   appear in the admin CMS commission config.
--
--   Additionally, seedDefaultGameCommissionRates uses
--   INSERT ... ON DUPLICATE KEY UPDATE rate=rate (no-op on duplicate).
--   Even if a deleted agent's nickname is reused, the old 1.25 rows
--   survive because the new INSERT finds an existing row and keeps it.
--
-- This script nulls those rows to 0 one time. Safe to re-run: the UPDATE
-- matches only rate=1.25 rows, which the current code never writes.
--
-- Rollback:
--   install/config/mysql/changes/SUN-1076-zero-legacy-commission-rates-rollback.sql

USE vinplay;

-- ---------------------------------------------------------------------------
-- 1. Archive pre-change state.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _archive_sun1076_legacy_rates_20260423 AS
SELECT id, agent_nickname, game_key, rate, updated_at, NOW() AS archived_at
FROM game_commission_rate
WHERE rate = 1.25;

SELECT COUNT(*) AS rows_to_be_zeroed FROM game_commission_rate WHERE rate = 1.25;

-- ---------------------------------------------------------------------------
-- 2. Zero the 1.25 rows.
--    Scope narrow: only touches rows whose value is exactly 1.25. If product
--    has since set a different rate (1.00, 0.80, 0.90, etc.) we leave it
--    alone. If product wants to zero ALL non-customised defaults, they'll
--    ship a separate migration scoped by agent_nickname.
-- ---------------------------------------------------------------------------
UPDATE game_commission_rate SET rate = 0 WHERE rate = 1.25;

-- ---------------------------------------------------------------------------
-- 3. Verify.
-- ---------------------------------------------------------------------------
SELECT rate, COUNT(*) AS cnt
FROM game_commission_rate
GROUP BY rate
ORDER BY cnt DESC;
-- Expected: 1.25 row is gone (or cnt=0).

-- ---------------------------------------------------------------------------
-- 4. No portal-api restart needed — ListGameCommissionConfigProcessor reads
--    game_commission_rate on every call and has no in-memory cache.
-- ---------------------------------------------------------------------------

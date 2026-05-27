-- SUN-1250 reopen: backfill rebate_logs.wager_code for AWC rows.
--
-- Why
-- ---
-- SUN-1248 introduced rebate_logs.wager_code (round-level grouping key)
-- so multi-bet rounds collapse to one row in agency LS Rolling. GSC
-- writers populate it; the AWC callback path was missed (the mongo
-- log_awc_bets row carries it, but LogMoneyUserMessage didn't, so the
-- rebate row landed with wager_code=NULL).
--
-- Result: Cyan's repro (Sexy Live Baccarat 5/6, bet 300 → bet 50 same
-- round) shows two LS Rolling rows instead of one — exactly what the
-- ticket originally fixed for Evolution.
--
-- The code fix (AwcCallbackProcessor.triggerCommission setWagerCode)
-- handles all NEW bets. This script repairs existing data.
--
-- Source-of-truth join
-- --------------------
-- rebate_logs.source_key carries "awc:<platformTxId>" — strip the prefix
-- and look up log_awc_bets.platform_tx_id → round_id (or platform_tx_id
-- if round_id was empty, mirroring the mongo writer's null-coalesce).
--
-- log_awc_bets is a Mongo collection, NOT a MySQL table. Run the Mongo
-- companion script (`scripts/sun1250_export_awc_round_ids.js`) first to
-- export a CSV of (platform_tx_id, round_id) pairs into a temp table:
--
--   docker exec -i sunwinkr-mongodb mongosh ... --quiet \
--     --eval "..." > /tmp/awc_rounds.csv
--   docker exec -i sunwinkr-mysql mysql -uroot -p... vinplay <<EOF
--     CREATE TEMPORARY TABLE awc_round_map (platform_tx_id VARCHAR(160) PRIMARY KEY, round_id VARCHAR(160));
--     LOAD DATA LOCAL INFILE '/tmp/awc_rounds.csv' INTO TABLE awc_round_map FIELDS TERMINATED BY ',';
--   EOF
--
-- Then run this script.

USE vinplay;

-- Verification first — how many AWC rebate rows are missing wager_code?
SELECT 'before' AS phase, COUNT(*) AS rows_missing_wager
FROM rebate_logs
WHERE source_key LIKE 'awc:%' AND (wager_code IS NULL OR wager_code = '');

-- Backfill from awc_round_map (loaded from Mongo). The pattern strips
-- the "awc:" prefix from source_key to recover platform_tx_id.
UPDATE rebate_logs r
JOIN awc_round_map m ON m.platform_tx_id = SUBSTRING(r.source_key, 5)
SET r.wager_code = COALESCE(NULLIF(m.round_id, ''), m.platform_tx_id)
WHERE r.source_key LIKE 'awc:%'
  AND (r.wager_code IS NULL OR r.wager_code = '');

-- Fallback: any remaining AWC rows that the round map could not cover
-- (Mongo doc missing / older AWC writer that never stamped round_id) —
-- use the platform_tx_id itself so the row is at least groupable. This
-- means single-bet rounds with no add-on collapse trivially (wager_code
-- == source_key suffix, one row per group), while orphan multi-bet rows
-- without an export entry stay split (acceptable — the live writer fix
-- handles all new traffic).
UPDATE rebate_logs
SET wager_code = SUBSTRING(source_key, 5)
WHERE source_key LIKE 'awc:%'
  AND (wager_code IS NULL OR wager_code = '');

-- Verify
SELECT 'after' AS phase, COUNT(*) AS rows_missing_wager
FROM rebate_logs
WHERE source_key LIKE 'awc:%' AND (wager_code IS NULL OR wager_code = '');

-- Sanity: how many distinct wager_code values now in AWC rows?
SELECT COUNT(DISTINCT wager_code) AS awc_wager_codes,
       COUNT(*)                  AS awc_total_rows
FROM rebate_logs WHERE source_key LIKE 'awc:%';

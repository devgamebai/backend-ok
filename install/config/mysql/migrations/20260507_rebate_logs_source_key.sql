-- SUN-1248: add source_key column to rebate_logs.
--
-- LogMoneyUserExtraProcessor.insertPendingLogIfAbsent (vbee) writes
-- source_key as the per-txn dedup id (e.g. "gsc:<txn_id>",
-- "awc:<platformTxId>"). Without the column, every commission emit
-- fails at the INSERT with "Unknown column 'source_key' in 'field
-- list'", silently dropping rebate_logs rows for AWC and GSC bets.
--
-- The companion migration 20260507_rebate_logs_wager_code.sql
-- assumed source_key already existed (its `AFTER source_key` clause
-- failed on staging during the MR !385 sync). This migration
-- back-fills the column so wager_code can sit beside it as the
-- code expects.
--
-- Rollback:
--   ALTER TABLE rebate_logs DROP INDEX idx_source_key, DROP COLUMN source_key;

ALTER TABLE rebate_logs
    ADD COLUMN source_key VARCHAR(160) NULL DEFAULT NULL,
    ADD INDEX idx_source_key (source_key);

-- =============================================================================
-- SUN-1248 — Mr.DEAL multi-bet baccarat fix, ledger-compliant.
--
-- Problem: when a player adds to their bet during one round (Evolution
-- Baccarat 1/4/5/6/Cxx send 2+ withdraw callbacks per wager_code), each
-- sub-bet creates a separate rebate_logs row. The previous quick fix
-- mutated the existing row's volume — which violates the ledger spec's
-- "transactions are immutable" principle and silently double-counts on
-- RMQ redelivery.
--
-- Proper fix: treat rebate_logs as a true ledger.
--   - Each sub-bet writes its own immutable row (per-txn dedup via source_key)
--   - New `wager_code` column groups sub-txs of one round
--   - The reader (c=9541) GROUPs BY wager_code and SUMs volume/amount,
--     so Rolling shows ONE row per round to the operator
--
-- Run order: apply this migration BEFORE the Java deploy that switches
-- back to per-txn source-key. The column is NULLable so legacy rows
-- (no wager_code) keep working until backfill lands.
-- =============================================================================

ALTER TABLE rebate_logs
    ADD COLUMN wager_code VARCHAR(160) NULL DEFAULT NULL AFTER source_key,
    ADD INDEX idx_wager_code (wager_code, period_start);

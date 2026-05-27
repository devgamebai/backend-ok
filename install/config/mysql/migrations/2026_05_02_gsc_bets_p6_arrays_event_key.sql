-- =============================================================================
-- P6.1 + P6.2 — log_gsc_bets reader-cutover prerequisites
--
-- Adds JSON arrays + UNIQUE event_key so vinplay.gsc_bets can mirror the
-- four Mongo log_gsc_bets behaviours that admin-CMS / audit / reconciler
-- readers depend on:
--
--   - $addToSet txn_ids        — multi-bet hand sub-bet provider tx ids
--   - $addToSet settle_txn_ids — multi-payout settle provider tx ids
--   - $push details            — per-bet line breakdown (admin CMS drill-down)
--   - upsert(event_key)        — c=303 baccarat / c=9843 dragon-tiger merge
--                                 sub-bets of one logical hand onto a single row
--
-- Without this migration the MySQL mirror cannot be the authoritative
-- source for hand-detail reads — multi-bet hands get split across rows
-- and per-line breakdown is lost. See docs/MONEY_LEDGER_OPEN_TASKS.md
-- § "log_gsc_bets MySQL Migration — Reader-Cutover Prerequisites" P6.1/P6.2.
--
-- Backward compat: dual-write writers that don't yet emit the JSON
-- columns leave them NULL (the column default). Existing readers that
-- only read scalar columns are unaffected.
-- =============================================================================

ALTER TABLE vinplay.gsc_bets
    ADD COLUMN txn_ids JSON NULL
        COMMENT 'Provider txn_ids merged into the row ($addToSet on Mongo). Multi-bet hand sub-bet ids.',
    ADD COLUMN settle_txn_ids JSON NULL
        COMMENT 'Provider settle txn_ids ($addToSet on Mongo). Multi-payout settle ids.',
    ADD COLUMN details JSON NULL
        COMMENT 'Per-bet line breakdown ($push on Mongo). Schema: [{txn_id, amount, raw_amount, action}].';

-- Promote idx_event_key (non-unique) to uk_event_key UNIQUE so
-- INSERT ... ON DUPLICATE KEY UPDATE merges sub-bets of one hand onto
-- a single row. MySQL allows multiple NULLs in UNIQUE, so the
-- non-event_key insert path (event_key IS NULL → wager_code dedup via
-- uk_wager_code) keeps working unchanged.
ALTER TABLE vinplay.gsc_bets
    DROP INDEX idx_event_key,
    ADD UNIQUE KEY uk_event_key (event_key);

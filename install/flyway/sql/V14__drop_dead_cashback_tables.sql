-- SUN-1099 cleanup: drop the orphan cashback_logs flow.
--
-- Background: tbl_cashback_logs + tbl_cashback_log_game_detail were
-- the original SUN-764 design tables for batched volume cashback.
-- That design was superseded by the per-bet RealTimeCommission flow
-- (writes rebate_logs SELF, claimed via c=3083 ClaimCashbackProcessor).
--
-- These two tables had:
--   - 0 rows on staging
--   - 0 INSERT writers anywhere in the codebase
--   - readers only in 4 dead processors (BatchCashbackPayoutProcessor,
--     CheckCashbackExpiryProcessor, GetCashbackLogGameDetailProcessor,
--     GetRefundHistoryProcessor) plus GetCashbackHistoryProcessor — all
--     deleted in this cleanup.
--
-- Active cashback infrastructure (KEPT):
--   - tbl_cashback_config         (rate config, 1 row, read by RealTimeCommission)
--   - tbl_cashback_game_config    (per-game rate, read by RealTimeCommission)
--   - tbl_cashback_changelog      (config audit, used by V12 triggers + logChange)
--
-- Idempotent: DROP IF EXISTS so re-applying the migration is safe.

DROP TABLE IF EXISTS vinplay.tbl_cashback_log_game_detail;
DROP TABLE IF EXISTS vinplay.tbl_cashback_logs;

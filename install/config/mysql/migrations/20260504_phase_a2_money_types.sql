-- Phase A2 — money column type fixes
-- Date: 2026-05-04
-- Audit: docs/db-audit/AUDIT_PHASE1_FINDINGS.md
--
-- Fixes:
--   * log_hoan_tra: 6 INT money columns → BIGINT (overflow at ~2.1B VND ≈ $80k)
--   * log_hoan_tra_histories: same 6 columns → BIGINT
--
-- Rationale:
--   INT max = 2^31 - 1 = 2,147,483,647. Single high-roller daily turnover
--   already breaks $80k threshold today. Both tables empty (0 rows in staging
--   on 2026-05-04) so conversion is free.
--
-- Skipped (already correct):
--   * rebate_logs.* — DECIMAL(20,4) for math, BIGINT only at wallet boundary
--   * rebate_payout — BIGINT correct (post-round)
--   * agency_wallet — BIGINT correct (wallet boundary)
--   * banca_bet_commission_log.bet_amount — BIGINT correct (whole-VIN turnover, comment confirms)
--   * tbl_cashback_* — slated for delete (dead flow per MR !230 redirect)
--
-- Optional next: promote hoan_tra_* (rebate output) to DECIMAL(20,4) if the
-- producer code keeps fractional VND mid-calc. Reading the producer code
-- before deciding — leaving as BIGINT for now.

ALTER TABLE vinplay.log_hoan_tra
    MODIFY COLUMN total_money_sport  BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_sport     BIGINT DEFAULT 0,
    MODIFY COLUMN total_money_casino BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_casino    BIGINT DEFAULT 0,
    MODIFY COLUMN total_money_game   BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_game      BIGINT DEFAULT 0;

ALTER TABLE vinplay.log_hoan_tra_histories
    MODIFY COLUMN total_money_sport  BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_sport     BIGINT DEFAULT 0,
    MODIFY COLUMN total_money_casino BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_casino    BIGINT DEFAULT 0,
    MODIFY COLUMN total_money_game   BIGINT DEFAULT 0,
    MODIFY COLUMN hoan_tra_game      BIGINT DEFAULT 0;

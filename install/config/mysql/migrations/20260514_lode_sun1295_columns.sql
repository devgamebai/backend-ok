-- SUN-1295 snapshot columns on lode (Lô Đề ticket table).
-- The new lottery-engine (PR-2) stamps rate_at_purchase + prize_multiplier
-- on the row at bet time so future LotteryMode rate changes cannot
-- retroactively alter pending bets. JdbcBetStore queries assume these
-- columns exist; legacy schema didn't have them → 500 on /history.
--
-- This migration is idempotent — safe to re-run.

ALTER TABLE vinplay_minigame.lode
    ADD COLUMN IF NOT EXISTS bet_unit BIGINT NULL AFTER bet_value,
    ADD COLUMN IF NOT EXISTS rate_at_purchase INT NULL AFTER bet_unit,
    ADD COLUMN IF NOT EXISTS prize_multiplier INT NULL AFTER rate_at_purchase;

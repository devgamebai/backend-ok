-- =============================================================================
-- 20260516_users_vin_nonneg_check.sql
--
-- DB-level guard: `users.vin >= 0` (or is_bot = 1 to allow legacy bot drift).
-- Complements the proc-level floor check added in
-- 20260516_atomic_money_procs_v5_floor_and_zero_dedup.sql so even a future
-- writer that bypasses our procs (e.g. ad-hoc admin UPDATE, raw JDBC,
-- legacy code we haven't ripped out yet) cannot push a real player wallet
-- negative. InnoDB rejects the UPDATE with chk_vin_nonneg; the caller's
-- Java exception bubbles up cleanly.
--
-- Asymmetric with bots (is_bot = 1) because:
--   - 7 bot accounts already have legacy negative vin (BanCa engine resets)
--     and we don't want to either backfill those bots or fail this migration
--     on existing data.
--   - Bot accounts are internal — even if a writer drives them more negative
--     it doesn't affect a real-money obligation.
--
-- Mirror constraint on money_account already exists:
--     CHECK (is_system = 1 OR balance >= 0)  -- chk_player_nonneg
-- This brings users.vin to parity with the ledger source of truth.
-- =============================================================================

USE vinplay;

-- Verify no real (non-bot) user is currently negative before adding the
-- constraint. If any exists the ADD CHECK would fail. Quanlu99 (id=9166)
-- is the one we know about — it'll be excluded by the constraint clause
-- (is_bot=0) so we need to refund/zero his vin first OR add it as exception
-- via WHERE clause not possible in CHECK.
--
-- Quanlu99 fix here: bring his vin back to 0 (matches money_account.balance
-- which IS 0). The -100 was caused by the double-debit bug; ledger already
-- has the correct state.
UPDATE users SET vin = 0 WHERE id = 9166 AND vin < 0 AND is_bot = 0;

-- Now add the constraint
ALTER TABLE users
    ADD CONSTRAINT chk_vin_nonneg
    CHECK (vin >= 0 OR is_bot = 1);

-- Post-flight verification:
--   SELECT COUNT(*) FROM users WHERE vin < 0 AND is_bot = 0;
--   -- expected: 0

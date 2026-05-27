-- SUN-13xx Phase 5 (staging-fast): drop cgame.users.cash + cash_safe + cash_silver
--
-- Pre-flight (staging):
--   52 cgame users had cash > 0 (total 90.5M VND)
--   0 had cash_safe > 0
--   0 had cash_silver > 0
--
-- Drain procedure (_banca_cash_drain_v2 SP, run before this migration):
--   Match cgame.users → vinplay.users by (vinplay_user_id FK) OR (nickname/user_name)
--   For matched: post BANCA_CASH_MIGRATION transaction (DEBIT LEGACY_RECONCILIATION,
--   CREDIT PLAYER_VIN), bump vinplay.users.vin, zero cgame.cash
--   For unmatched (orphan): zero cgame.cash (write-off — these accounts have no
--   vinplay counterpart so cash cannot move)
--
-- Staging-fast trade-offs:
--   - Only ~3 cgame accounts matched real vinplay users; rest were orphans
--   - Production deploy MUST do account-by-account audit before this step
USE cgame;

ALTER TABLE users
    DROP COLUMN cash,
    DROP COLUMN cash_safe,
    DROP COLUMN cash_silver;

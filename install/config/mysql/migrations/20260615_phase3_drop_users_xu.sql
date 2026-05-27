-- ============================================================================
-- WALLET PHASE 3 — Drop `users.xu` and `users.xu_total` columns
-- ----------------------------------------------------------------------------
-- RFC:          docs/RFC_SINGLE_WALLET_UNIFICATION.md §Phase 3 (gate: 14-day soak)
-- Jira:         SUN-13xx (Phase 3 column drop)
-- Author:       Backend / Wallet Unification team
-- Created:      2026-06-15 (14 days after 20260601_phase3a_xu_collapse_to_vin.sql)
-- ============================================================================
--
-- PRECONDITIONS (every one MUST be true before running):
--   ☐ 20260601_phase3a_xu_collapse_to_vin.sql ran AND its STEP 4 verification
--     queries returned zero rows (no user has non-zero xu).
--   ☐ Java MoneyGateway / UserServiceImpl / GiftCodeDAOImpl xu-aware branches
--     are deployed in their "vin-only" Option-A form for 14 consecutive days.
--   ☐ Drift monitor has reported zero xu drift for 14 days.
--   ☐ `lint scan` of all backend modules shows zero remaining writes to
--     `users.xu` / `users.xu_total` outside this migration set.
--   ☐ FE confirmed (see WALLET_PHASE3_FE_COORDINATION.md) that they are no
--     longer reading the `xu` field from responses (or are tolerant of `0`).
--   ☐ Snapshot taken: snapshots/wave-3-pre-{date}.sql.gz
--
-- WHAT
--   Drops the two columns. Irreversible without snapshot restore. This is
--   the point-of-no-return for Option A.
--
-- ROLLBACK
--   Fix-forward only. Snapshot restore is DR, not rollback.
-- ============================================================================

USE vinplay;

-- ---------------------------------------------------------------------------
-- Safety guard: refuse to drop if any user still has non-zero xu.
-- ---------------------------------------------------------------------------
SET @nonzero := (SELECT COUNT(*) FROM users WHERE xu <> 0 OR xu_total <> 0);
SET @msg := IF(@nonzero = 0,
               'OK: 0 users with nonzero xu/xu_total — safe to drop',
               CONCAT('ABORT: ', @nonzero, ' users still have nonzero xu/xu_total. Re-run phase3a first.'));
SELECT @msg AS preflight;

-- The signal below is intentionally noisy. If you really want to force-drop
-- (e.g. you have already cleaned the rows manually), comment it out — but do
-- not commit that change.
DROP PROCEDURE IF EXISTS _phase3_preflight;
DELIMITER $$
CREATE PROCEDURE _phase3_preflight()
BEGIN
    IF @nonzero > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 3 drop aborted: users with nonzero xu remain';
    END IF;
END$$
DELIMITER ;
CALL _phase3_preflight();
DROP PROCEDURE _phase3_preflight;

-- ---------------------------------------------------------------------------
-- Drop the columns
-- ---------------------------------------------------------------------------
ALTER TABLE users
    DROP COLUMN xu,
    DROP COLUMN xu_total;

-- ---------------------------------------------------------------------------
-- Post-drop: log to migration_history if that table exists in this env.
-- ---------------------------------------------------------------------------
-- INSERT INTO migration_history(name, applied_at, notes)
-- VALUES ('20260615_phase3_drop_users_xu',
--         NOW(),
--         'Phase 3 column drop — xu retired per RFC_SINGLE_WALLET_UNIFICATION');

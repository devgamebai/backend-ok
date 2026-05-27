-- SUN-13xx Phase 4 — drop legacy update_money_db SP + retire vin_total/xu_total
--
-- DO NOT APPLY UNTIL ALL OF THE FOLLOWING ARE TRUE:
--
--   1. Phase 1 ran on production with UNIFIED_WALLET_PHASE_1=on for ≥14 days
--   2. wallet_drift_snapshot.drifting_users = 0 for all 14 daily readings:
--          SELECT MAX(drifting_users) FROM wallet_drift_snapshot
--           WHERE snapshot_at >= DATE_SUB(NOW(), INTERVAL 14 DAY);
--   3. Smoke-pack tests/wallet-unification/phase1_smoke.sh green for 14 days
--   4. Pre-flight snapshot taken per WALLET_UNIFICATION_PRODUCTION_RUNBOOK.md
--   5. Operator + observer both signed off (two-person rule)
--
-- After this migration runs:
--   - Java code that calls CALL update_money_db(...) will fail with
--     "PROCEDURE vinplay.update_money_db does not exist" — every caller MUST
--     have been migrated to update_money_db_v2 BEFORE this is applied. By the
--     time we reach Phase 4 the UserDaoImpl flag should be hard-coded to v2
--     and the env-var gate removed; if you see ANY reference to
--     update_money_db (not v2) in the deployed JAR set, STOP.
--   - BalanceGuard.clamp() becomes a no-op (vin_total is gone so no negative-flip
--     bug can recur). Class is intentionally left in tree as a safety net; can
--     be removed in a follow-up clean-up commit.
--   - SELECT vin_total / xu_total FROM users will fail. Every read site must
--     have been migrated to v_derived_player_pnl in Phase 1.
USE vinplay;

-- 1) Drop the legacy stored procedure. The v2 SP stays.
DROP PROCEDURE IF EXISTS update_money_db;

-- 2) Drop the legacy *_total columns. ALGORITHM=INPLACE keeps the table online
--    while the column is being removed; LOCK=NONE prevents blocking writers.
--    On a small users table this is sub-second anyway, but we want zero
--    downtime if the table grows.
ALTER TABLE users
    DROP COLUMN vin_total,
    DROP COLUMN xu_total,
    ALGORITHM=INPLACE,
    LOCK=NONE;

-- 3) Sanity probe. Operator should see zero rows.
SELECT 'Phase 4 verification' AS step,
       (SELECT COUNT(*) FROM information_schema.ROUTINES
         WHERE ROUTINE_SCHEMA='vinplay' AND ROUTINE_NAME='update_money_db') AS legacy_sp_count,
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='users'
           AND COLUMN_NAME IN ('vin_total','xu_total')) AS legacy_cols_count;
-- Expected: legacy_sp_count=0, legacy_cols_count=0.

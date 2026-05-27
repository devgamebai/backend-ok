-- =============================================================================
-- Phase 2 — Drop legacy users.safe column (T+14 days).
--
-- Pre-conditions (must be verified manually before applying):
--   1. 20260512_phase2_player_vault.sql applied successfully.
--   2. 20260512_phase2_safe_migration.sql applied successfully.
--   3. All 17 game servers + 4 APIs running with MoneyGateway.lockFunds /
--      unlockFunds and the patched SafeBoxDaoImpl in production for at least
--      14 days (RFC §6 Phase 2: "After 14 days zero traffic on legacy path,
--      drop users.safe column").
--   4. Drift monitor reports 0 disagreement between PLAYER_VAULT balance and
--      the (now-frozen) users.safe column for those 14 days.
--   5. Lint scan finds zero remaining call sites that read or write users.safe.
--
-- Rollback: re-add the column from the pre-drop snapshot.
--     ALTER TABLE vinplay.users ADD COLUMN safe BIGINT NOT NULL DEFAULT 0;
--     -- then restore values from snapshots/wave-2-pre-{date}.sql.gz
-- =============================================================================

-- Drop only if the column still exists (idempotent in case of rerun).
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'vinplay'
      AND table_name   = 'users'
      AND column_name  = 'safe'
);

SET @sql := IF(@col_exists > 0,
    'ALTER TABLE vinplay.users DROP COLUMN safe',
    'SELECT ''users.safe already dropped'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Post-run verification:
-- SHOW COLUMNS FROM vinplay.users LIKE 'safe';   -- expect 0 rows

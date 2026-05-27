-- =============================================================================
-- 20260518_users_bank_idx_bank_name.sql
--
-- SUN-1389 — back the new (bank_id, customer_name) uniqueness check
-- (UserBankDao.nameOnBankExistsForOtherUser) with a composite index.
--
-- WHY
-- ----
-- The new check runs on every bank-add and admin-edit. Without an
-- index it falls back to a full users_bank scan (~900 rows today,
-- growing), which is cheap now but linear in the active wallet
-- population. Composite index on (bank_id, customer_name(64))
-- prefixed-key keeps the lookup constant-time and reuses the bank_id
-- leading column for other filters.
--
-- NOT A UNIQUE CONSTRAINT
-- -----------------------
-- 103 (UPPER(TRIM(customer_name)), bank_id) tuples currently appear
-- for 2+ user_ids in production (313 user rows total). Worst rings
-- include "NGUYEN TRAN DAI PHU" / bank=21 (26 users) and "KIMJONGUN" /
-- bank=1 (17 users). A real UNIQUE constraint would fail to apply
-- against this data. Block-forward only at the application layer;
-- historical cleanup is operator / AML work, not a schema change.
--
-- IDEMPOTENT
-- ----------
-- information_schema-guarded — re-runs are no-ops.
-- =============================================================================

USE vinplay;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'users_bank'
      AND INDEX_NAME   = 'idx_users_bank_bankid_name'
);
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE users_bank ADD INDEX idx_users_bank_bankid_name (bank_id, customer_name(64)), ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT "idx_users_bank_bankid_name already present — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

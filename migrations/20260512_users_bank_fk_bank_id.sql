-- SUN-1xxx (2026-05-12): users_bank.bank_id FK migration
--
-- Background
-- ----------
-- users_bank.bank_name was stored as a free-text snapshot of banks.bank_name
-- at the moment the player linked their bank. The c=3002 read path JOINs on
--   banks ON ub.bank_name = b.bank_name
-- so when an admin renames a bank in `banks` (e.g. "농협" → "농협은행"), every
-- existing player's row stops matching → code/logo go NULL → play UI broken.
--
-- The 2026-05-12 audit also revealed laviai (id=62, users_bank.id=105) holding
-- "Vietcombank" — a pre-KR-cutover string that no longer matches any banks row.
--
-- Fix: add an FK `bank_id` so renaming `banks.bank_name` propagates to every
-- player automatically. Snapshot column `bank_name` is kept for legacy compat
-- (still written on insert/update) and as the "stored value at link time"
-- fallback when bank_id is NULL (i.e. the row predates this migration AND its
-- bank_name no longer joins to any banks row — exactly laviai's case).
--
-- Idempotent. Safe to re-run.

USE vinplay;

DELIMITER //
DROP PROCEDURE IF EXISTS _add_users_bank_fk//
CREATE PROCEDURE _add_users_bank_fk()
BEGIN
  -- 1) Add the column if it doesn't already exist.
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay' AND TABLE_NAME = 'users_bank' AND COLUMN_NAME = 'bank_id'
  ) THEN
    ALTER TABLE users_bank ADD COLUMN bank_id INT NULL AFTER bank_name;
  END IF;

  -- 2) Add the index if it doesn't already exist.
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'vinplay' AND TABLE_NAME = 'users_bank' AND INDEX_NAME = 'idx_users_bank_bank_id'
  ) THEN
    ALTER TABLE users_bank ADD KEY idx_users_bank_bank_id (bank_id);
  END IF;
END//
DELIMITER ;

CALL _add_users_bank_fk();
DROP PROCEDURE _add_users_bank_fk;

-- 3) Backfill from the existing JOIN. The 1 stale row (laviai's "Vietcombank")
--    will not match and will remain bank_id=NULL — by design, so the read path
--    can fall back to the snapshot and the player still sees something.
UPDATE users_bank ub
JOIN banks b ON ub.bank_name = b.bank_name
SET ub.bank_id = b.id
WHERE ub.bank_id IS NULL;

-- 4) Sanity check (informational; will appear in mysql session log).
SELECT
  COUNT(*) AS total_rows,
  SUM(CASE WHEN bank_id IS NOT NULL THEN 1 ELSE 0 END) AS resolved,
  SUM(CASE WHEN bank_id IS NULL THEN 1 ELSE 0 END) AS unresolved_legacy
FROM users_bank;

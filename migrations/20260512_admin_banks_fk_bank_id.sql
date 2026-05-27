-- SUN-1xxx (2026-05-12): admin_banks.bank_id FK migration
--
-- Background
-- ----------
-- admin_banks is the COMPANY's receiving-bank list (where deposits land).
-- Its `bank_name` column stores short codes ("CITI", "NH") because the
-- admin UI's bank picker sends `value: b.code || b.bank_short_name`.
-- The crypto deposit history (c=3022) then shows these codes to the player
-- instead of the canonical Korean fullname ("씨티은행", "농협").
--
-- Same fix as the 2026-05-12 users_bank.bank_id migration: add an FK so
-- the canonical name flows live through the JOIN and admin renames in
-- `banks` propagate everywhere automatically.
--
-- Idempotent. Safe to re-run.

USE vinplay;

DELIMITER //
DROP PROCEDURE IF EXISTS _add_admin_banks_fk//
CREATE PROCEDURE _add_admin_banks_fk()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay' AND TABLE_NAME = 'admin_banks' AND COLUMN_NAME = 'bank_id'
  ) THEN
    ALTER TABLE admin_banks ADD COLUMN bank_id INT NULL AFTER bank_name;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'vinplay' AND TABLE_NAME = 'admin_banks' AND INDEX_NAME = 'idx_admin_banks_bank_id'
  ) THEN
    ALTER TABLE admin_banks ADD KEY idx_admin_banks_bank_id (bank_id);
  END IF;
END//
DELIMITER ;

CALL _add_admin_banks_fk();
DROP PROCEDURE _add_admin_banks_fk;

-- Backfill: admin_banks.bank_name holds either a code (e.g. "CITI") OR a
-- canonical name (e.g. "씨티은행"). Match against banks by either column.
UPDATE admin_banks ab
JOIN banks b ON b.code = ab.bank_name OR b.bank_name = ab.bank_name
SET ab.bank_id = b.id
WHERE ab.bank_id IS NULL;

-- Sanity check
SELECT
  COUNT(*) AS total_rows,
  SUM(CASE WHEN bank_id IS NOT NULL THEN 1 ELSE 0 END) AS resolved,
  SUM(CASE WHEN bank_id IS NULL THEN 1 ELSE 0 END) AS unresolved_legacy
FROM admin_banks;

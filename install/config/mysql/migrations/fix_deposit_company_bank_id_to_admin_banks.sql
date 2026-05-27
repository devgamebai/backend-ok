-- ================================================================
-- SUN-85x (admin bank set consistency):
-- deposit_transactions.company_bank_id used to reference vinplay.company_banks,
-- a table nobody manages from the admin UI. The live admin CMS (sunkr-admin
-- PHP + sunkr-admin-next) and player-facing bank list (c=3014) both read
-- from admin_banks. Unifying on admin_banks means admins see exactly the
-- bank they set when reviewing deposit history.
--
-- This script remaps historical deposit rows from the stale
-- company_banks.id to the active admin_banks.id, so the new
-- "LEFT JOIN admin_banks cb ON cb.id = dt.company_bank_id" JOIN
-- resolves cleanly for existing data.
--
-- Idempotent + preview-first. Safe to rerun.
-- ================================================================

USE vinplay;

-- 1. Preview — before state
SELECT 'deposit_transactions.company_bank_id histogram (BEFORE):' AS info;
SELECT company_bank_id, COUNT(*) AS cnt FROM deposit_transactions GROUP BY company_bank_id ORDER BY 1;

-- 2. Pick the active admin bank (what the unified pipeline now uses)
SELECT 'Active admin bank:' AS info;
SELECT id, bank_name, bank_number, customer_name FROM admin_banks WHERE status = 1 ORDER BY id ASC LIMIT 1;

-- 3. Remap legacy company_bank_id values (anything not 0/NULL and not already
--    pointing to a live admin_banks.id) to the first active admin_banks.id.
--    Rows already pointing at an admin_banks.id stay intact.
UPDATE deposit_transactions dt
JOIN (
    SELECT id AS active_id
    FROM admin_banks
    WHERE status = 1
    ORDER BY id ASC
    LIMIT 1
) AS active ON 1 = 1
LEFT JOIN admin_banks ab_existing ON ab_existing.id = dt.company_bank_id
SET dt.company_bank_id = active.active_id
WHERE dt.company_bank_id IS NOT NULL
  AND dt.company_bank_id <> 0
  AND ab_existing.id IS NULL;

-- 4. Preview — after state
SELECT 'deposit_transactions.company_bank_id histogram (AFTER):' AS info;
SELECT company_bank_id, COUNT(*) AS cnt FROM deposit_transactions GROUP BY company_bank_id ORDER BY 1;

-- 5. Sanity — JOIN resolution check
SELECT 'Unresolved rows (company_bank_id not matching any admin_bank):' AS info;
SELECT COUNT(*) AS orphan
FROM deposit_transactions dt
LEFT JOIN admin_banks ab ON ab.id = dt.company_bank_id
WHERE dt.company_bank_id IS NOT NULL
  AND dt.company_bank_id <> 0
  AND ab.id IS NULL;

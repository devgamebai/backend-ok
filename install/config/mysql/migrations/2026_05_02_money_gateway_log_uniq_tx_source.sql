-- =============================================================================
-- money_gateway_log — promote idx_tx_source from non-unique to UNIQUE  (audit #17)
--
-- PROBLEM:
--   MoneyGateway.creditUser / debitUser / transferBetweenUsers dedup using:
--       1. SELECT 1 FROM money_gateway_log WHERE tx_id=? AND source=?  (isDuplicate)
--       2. UPDATE users.vin                                            (apply)
--       3. INSERT INTO money_gateway_log                               (audit)
--   The current `idx_tx_source` is a plain B-tree index (Non_unique=1), so two
--   concurrent webhook-retry calls for the same (tx_id, source) BOTH pass step 1,
--   BOTH apply step 2, and BOTH succeed at step 3 — wallet double-credited and
--   the audit table gets two rows. Phantom money.
--
-- FIX:
--   Promote `idx_tx_source` to a UNIQUE constraint `uk_tx_source` on
--   (tx_id, source, user_id).  Combined with the companion code change in
--   MoneyGateway (transactional UPDATE + INSERT IGNORE), MySQL's row-level
--   uniqueness lock serializes racing INSERTs so only one wins; the loser's
--   transaction is rolled back and the wallet UPDATE is undone.  Net effect:
--   at most one wallet movement per (tx_id, source) per user.
--
-- WHY (tx_id, source, user_id) AND NOT JUST (tx_id, source):
--   `transferBetweenUsers` legitimately writes TWO audit rows under the same
--   (tx_id, source) — one for the src user (negative amount) and one for the
--   dest user (positive amount).  A 2-column UNIQUE would reject the dest
--   INSERT and break every user-to-user transfer.  Adding user_id keeps the
--   two transfer sides distinct (different user_ids) while still preventing
--   a webhook retry of EITHER side from posting twice for the same user
--   (same tx_id, same source, same user_id → blocked).
--
--   The `isDuplicate` SELECT is `WHERE tx_id=? AND source=?` (no user_id),
--   so a retry of a transfer is still short-circuited by the pre-check, but
--   the AUTHORITATIVE race-safe gate is the unique key + INSERT IGNORE
--   inside the transaction.
--
-- NULL HANDLING:
--   MySQL UNIQUE permits MULTIPLE NULL values, which is intentional here:
--   PROMO_BONUS, SYSTEM_RECOVERY_RESET, and admin-topup paths legitimately
--   log rows with tx_id IS NULL.  They are not subject to dedup and are
--   unaffected.
--
-- SAFETY:
--   The migration first scans for existing duplicate (tx_id, source, user_id)
--   tuples and FAILS LOUDLY if any exist (SIGNAL SQLSTATE 45000).  It does
--   NOT auto-delete any rows — the operator must decide which row to keep
--   when reconciling pre-existing duplicates.  After cleanup, re-run the
--   migration.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Step 1: Pre-flight duplicate check.
--   If any (tx_id, source, user_id) tuple appears more than once, abort the
--   migration.  The SIGNAL surfaces a clear error to the operator instead of
--   silently skipping the ALTER (which would leave the bug unfixed).
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS vinplay.check_dup_money_gateway_log;

DELIMITER $$

CREATE PROCEDURE vinplay.check_dup_money_gateway_log()
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt FROM (
        SELECT tx_id, source, user_id
          FROM vinplay.money_gateway_log
         WHERE tx_id IS NOT NULL
         GROUP BY tx_id, source, user_id
        HAVING COUNT(*) > 1
    ) AS d;
    IF cnt > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'money_gateway_log has duplicate (tx_id, source, user_id) rows; manual cleanup required before adding UNIQUE constraint';
    END IF;
END$$

DELIMITER ;

CALL vinplay.check_dup_money_gateway_log();
DROP PROCEDURE vinplay.check_dup_money_gateway_log;

-- -----------------------------------------------------------------------------
-- Step 2: Drop the non-unique idx_tx_source and add UNIQUE uk_tx_source.
--   Renaming (rather than ALTER ... ADD UNIQUE) makes the constraint type
--   change explicit in subsequent SHOW CREATE TABLE output, and consistent
--   with the existing migration style of using `uk_*` prefix for unique keys.
--   The (tx_id, source) prefix is still useful for the isDuplicate SELECT,
--   which doesn't filter by user_id — leftmost-prefix index lookup applies.
-- -----------------------------------------------------------------------------
ALTER TABLE vinplay.money_gateway_log
    DROP INDEX idx_tx_source,
    ADD UNIQUE KEY uk_tx_source (tx_id, source, user_id);

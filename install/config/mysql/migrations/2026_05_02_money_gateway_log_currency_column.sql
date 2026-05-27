-- =============================================================================
-- money_gateway_log — add `currency` column + extend uk_tx_source UNIQUE to
-- include it.
--
-- WHY:
--   Phase 1 introduced MoneyGateway.{creditUser,debitUser,transferBetweenUsers}
--   for the `vin` currency only.  Phase 2 generalises the gateway to also cover
--   `xu`, `safe`, and `money_vp`.  A single deposit can legitimately credit
--   multiple currencies under the same external txId (e.g. a deposit credits
--   `vin` AND `money_vp`), so the existing 3-column UNIQUE
--   `uk_tx_source(tx_id, source, user_id)` would falsely reject the second
--   currency-side INSERT.
--
--   Adding `currency` to the UNIQUE key makes per-currency dedup independent
--   while still preventing the same `(tx_id, source, user_id, currency)` from
--   posting twice.
--
-- DEFAULT 'vin' FOR BC:
--   Every existing row was a vin-currency transaction.  Setting the default to
--   'vin' (and backfilling existing rows with the same value) preserves the
--   semantic of every legacy audit row.  New code passes the explicit currency.
--
-- NULL HANDLING:
--   MySQL UNIQUE permits MULTIPLE NULL values, so rows with `tx_id IS NULL`
--   (PROMO_BONUS, SYSTEM_RECOVERY_RESET, admin-topup paths) remain unaffected.
--   The new `currency` column is NOT NULL with a default, so it never goes NULL.
--
-- IDEMPOTENCY:
--   Wrapped in a procedure that checks information_schema first — safe to
--   re-run on a partially-applied DB.
-- =============================================================================

DROP PROCEDURE IF EXISTS vinplay.add_money_gateway_log_currency;

DELIMITER $$

CREATE PROCEDURE vinplay.add_money_gateway_log_currency()
BEGIN
    -- Step 1: Add the `currency` column if not present.
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = 'vinplay'
           AND TABLE_NAME   = 'money_gateway_log'
           AND COLUMN_NAME  = 'currency'
    ) THEN
        ALTER TABLE vinplay.money_gateway_log
            ADD COLUMN currency VARCHAR(16) NOT NULL DEFAULT 'vin' AFTER amount;
    END IF;

    -- Step 2: Replace uk_tx_source with the 4-column variant.  Drop the old
    --   3-column UNIQUE first (if present), then add the new one.  Wrapped in
    --   IF EXISTS / IF NOT EXISTS guards via information_schema for idempotency.
    IF EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = 'vinplay'
           AND TABLE_NAME   = 'money_gateway_log'
           AND INDEX_NAME   = 'uk_tx_source'
           AND NON_UNIQUE   = 0
           AND COLUMN_NAME  = 'user_id'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = 'vinplay'
           AND TABLE_NAME   = 'money_gateway_log'
           AND INDEX_NAME   = 'uk_tx_source'
           AND COLUMN_NAME  = 'currency'
    ) THEN
        ALTER TABLE vinplay.money_gateway_log
            DROP INDEX uk_tx_source,
            ADD UNIQUE KEY uk_tx_source (tx_id, source, user_id, currency);
    END IF;
END$$

DELIMITER ;

CALL vinplay.add_money_gateway_log_currency();
DROP PROCEDURE vinplay.add_money_gateway_log_currency;

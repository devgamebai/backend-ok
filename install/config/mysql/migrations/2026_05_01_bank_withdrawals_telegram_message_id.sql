-- SUN-1220: add telegram_message_id to bank_withdrawals so the Telegram
-- approve/reject callbacks can edit the original "pending" message
-- back to APPROVED / REJECTED state with buttons cleared (parity with
-- the deposit_transactions.telegram_message_id flow).
--
-- Also adds the missing column to crypto_withdrawals — code references
-- this column (WithdrawCryptoProcessor line 428) but the table doesn't
-- have it on staging, so silent SQL errors have been hiding crypto-side
-- Telegram edit failures.

ALTER TABLE vinplay.bank_withdrawals
  ADD COLUMN telegram_message_id BIGINT DEFAULT NULL AFTER processed_at;

ALTER TABLE vinplay.crypto_withdrawals
  ADD COLUMN telegram_message_id BIGINT DEFAULT NULL AFTER processed_at;

-- SUN-1171 follow-up — store the on-chain TRON tx_hash returned by the
-- gateway for completed crypto withdrawals.
--
-- Background: crypto_withdrawals.gateway_tx_id is the GUID the gateway
-- assigns to its own wallet_histories row at withdraw-creation time. It
-- has NO relationship to the eventual on-chain transaction hash.
--
-- For finance audit + customer support ("show me the blockchain
-- transaction for this withdraw"), we need the on-chain hash. The
-- gateway's ProcessWithdrawalToNetworkJob populates wallet_histories.tx_id
-- with the real on-chain hash when the on-chain transfer lands; the new
-- c=3027 NotifyCryptoWithdrawalProcessor relays that value into this
-- column on the backend side.
--
-- Metadata-only on InnoDB. Idempotent — re-running with the column
-- already present is a no-op.

USE `vinplay`;

DELIMITER $$
CREATE PROCEDURE AddCryptoWithdrawalsTxHash()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = 'vinplay' AND TABLE_NAME = 'crypto_withdrawals'
           AND COLUMN_NAME = 'tx_hash'
    ) THEN
        ALTER TABLE `crypto_withdrawals`
          ADD COLUMN `tx_hash` VARCHAR(255) NULL DEFAULT NULL
              COMMENT 'On-chain TRON transaction hash from the gateway after the USDT-TRC20 transfer is mined'
              AFTER `gateway_tx_id`,
          ADD KEY `idx_tx_hash` (`tx_hash`);
    END IF;
END$$
DELIMITER ;

CALL AddCryptoWithdrawalsTxHash();
DROP PROCEDURE AddCryptoWithdrawalsTxHash;

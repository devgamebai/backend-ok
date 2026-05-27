-- Migration: Add 2FA (TOTP) support for Admin accounts
-- Desc: Extends vinplay_admin.user and adds vinplay_admin.user_recovery_codes
-- JIRA: SEC-2FA

USE `vinplay_admin`;

-- 1. Extend user table with security enhancements idempotently
DELIMITER $$
CREATE PROCEDURE Add2FAColumns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='vinplay_admin' AND TABLE_NAME='user' AND COLUMN_NAME='GoogleAuthSecretEnc') THEN
        ALTER TABLE `user`
          ADD COLUMN `GoogleAuthSecretEnc` VARCHAR(255) NULL DEFAULT NULL
              COMMENT 'AES-GCM ciphertext của TOTP secret (base64), key trong .env TOTP_ENCRYPTION_KEY',
          ADD COLUMN `Is2FAEnabled` TINYINT(1) NOT NULL DEFAULT 0
              COMMENT '0: Disabled, 1: Enabled',
          ADD COLUMN `Last2FAWindow` BIGINT NULL DEFAULT NULL
              COMMENT 'TOTP window đã consume gần nhất, anti-replay',
          ADD COLUMN `TwoFAEnabledAt` DATETIME NULL
              COMMENT 'Lần đầu enable 2FA — audit',
          ADD COLUMN `TwoFAResetCount` INT NOT NULL DEFAULT 0
              COMMENT 'Số lần đã reset 2FA, audit / abuse detection';
    END IF;
END$$
DELIMITER ;

CALL Add2FAColumns();
DROP PROCEDURE Add2FAColumns;

-- 2. Recovery codes table (chống lockout)
CREATE TABLE IF NOT EXISTS `user_recovery_codes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `code_hash` VARCHAR(255) NOT NULL,
    `used_at` DATETIME NULL,
    `used_ip` VARCHAR(45) NULL,
    `created_at` DATETIME NOT NULL,
    KEY `idx_user_unused` (`user_id`, `used_at`),
    CONSTRAINT `fk_rec_codes_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

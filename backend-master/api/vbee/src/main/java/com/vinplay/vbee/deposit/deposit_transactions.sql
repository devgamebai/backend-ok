-- Migration: Create deposit_transactions table
-- Run against the vinplay database

CREATE TABLE IF NOT EXISTS `deposit_transactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tx_code` VARCHAR(50) NOT NULL,
    `user_id` INT NOT NULL,
    `nick_name` VARCHAR(50) NOT NULL,
    `amount` BIGINT NOT NULL DEFAULT 0,
    `bank_name` VARCHAR(100) DEFAULT NULL,
    `bank_number` VARCHAR(50) DEFAULT NULL,
    `status` ENUM('PENDING','PROCESSING','APPROVED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    `operator_name` VARCHAR(100) DEFAULT NULL,
    `operator_source` VARCHAR(20) DEFAULT NULL,
    `reject_reason` VARCHAR(255) DEFAULT NULL,
    `credit_status` ENUM('NONE','OK','FAILED') NOT NULL DEFAULT 'NONE',
    `force_approved` TINYINT(1) NOT NULL DEFAULT 0,
    `force_approved_by` VARCHAR(100) DEFAULT NULL,
    `force_approved_at` DATETIME DEFAULT NULL,
    `telegram_msg_id` BIGINT DEFAULT NULL,
    `telegram_chat_id` VARCHAR(50) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tx_code` (`tx_code`),
    KEY `idx_status` (`status`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_credit_status` (`credit_status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_telegram_msg` (`telegram_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migration: Add force_approved columns (for existing databases that lack these columns)
-- Date: 2026-03-25
-- NOTE: Only run these if the table already existed WITHOUT these columns.
--       If you just created the table above, these are already included and will produce harmless errors.
ALTER TABLE `deposit_transactions`
    ADD COLUMN `force_approved` TINYINT(1) NOT NULL DEFAULT 0 AFTER `credit_status`,
    ADD COLUMN `force_approved_by` VARCHAR(100) DEFAULT NULL AFTER `force_approved`,
    ADD COLUMN `force_approved_at` DATETIME DEFAULT NULL AFTER `force_approved_by`;

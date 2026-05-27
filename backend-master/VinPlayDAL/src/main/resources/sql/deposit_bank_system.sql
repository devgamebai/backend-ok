-- ============================================================
-- Migration: Company Banks table for Deposit System
-- Date: 2026-03-25
-- Purpose: Store platform's bank accounts for user deposits
-- ============================================================

-- Company bank accounts (where users transfer money to)
CREATE TABLE IF NOT EXISTS `company_banks` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `bank_name` VARCHAR(100) NOT NULL COMMENT 'Bank display name (e.g. 국민은행)',
  `bank_number` VARCHAR(50) NOT NULL COMMENT 'Account number',
  `account_holder` VARCHAR(100) NOT NULL COMMENT 'Account holder name',
  `code` VARCHAR(20) NOT NULL COMMENT 'Bank code (e.g. KB, SHINHAN)',
  `type` ENUM('DEPOSIT', 'WITHDRAW', 'BOTH') DEFAULT 'DEPOSIT' COMMENT 'What this account is used for',
  `status` TINYINT DEFAULT 1 COMMENT '1=active, 0=disabled',
  `priority` INT DEFAULT 0 COMMENT 'Lower = higher priority (for rotation)',
  `daily_limit` BIGINT DEFAULT 0 COMMENT 'Daily deposit limit (0=unlimited)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_status_type` (`status`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Platform bank accounts for deposit/withdrawal';

-- Dead Letter Queue tracking table (optional - for admin alerting)
CREATE TABLE IF NOT EXISTS `deposit_dlq_messages` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `tx_id` BIGINT COMMENT 'Transaction ID from the original message',
  `tx_code` VARCHAR(50),
  `message_body` TEXT COMMENT 'Serialized message content',
  `error_message` VARCHAR(500),
  `retry_count` INT DEFAULT 0,
  `status` ENUM('PENDING', 'RESOLVED', 'IGNORED') DEFAULT 'PENDING',
  `resolved_by` VARCHAR(100),
  `resolved_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_status` (`status`),
  INDEX `idx_tx_id` (`tx_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Deposit messages that failed after max retries';

-- ============================================================
-- Insert a default company bank (UPDATE WITH REAL DATA BEFORE LAUNCH)
-- ============================================================
-- INSERT INTO company_banks (bank_name, bank_number, account_holder, code, type, status, priority)
-- VALUES ('국민은행', '123-456-789012', 'SUNWIN CO LTD', 'KB', 'DEPOSIT', 1, 0);

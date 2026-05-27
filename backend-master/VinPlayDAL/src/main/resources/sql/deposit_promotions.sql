-- Migration: Create deposit promotion tables
-- Run against the vinplay database

-- 1. Promotion config table
CREATE TABLE IF NOT EXISTS `deposit_promotions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `promo_type` TINYINT NOT NULL COMMENT '1=First Deposit, 2=Daily Deposit',
    `bonus_percent` DECIMAL(5,2) NOT NULL DEFAULT 30.00 COMMENT '{A} Bonus %',
    `max_users` INT NOT NULL DEFAULT 0 COMMENT '{B} Max distinct users, 0=unlimited',
    `turnover_factor` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '{C} Wagering multiplier',
    `max_bonus_per_tx` BIGINT NOT NULL DEFAULT 150000 COMMENT '{D} Max bonus per transaction',
    `max_claims_daily` INT DEFAULT NULL COMMENT '{E} Max claims per day (Daily only)',
    `max_bonus_daily` BIGINT DEFAULT NULL COMMENT '{F} Max bonus amount per day (Daily only)',
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=Active, 0=Inactive',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`promo_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Promotion logs table (tracking each bonus claim)
CREATE TABLE IF NOT EXISTS `deposit_promotion_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `promo_id` BIGINT NOT NULL COMMENT 'FK -> deposit_promotions.id',
    `promo_type` TINYINT NOT NULL COMMENT 'Denormalized: 1=First, 2=Daily',
    `user_id` BIGINT NOT NULL,
    `nick_name` VARCHAR(128) NOT NULL,
    `deposit_tx_id` BIGINT NOT NULL COMMENT 'FK -> deposit_transactions.id',
    `deposit_amount` BIGINT NOT NULL COMMENT 'Original deposit amount',
    `bonus_amount` BIGINT NOT NULL COMMENT 'Actual bonus credited',
    `is_completed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=Turnover met, 0=Pending',
    `claim_date` DATE NOT NULL COMMENT 'Date of claim for daily limit check',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_promo_type` (`user_id`, `promo_type`),
    KEY `idx_user_date` (`user_id`, `claim_date`),
    KEY `idx_promo_id` (`promo_id`),
    KEY `idx_completed` (`user_id`, `is_completed`),
    KEY `idx_deposit_tx` (`deposit_tx_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

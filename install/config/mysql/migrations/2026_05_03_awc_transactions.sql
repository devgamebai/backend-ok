-- Migration: create awc_transactions table
-- Ticket: SUN-AWC-VOIDSETTLE
-- Used by AwcCallbackProcessor for idempotency checks and voidSettle clawback lookups.
-- bet_milli / win_milli store milli-VND values (integer × 1000) for sub-VND precision
-- needed by handleVoidSettle to reconstruct winLoss without rounding loss.

CREATE TABLE IF NOT EXISTS `awc_transactions` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `platform_tx_id`  VARCHAR(128) NOT NULL,
    `round_id`        VARCHAR(128)          DEFAULT NULL,
    `user_id`         BIGINT       NOT NULL DEFAULT 0,
    `user_name`       VARCHAR(64)  NOT NULL DEFAULT '',
    `platform`        VARCHAR(32)           DEFAULT NULL,
    `game_code`       VARCHAR(64)           DEFAULT NULL,
    `game_name`       VARCHAR(128)          DEFAULT NULL,
    `game_type`       VARCHAR(64)           DEFAULT NULL,
    `action`          VARCHAR(32)  NOT NULL,
    `bet_amount`      BIGINT       NOT NULL DEFAULT 0,
    `win_amount`      BIGINT       NOT NULL DEFAULT 0,
    `turnover`        BIGINT       NOT NULL DEFAULT 0,
    `adjust_amount`   BIGINT       NOT NULL DEFAULT 0,
    `tip_amount`      BIGINT       NOT NULL DEFAULT 0,
    -- milli-VND columns for sub-VND precision (used by voidSettle clawback)
    `bet_milli`       BIGINT       NOT NULL DEFAULT 0,
    `win_milli`       BIGINT       NOT NULL DEFAULT 0,
    `currency`        VARCHAR(8)            DEFAULT 'VND',
    `money_type`      TINYINT      NOT NULL DEFAULT 1,
    `bet_time`        VARCHAR(32)           DEFAULT NULL,
    `tx_time`         VARCHAR(32)           DEFAULT NULL,
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'success',
    `balance_after`   BIGINT       NOT NULL DEFAULT 0,
    `raw_json`        TEXT                  DEFAULT NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_platform_tx_id` (`platform_tx_id`),
    KEY `idx_action_platform_tx_id` (`action`, `platform_tx_id`),
    KEY `idx_user_name_created_at` (`user_name`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================================
-- Loss Rebate (Hoan Thua / Cashback) Module
-- Jira: SUN-257 to SUN-305
-- ================================================================

-- 1. Cashback program config (admin-managed)
CREATE TABLE IF NOT EXISTS tbl_cashback_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    program_name    VARCHAR(100) NOT NULL DEFAULT 'Default' COMMENT 'Program name',
    rebate_percent  DECIMAL(5,2) NOT NULL DEFAULT 5.00 COMMENT '% loss rebated',
    min_loss        BIGINT NOT NULL DEFAULT 0 COMMENT 'Min daily loss to qualify (0=no min)',
    min_rebate      BIGINT NOT NULL DEFAULT 0 COMMENT 'Min rebate amount (floor)',
    max_rebate      BIGINT NOT NULL DEFAULT 0 COMMENT 'Max rebate cap per day (0=no cap)',
    balance_threshold BIGINT NOT NULL DEFAULT 10000 COMMENT 'Player balance must be <= this to qualify',
    expiry_days     INT NOT NULL DEFAULT 7 COMMENT 'Days before unclaimed rebate expires',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    start_date      DATE DEFAULT NULL COMMENT 'Program start (NULL=no limit)',
    end_date        DATE DEFAULT NULL COMMENT 'Program end (NULL=no limit)',
    created_by      VARCHAR(64) DEFAULT NULL,
    updated_by      VARCHAR(64) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cashback (loss rebate) program config';

-- Seed default config
INSERT INTO tbl_cashback_config (program_name, rebate_percent, min_loss, max_rebate, is_active)
VALUES ('Default Loss Rebate', 5.00, 100000, 5000000, 1);

-- 2. Cashback log per player per day
CREATE TABLE IF NOT EXISTS tbl_cashback_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       INT NOT NULL COMMENT 'FK tbl_cashback_config.id',
    user_id         BIGINT NOT NULL,
    nick_name       VARCHAR(64) NOT NULL,
    calc_date       DATE NOT NULL COMMENT 'Date losses calculated for',
    total_deposit   BIGINT NOT NULL DEFAULT 0,
    total_withdraw  BIGINT NOT NULL DEFAULT 0,
    balance_at_calc BIGINT NOT NULL DEFAULT 0,
    net_loss        BIGINT NOT NULL DEFAULT 0,
    rebate_percent  DECIMAL(5,2) NOT NULL DEFAULT 0,
    rebate_amount   BIGINT NOT NULL DEFAULT 0,
    status          ENUM('PENDING','APPROVED','PAID','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    approved_by     VARCHAR(64) DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    paid_at         DATETIME DEFAULT NULL,
    expires_at      DATETIME NOT NULL,
    reject_reason   VARCHAR(500) DEFAULT NULL,
    note            TEXT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, calc_date),
    INDEX idx_status (status),
    INDEX idx_calc_date (calc_date),
    INDEX idx_expires (expires_at),
    INDEX idx_nick_name (nick_name),
    INDEX idx_config (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily loss rebate logs';

-- 3. Audit trail
CREATE TABLE IF NOT EXISTS tbl_cashback_changelog (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type     VARCHAR(20) NOT NULL COMMENT 'CONFIG or LOG',
    entity_id       BIGINT NOT NULL,
    action          VARCHAR(20) NOT NULL COMMENT 'CREATE, UPDATE, APPROVE, REJECT, PAY, EXPIRE',
    field_changed   VARCHAR(100) DEFAULT NULL,
    old_value       TEXT DEFAULT NULL,
    new_value       TEXT DEFAULT NULL,
    changed_by      VARCHAR(64) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit trail for cashback module';

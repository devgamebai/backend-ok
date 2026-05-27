-- =============================================================
-- Signing Bonus Feature (SUN-172)
-- Tracks first-time app installs and grants signup bonus
-- Supports auto payout + manual payout + per-user config
-- =============================================================

-- 1. Track device first-install
CREATE TABLE IF NOT EXISTS tbl_device_install (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_fingerprint  VARCHAR(255) NOT NULL   COMMENT 'Device ID from client (ANDROID_ID / identifierForVendor)',
    platform            VARCHAR(20)  DEFAULT 'unknown' COMMENT 'android | ios | web',
    app_version         VARCHAR(50)  DEFAULT NULL,
    ip_address          VARCHAR(50)  DEFAULT NULL,
    user_id             BIGINT       DEFAULT NULL COMMENT 'Linked after user registers',
    nick_name           VARCHAR(50)  DEFAULT NULL,
    first_open_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_fp (device_fingerprint),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Track device first-time install';

-- 2. Config signing bonus (admin managed, global)
CREATE TABLE IF NOT EXISTS tbl_signing_bonus_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    bonus_amount    BIGINT   NOT NULL DEFAULT 10000 COMMENT 'Default 10,000 won',
    status          TINYINT  NOT NULL DEFAULT 1     COMMENT '1=active, 0=inactive',
    wager_enabled   TINYINT  NOT NULL DEFAULT 1     COMMENT '1=apply wagering before withdrawal, 0=disable',
    wager_multiplier DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT 'Required volume = bonus_amount * wager_multiplier',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Signing bonus configuration';

-- Seed default config
INSERT INTO tbl_signing_bonus_config (bonus_amount, status, wager_enabled, wager_multiplier)
VALUES (10000, 1, 1, 3.00);

-- 3. Signing bonus log (anti-duplicate + audit)
CREATE TABLE IF NOT EXISTS tbl_signing_bonus_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    nick_name           VARCHAR(50)  NOT NULL,
    device_fingerprint  VARCHAR(255) NOT NULL,
    bonus_amount        BIGINT       NOT NULL,
    config_id           INT          NOT NULL COMMENT 'FK to tbl_signing_bonus_config.id (0 for manual payout)',
    status              TINYINT      NOT NULL DEFAULT 1 COMMENT '1=credited, 0=pending, 2=reversed',
    payout_mode         VARCHAR(20)  NOT NULL DEFAULT 'auto' COMMENT 'auto=auto payout on register, manual=admin approved',
    payout_by           VARCHAR(50)  DEFAULT NULL COMMENT 'Admin username who manually paid out',
    reason              VARCHAR(500) DEFAULT NULL COMMENT 'Manual payout reason/note',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_bonus (user_id),
    UNIQUE KEY uk_device_bonus (device_fingerprint),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Signing bonus claim logs';

-- 4. Per-user signing bonus config (admin can block/enable per user)
CREATE TABLE IF NOT EXISTS tbl_signing_bonus_user_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    nick_name       VARCHAR(50)  NOT NULL,
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '1=eligible for bonus, 0=blocked',
    payout_mode     VARCHAR(20)  NOT NULL DEFAULT 'auto' COMMENT 'auto=auto payout, manual=requires admin approval',
    reason          VARCHAR(500) DEFAULT NULL COMMENT 'Reason for blocking or setting manual mode',
    updated_by      VARCHAR(50)  DEFAULT NULL COMMENT 'Admin who last updated',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_nick_name (nick_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-user signing bonus configuration';

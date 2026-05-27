-- ================================================================
-- Module Hoan Cuoc (Rebate) - Multi-Level Schema
-- DB: vinplay (main game DB, same as user_volume_tracking)
-- ancestors format is COMMA-separated: "2,5" not "2.5"
-- IMPORTANT: Column names MUST match Java code in:
--   com.vinplay.dal.rebate.RebateService (DAL)
--   com.vinplay.api.backend.services.RebateService (Backend)
-- ================================================================
USE vinplay;

-- 1. Cau hinh % hoan cuoc theo dai ly
CREATE TABLE IF NOT EXISTS rebate_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_user_id INT NOT NULL COMMENT 'useragent.id (vinplay_admin)',
    agent_nickname VARCHAR(64) NOT NULL,
    agent_level TINYINT NOT NULL DEFAULT 1 COMMENT '1=TDL, 2=DL1, 3=DL2',
    rebate_percentage DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% hoan cuoc tong',
    share_percentage  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% chia cho tuyen duoi',
    period_type ENUM('DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'WEEKLY',
    min_volume_threshold BIGINT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent (agent_user_id),
    INDEX idx_active (is_active),
    INDEX idx_level (agent_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Log hoan cuoc theo ky
-- Column names match Java RebateService.createRebateLog() and mapLog()
CREATE TABLE IF NOT EXISTS rebate_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_user_id   INT NOT NULL,
    agent_nickname  VARCHAR(64) NOT NULL,
    agent_level     TINYINT NOT NULL DEFAULT 1,
    period_start    DATETIME NOT NULL,
    period_end      DATETIME NOT NULL,
    period_type     ENUM('DAILY','WEEKLY','MONTHLY') NOT NULL,

    -- Volume: Java uses "total_f1_volume"
    total_f1_volume BIGINT NOT NULL DEFAULT 0 COMMENT 'Total volume from downline users',

    -- Commission rates: Java uses "rebate_percentage" and "share_percentage"
    rebate_percentage DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% hoan cuoc cua agent nay',
    share_percentage  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% chia cho tuyen duoi',

    -- Differential fields (used by backend/services/RebateService.logRebate)
    own_percentage    DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% cua agent (alias of rebate_percentage)',
    child_percentage  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% cua tuyen duoi truc tiep',
    differential_pct  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '= own - child',

    -- Amounts: Java uses "rebate_amount", "share_amount", "net_rebate"
    rebate_amount   BIGINT NOT NULL DEFAULT 0 COMMENT 'volume x rebate%',
    share_amount    BIGINT NOT NULL DEFAULT 0 COMMENT 'phan chia cho tuyen duoi',
    net_rebate      BIGINT NOT NULL DEFAULT 0 COMMENT 'Agent thuc nhan = rebate - share',

    -- Status workflow: PENDING -> APPROVED -> PAID (or REJECTED/REVERSED)
    status ENUM('PENDING','APPROVED','PAID','REJECTED','REVERSED') NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(64) DEFAULT NULL,
    approved_at DATETIME DEFAULT NULL,
    note TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_agent_period (agent_user_id, period_start, period_end),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Lich su payout
CREATE TABLE IF NOT EXISTS rebate_payout (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rebate_log_id   BIGINT NOT NULL,
    agent_user_id   INT NOT NULL,
    agent_nickname  VARCHAR(64) NOT NULL,
    payout_amount   BIGINT NOT NULL,
    balance_before  BIGINT DEFAULT 0,
    balance_after   BIGINT DEFAULT 0,
    triggered_by    VARCHAR(64) NOT NULL,
    note TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent (agent_user_id),
    INDEX idx_log   (rebate_log_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Agency wallet table
CREATE TABLE IF NOT EXISTS agency_wallet (
    agent_id INT NOT NULL PRIMARY KEY COMMENT 'useragent.id',
    balance BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

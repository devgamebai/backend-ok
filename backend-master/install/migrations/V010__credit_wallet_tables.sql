-- V010__credit_wallet_tables.sql
-- Credit Wallet: ví mới cho Agent, tách biệt với agency_wallet
-- Created: 2026-04-13

CREATE TABLE IF NOT EXISTS vinplay.credit_wallet (
    agent_id        INT PRIMARY KEY,
    balance         BIGINT NOT NULL DEFAULT 0,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Credit wallet balance per agent';

CREATE TABLE IF NOT EXISTS vinplay.credit_wallet_transactions (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id         INT NOT NULL,
    agent_nickname   VARCHAR(128) NOT NULL,
    type             VARCHAR(32) NOT NULL COMMENT 'ADMIN_CREDIT|TRANSFER_OUT|TRANSFER_IN|DEPOSIT_TO_AGENT|DEPOSIT_TO_USER',
    amount           BIGINT NOT NULL COMMENT 'Always positive',
    direction        VARCHAR(8) NOT NULL COMMENT 'CREDIT or DEBIT',
    balance_after    BIGINT NOT NULL,
    related_user     VARCHAR(128) DEFAULT NULL COMMENT 'Nickname of receiver/sender',
    related_agent_id INT DEFAULT NULL COMMENT 'agent_id of receiver/sender if agent',
    note             VARCHAR(500) DEFAULT NULL,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_date (agent_id, created_at),
    INDEX idx_type (type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transaction log for credit_wallet';

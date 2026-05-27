-- ================================================================
-- Migration: Add commission_rate + indexes to useragent
-- DB: vinplay_admin
-- Required by: LoginAgentProcessor, AddNewUserAgentChildProcessor,
--              GetChilds4AgentProcessor, backend/services/RebateService
-- ================================================================
USE vinplay_admin;

-- 1. Add commission_rate column (Java code SELECTs and INSERTs this)
-- Dropped IF NOT EXISTS as it is not supported in MySQL 5.7 natively for ADD COLUMN
ALTER TABLE useragent
    ADD COLUMN commission_rate DECIMAL(5,2) DEFAULT 0
    COMMENT 'Commission rate for differential calculation'
    AFTER percent_bonus_vincard;

-- 2. Backfill: copy existing percent_bonus_vincard to commission_rate
-- so existing agents get their commission rate populated
UPDATE useragent
SET commission_rate = COALESCE(percent_bonus_vincard, 0)
WHERE commission_rate = 0 OR commission_rate IS NULL;

-- 3. Add indexes for hierarchy queries (spec Phase A.1)
-- ancestors index for FIND_IN_SET and LIKE queries
CREATE INDEX idx_ancestors ON useragent(ancestors(191));

-- code index for referral lookup
CREATE INDEX idx_code ON useragent(code);

-- level index for filtering by agent level
CREATE INDEX idx_level ON useragent(level);

-- 4. Add telegram/zalo columns if missing (Java code selects these)
ALTER TABLE useragent
    ADD COLUMN telegram VARCHAR(255) DEFAULT NULL AFTER ancestors,
    ADD COLUMN zalo VARCHAR(255) DEFAULT NULL AFTER telegram;

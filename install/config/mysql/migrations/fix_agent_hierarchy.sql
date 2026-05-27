-- Fix agent hierarchy for proper subtree visibility
-- See docs/AGENT_REFERRAL_CODE_SPEC.md + hierarchy analysis

USE vinplay_admin;

-- 1. Track old codes so referral_code → agent linkage survives code changes
CREATE TABLE IF NOT EXISTS agent_code_history (
  id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  agent_id   INT         NOT NULL,
  old_code   VARCHAR(80) NOT NULL,
  new_code   VARCHAR(80) NOT NULL,
  changed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_old_code (old_code),
  KEY idx_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Populate ancestors for existing agents
-- Agent id=1 (dfsdfsdfs): level NULL, parentid NULL → top-level, set level=0
UPDATE useragent SET level = 0, ancestors = '' WHERE id = 1 AND level IS NULL;
-- Agent id=2 (DaiLySo1SunWin): level=1, parentid=-1 → top-level TĐL, ancestors=''
UPDATE useragent SET ancestors = '' WHERE id = 2 AND (ancestors IS NULL OR ancestors = '');

-- For any future agents with parentid > 0, ancestors will be set by the processor.
-- Batch-fix script for existing deep trees (run once):
-- UPDATE useragent c SET c.ancestors = (
--   SELECT GROUP_CONCAT(p.id ORDER BY p.level) FROM useragent p
--   WHERE FIND_IN_SET(p.id, c.ancestors_temp) -- needs recursive CTE on MySQL 8
-- );
-- Skipped for now — only 2 agents exist on staging, both top-level.

USE vinplay;

-- 3. Add parent_agent_id column to users for reliable FK (supplement parrentUser string)
ALTER TABLE users ADD COLUMN parent_agent_id INT DEFAULT NULL AFTER parrentUser;
ALTER TABLE users ADD INDEX idx_parent_agent_id (parent_agent_id);

-- 4. Backfill parent_agent_id from referral_code → useragent.code
-- Also handle old codes via code history fallback
UPDATE users u
JOIN vinplay_admin.useragent ua
  ON u.referral_code COLLATE utf8mb3_general_ci = ua.code COLLATE utf8mb3_general_ci
SET u.parent_agent_id = ua.id
WHERE u.referral_code IS NOT NULL AND u.referral_code != '';

-- Also backfill from old code (321426 → agent id=2 who changed to VIP888)
-- Direct fix for known case:
UPDATE users SET parent_agent_id = 2
WHERE referral_code = '321426' AND parent_agent_id IS NULL;

-- ================================================================
-- Agency Enhancement Migration
-- Jira: SUN-765 — Agency hierarchy enhancement (SpecialAccount + CompanyAgent)
-- Created: 2026-04-10
-- Note: deposit_rate, wallet_balance, deposit_commission_logs
--       đã được tạo trong sql_deposit_commission.sql - KHÔNG duplicate ở đây.
--
-- ⚠️ OPS WARNING — Deploy Order:
--   1. FIRST: Merge code + build + deploy all 19 containers
--   2. THEN: Verify new code is live (test c=2000 creates user with parent_agent_id)
--   3. THEN: Run this migration during LOW TRAFFIC window
--   4. THEN: Run smoke tests (c=9838, c=9839, c=3 login)
--
-- ⚠️ PERFORMANCE WARNING:
--   Bước 4 runs a single UPDATE on ~12,833 rows in vinplay.users.
--   This holds a table-level lock for ~2-5 seconds.
--   Recommend running during maintenance/off-peak window.
--   For production >50k rows, consider batching with LIMIT loop.
--
-- ⚠️ REQUIRES PM SIGN-OFF:
--   After migration, 12,833 previously-orphan users start earning cashback
--   and generate DOWNLINE commission for CompanyAgent. Budget impact is non-zero.
--
-- Rollback: see agency_rollback_migration.sql
-- ================================================================
-- Bước 1: Tạo Special Account (level=0, code='0') trong users + useragent
-- Bước 2: Tạo Company Agent (level=1, code='1') trong users + useragent
-- Bước 3: Gán tất cả TĐL hiện tại về Special Account
-- Bước 4: Gán tất cả user mồ côi về Company Agent
-- ================================================================

-- ============================================================
-- BƯỚC 1: Tạo Special Account (level=0, code='0')
-- ============================================================

-- 1a. Tạo trong bảng users (game DB) — nếu chưa tồn tại
INSERT INTO vinplay.users (user_name, nick_name, password, vin, xu, dai_ly, status, create_time)
SELECT 'specialAccount', 'SpecialAccount', MD5('account@2026'), 0, 0, 1, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM vinplay.users WHERE user_name = 'specialAccount');

-- 1b. Tạo trong bảng useragent (admin DB) — nếu chưa tồn tại
INSERT INTO vinplay_admin.useragent (username, nickname, password, nameagent, level, code, status, active, `show`, parentid, ancestors, createtime, updatetime, commission_rate, deposit_rate, percent_bonus_vincard, `order`, login_times)
SELECT 'specialAccount', 'SpecialAccount', MD5('account@2026'), 'Special Account - Quản lý hệ thống Agency', 0, '0', '1', 1, 1, -1, '', NOW(), NOW(), 0, 0, 0, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM vinplay_admin.useragent WHERE code = '0');

SELECT '✅ Bước 1: Đã tạo Special Account (level=0, code=0)' AS status;

-- ============================================================
-- BƯỚC 2: Tạo Company Agent (level=1, code='1')
-- ============================================================

-- Lấy ID của Special Account vừa tạo
SET @special_account_id = (SELECT id FROM vinplay_admin.useragent WHERE code = '0' LIMIT 1);

-- 2a. Tạo trong bảng users (game DB)
INSERT INTO vinplay.users (user_name, nick_name, password, vin, xu, dai_ly, status, create_time, parent_agent_id, referral_code)
SELECT 'company-agent', 'CompanyAgent', MD5('agent@2026'), 0, 0, 1, 1, NOW(), @special_account_id, '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM vinplay.users WHERE user_name = 'company-agent');

-- Đảm bảo update nếu row đã tồn tại nhưng có parent_agent_id sai (do lần chạy lỗi trước)
UPDATE vinplay.users SET parent_agent_id = @special_account_id, referral_code = '0' WHERE user_name = 'company-agent';

-- 2b. Tạo trong bảng useragent (admin DB) — parentid trỏ về Special Account
INSERT INTO vinplay_admin.useragent (username, nickname, password, nameagent, level, code, status, active, `show`, parentid, ancestors, createtime, updatetime, commission_rate, deposit_rate, percent_bonus_vincard, `order`, login_times)
SELECT 'company-agent', 'CompanyAgent', MD5('agent@2026'), 'Tổng Đại Lý Công Ty - Mặc định', 1, '1', '1', 1, 1, @special_account_id, CAST(@special_account_id AS CHAR), NOW(), NOW(), 0, 0, 0, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM vinplay_admin.useragent WHERE code = '1');

SELECT '✅ Bước 2: Đã tạo Company Agent (level=1, code=1)' AS status;
SELECT CONCAT('   Special Account ID = ', @special_account_id) AS info;

-- ============================================================
-- BƯỚC 3: Gán tất cả TĐL hiện tại (level=1, parentid=-1) về Special Account
-- ============================================================

SET @company_agent_id = (SELECT id FROM vinplay_admin.useragent WHERE code = '1' LIMIT 1);

-- 3a. Gán TĐL trong bảng useragent
UPDATE vinplay_admin.useragent
SET parentid  = @special_account_id,
    updatetime = NOW()
WHERE level = 1
  AND (parentid IS NULL OR parentid = -1 OR parentid = 0)
  AND code NOT IN ('0', '1');   -- bỏ qua 2 account đặc biệt

-- 3b. Gán parent_agent_id của TĐL trong bảng user sang Special Account
UPDATE vinplay.users u
JOIN vinplay_admin.useragent ua ON u.nick_name COLLATE utf8mb3_general_ci = ua.nickname
SET u.parent_agent_id = @special_account_id,
    u.referral_code = '0'
WHERE ua.level = 1
  AND ua.code NOT IN ('0', '1');

SELECT '✅ Bước 3: Đã gán TĐL hiện tại (user + useragent) về Special Account' AS status;

-- ============================================================
-- BƯỚC 4: Gán user mồ côi về Company Agent
-- ============================================================

-- Lúc này các TĐL đã có parent_agent_id = @special_account_id (nhờ 3b), 
-- nên câu query này sẽ chỉ dọn dẹp đúng những user nào thực sự mồ côi (chủ yếu là player)
UPDATE vinplay.users
SET parent_agent_id = @company_agent_id,
    referral_code   = '1'
WHERE (parent_agent_id IS NULL OR parent_agent_id = 0)
  AND user_name NOT IN ('specialAccount', 'company-agent');

SELECT CONCAT('✅ Bước 4: Đã gán ', ROW_COUNT(), ' user mồ côi về Company Agent (id=', @company_agent_id, ')') AS status;

-- ============================================================
-- VERIFICATION
-- ============================================================
SELECT '========== KẾT QUẢ SAU MIGRATION ==========' AS section;

SELECT id, username, nickname, level, code, parentid, ancestors
FROM vinplay_admin.useragent
WHERE level IN (0, 1)
ORDER BY level, id;

SELECT
    COUNT(*) AS total_users,
    SUM(CASE WHEN parent_agent_id IS NOT NULL AND parent_agent_id > 0 THEN 1 ELSE 0 END) AS has_agent,
    SUM(CASE WHEN parent_agent_id IS NULL OR parent_agent_id = 0 THEN 1 ELSE 0 END) AS orphan
FROM vinplay.users;

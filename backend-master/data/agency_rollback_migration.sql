-- ================================================================
-- Agency Enhancement — ROLLBACK Migration
-- Dùng khi cần revert về trạng thái trước migration
--
-- ⚠️ WARNING:
--   1. Chạy SAU KHI đã backup: mysqldump vinplay users > pre_rollback_users.sql
--                               mysqldump vinplay_admin useragent --routines > pre_rollback_useragent.sql
--   2. Script này revert CẢ data VÀ triggers.
--   3. Sau rollback, path_ancestors sẽ được maintained lại bởi old triggers.
-- ================================================================

-- Bước 1: Revert user mồ côi — xóa referral_code và parent_agent_id
-- Chỉ revert user có referral_code='1' VÀ parent_agent_id = Company Agent
SET @company_agent_id = (SELECT id FROM vinplay_admin.useragent WHERE code = '1' LIMIT 1);

UPDATE vinplay.users
SET parent_agent_id = NULL,
    referral_code = NULL
WHERE referral_code = '1'
  AND parent_agent_id = @company_agent_id
  AND user_name NOT IN ('specialAccount', 'company-agent');

SELECT CONCAT('✅ Bước 1: Reverted ', ROW_COUNT(), ' user về orphan') AS status;

-- Bước 2: Revert TĐL — set parentid = -1 (trạng thái cũ)
SET @special_account_id = (SELECT id FROM vinplay_admin.useragent WHERE code = '0' LIMIT 1);

-- 2a. Revert parentid trong bảng useragent
UPDATE vinplay_admin.useragent
SET parentid = -1,
    updatetime = NOW()
WHERE parentid = @special_account_id
  AND code NOT IN ('0', '1');

-- 2b. Revert parent_agent_id trong bảng users
UPDATE vinplay.users u
JOIN vinplay_admin.useragent ua ON u.nick_name COLLATE utf8mb3_general_ci = ua.nickname
SET u.parent_agent_id = NULL,
    u.referral_code = NULL
WHERE ua.level = 1
  AND u.parent_agent_id = @special_account_id
  AND ua.code NOT IN ('0', '1');

SELECT CONCAT('✅ Bước 2: Reverted ', ROW_COUNT(), ' TĐL về parentid=-1') AS status;

-- Bước 3: Xóa Company Agent + Special Account từ useragent
DELETE FROM vinplay_admin.useragent WHERE code IN ('0', '1');
SELECT CONCAT('✅ Bước 3: Xóa SA + CA từ useragent (', ROW_COUNT(), ' rows)') AS status;

-- Bước 4: Xóa từ users
DELETE FROM vinplay.users WHERE user_name IN ('specialAccount', 'company-agent');
SELECT CONCAT('✅ Bước 4: Xóa SA + CA từ users (', ROW_COUNT(), ' rows)') AS status;

-- ================================================================
-- Bước 5: Restore old triggers (trước khi có SpecialAccount)
-- Old triggers maintain CẢ ancestors VÀ path_ancestors.
-- ================================================================

DROP TRIGGER IF EXISTS tg_before_useragent_insert;
DROP TRIGGER IF EXISTS tg_before_useragent_update;

DELIMITER //

-- OLD INSERT TRIGGER (pre-SUN-765)
CREATE TRIGGER tg_before_useragent_insert BEFORE INSERT ON useragent
FOR EACH ROW
BEGIN
    DECLARE p_ancestors VARCHAR(255);
    DECLARE p_path VARCHAR(255);

    IF NEW.parentid IS NOT NULL AND NEW.parentid > 0 THEN
        SELECT IFNULL(ancestors,''), IFNULL(path_ancestors,'/')
          INTO p_ancestors, p_path
          FROM useragent WHERE id = NEW.parentid;

        IF p_ancestors = '' THEN
            SET NEW.ancestors = CAST(NEW.parentid AS CHAR);
        ELSE
            SET NEW.ancestors = CONCAT(p_ancestors, ',', NEW.parentid);
        END IF;
        SET NEW.path_ancestors = CONCAT(p_path, NEW.parentid, '/');
        SET NEW.level = LENGTH(NEW.path_ancestors) - LENGTH(REPLACE(NEW.path_ancestors, '/', ''));
    ELSE
        SET NEW.ancestors = '';
        SET NEW.path_ancestors = '/';
        SET NEW.level = 1;
    END IF;
END//

-- OLD UPDATE TRIGGER (pre-SUN-765)
CREATE TRIGGER tg_before_useragent_update BEFORE UPDATE ON useragent
FOR EACH ROW
BEGIN
    DECLARE p_ancestors VARCHAR(255);
    DECLARE p_path VARCHAR(255);

    IF NEW.parentid IS NOT NULL AND NEW.parentid > 0 THEN
        SELECT IFNULL(ancestors,''), IFNULL(path_ancestors,'/')
          INTO p_ancestors, p_path
          FROM useragent WHERE id = NEW.parentid;

        IF p_ancestors = '' THEN
            SET NEW.ancestors = CAST(NEW.parentid AS CHAR);
        ELSE
            SET NEW.ancestors = CONCAT(p_ancestors, ',', NEW.parentid);
        END IF;
        SET NEW.path_ancestors = CONCAT(p_path, NEW.parentid, '/');
        SET NEW.level = LENGTH(NEW.path_ancestors) - LENGTH(REPLACE(NEW.path_ancestors, '/', ''));
    ELSE
        SET NEW.ancestors = '';
        SET NEW.path_ancestors = '/';
        SET NEW.level = 1;
    END IF;
END//

DELIMITER ;

SELECT '✅ Bước 5: Restored old triggers (path_ancestors + ancestors)' AS status;

-- ================================================================
-- Bước 6: Re-trigger all agents to rebuild ancestors + path_ancestors
-- ================================================================
UPDATE vinplay_admin.useragent SET parentid = parentid WHERE parentid > 0;
UPDATE vinplay_admin.useragent SET parentid = parentid WHERE parentid = -1;

SELECT '✅ Bước 6: Re-triggered all agents to rebuild hierarchy' AS status;

-- ================================================================
-- VERIFICATION
-- ================================================================
SELECT '========== VERIFY ROLLBACK ==========' AS section;
SELECT COUNT(*) AS total, SUM(CASE WHEN parent_agent_id IS NULL OR parent_agent_id = 0 THEN 1 ELSE 0 END) AS orphan FROM vinplay.users;
SELECT * FROM vinplay_admin.useragent WHERE code IN ('0','1');
SELECT id, nickname, level, parentid, ancestors, path_ancestors FROM vinplay_admin.useragent ORDER BY level, id LIMIT 10;

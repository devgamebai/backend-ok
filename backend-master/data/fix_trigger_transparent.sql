-- Fix trigger: Special Account (code='0') là "trong suốt" (transparent)
-- Không đếm vào depth khi tính level
-- TĐL vẫn giữ level=1, ĐL1=2, ĐL2=3

DROP TRIGGER IF EXISTS tg_before_useragent_update;
DROP TRIGGER IF EXISTS tg_before_useragent_insert;

DELIMITER //

-- INSERT TRIGGER
CREATE TRIGGER tg_before_useragent_insert BEFORE INSERT ON useragent
FOR EACH ROW
BEGIN
    DECLARE p1 VARCHAR(255);
    DECLARE p2 VARCHAR(255);
    DECLARE special_id INT;

    -- Special Account (code='0') → force level=0, no ancestors
    IF NEW.code = '0' THEN
        SET NEW.level = 0;
        SET NEW.ancestors = '';
    ELSE
        SET p1 := '';
        IF NEW.parentid > -1 THEN
            SELECT IFNULL(ancestors,'') INTO p1 FROM useragent WHERE id = NEW.parentid;
        ELSE
            SET p1 := '';
        END IF;

        SET p2 := '';
        IF p1 = '' THEN
            IF NEW.parentid = -1 THEN
                SET p2 := '';
            ELSE
                SET p2 := NEW.parentid;
            END IF;
        ELSE
            SELECT CONCAT(p1, ',', NEW.parentid) INTO p2;
        END IF;

        -- Tính level nhưng trừ đi Special Account (nếu có trong ancestors)
        SET special_id = NULL;
        SELECT id INTO special_id FROM useragent WHERE code = '0' LIMIT 1;

        IF special_id IS NOT NULL AND FIND_IN_SET(special_id, p2) > 0 THEN
            -- Có Special Account trong ancestors → level = depth - 1 (bỏ qua nó)
            SET NEW.level = FIND_IN_SET(NEW.parentid, p2);
        ELSE
            SET NEW.level = FIND_IN_SET(NEW.parentid, p2) + 1;
        END IF;
        SET NEW.ancestors = p2;
    END IF;
END//

-- UPDATE TRIGGER
CREATE TRIGGER tg_before_useragent_update BEFORE UPDATE ON useragent
FOR EACH ROW
BEGIN
    DECLARE p1 VARCHAR(255);
    DECLARE p2 VARCHAR(255);
    DECLARE special_id INT;

    -- Special Account (code='0') → force level=0, no ancestors
    IF OLD.code = '0' THEN
        SET NEW.level = 0;
        SET NEW.ancestors = '';
    ELSE
        SET p1 := '';
        IF NEW.parentid > -1 THEN
            SELECT IFNULL(ancestors,'') INTO p1 FROM useragent WHERE id = NEW.parentid;
        ELSE
            SET p1 := '';
        END IF;

        SET p2 := '';
        IF p1 = '' THEN
            IF NEW.parentid = -1 THEN
                SET p2 := '';
            ELSE
                SET p2 := NEW.parentid;
            END IF;
        ELSE
            SELECT CONCAT(p1, ',', NEW.parentid) INTO p2;
        END IF;

        -- Tính level nhưng trừ đi Special Account (nếu có trong ancestors)
        SET special_id = NULL;
        SELECT id INTO special_id FROM useragent WHERE code = '0' LIMIT 1;

        IF special_id IS NOT NULL AND FIND_IN_SET(special_id, p2) > 0 THEN
            SET NEW.level = FIND_IN_SET(NEW.parentid, p2);
        ELSE
            SET NEW.level = FIND_IN_SET(NEW.parentid, p2) + 1;
        END IF;
        SET NEW.ancestors = p2;
    END IF;
END//
DELIMITER ;

-- Giờ re-trigger lại tất cả agent cũ bằng cách update parentid = parentid (trigger sẽ chạy lại)
-- Company Agent (code='1')
UPDATE useragent SET parentid = parentid WHERE code = '1';

-- TĐL cũ (parentid = Special Account)
SET @special_id = (SELECT id FROM useragent WHERE code = '0' LIMIT 1);
UPDATE useragent SET parentid = parentid WHERE parentid = @special_id AND code != '1' AND code != '0';

-- ĐL1 (level hiện tại = 3)
UPDATE useragent SET parentid = parentid WHERE level > 2;

-- ĐL2
UPDATE useragent SET parentid = parentid WHERE level > 3;

-- Verify
SELECT id, username, nickname, level, code, parentid, ancestors FROM useragent ORDER BY level, id;

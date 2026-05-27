-- SUN-XXXX — Hierarchy invariant for game_commission_rate.
--
-- Rule: an agent's per-game rate must never exceed its direct parent's
-- per-game rate for the same game_key. If parent has no entry for a
-- game_key (effective rate = 0), child rate must be 0 for that game.
-- Enforced at the DB layer so admin tooling, hot fixes, and direct
-- writes all share one truth.
--
-- Three triggers:
--   1. BEFORE INSERT — block creating a child row that exceeds parent
--   2. BEFORE UPDATE on child — same check on the new value
--   3. BEFORE UPDATE on parent — block lowering parent rate below any
--      existing descendant's rate for same game_key
--
-- Platform root (vinplay_admin.useragent.parentid = -1, level = 0) is
-- exempt from check #1/#2 because there is no upstream master to bound
-- it; the platform's rate IS the upper bound for everyone below.
--
-- Existing violating rows are NOT auto-fixed; they're left in place
-- and surfaced via SUN-XXXX cleanup. Triggers only fire on new writes.

DROP TRIGGER IF EXISTS vinplay.tg_gcr_guard_ins;
DROP TRIGGER IF EXISTS vinplay.tg_gcr_guard_upd;

DELIMITER $$

CREATE TRIGGER vinplay.tg_gcr_guard_ins
BEFORE INSERT ON vinplay.game_commission_rate
FOR EACH ROW
BEGIN
    DECLARE v_parent_id INT;
    DECLARE v_parent_level TINYINT;
    DECLARE v_parent_rate DECIMAL(5,2);
    DECLARE v_max_descendant_rate DECIMAL(5,2);
    DECLARE v_msg VARCHAR(500);

    -- Skip zero rates (no commission to validate against parent).
    IF NEW.rate > 0 THEN
        SELECT u.parentid, u.level INTO v_parent_id, v_parent_level
        FROM vinplay_admin.useragent u
        WHERE u.id = NEW.agent_user_id;

        -- Only enforce when parent is a real agent (not platform root or missing).
        IF v_parent_id IS NOT NULL AND v_parent_id > 0 THEN
            SELECT IFNULL(MAX(rate), 0) INTO v_parent_rate
            FROM vinplay.game_commission_rate
            WHERE agent_user_id = v_parent_id AND game_key = NEW.game_key;

            IF NEW.rate > v_parent_rate THEN
                SET v_msg = CONCAT(
                    'gcr_guard: agent_id=', NEW.agent_user_id,
                    ' rate=', NEW.rate,
                    ' game_key=', NEW.game_key,
                    ' exceeds parent_id=', v_parent_id,
                    ' rate=', v_parent_rate);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;
        END IF;
    END IF;
END$$

CREATE TRIGGER vinplay.tg_gcr_guard_upd
BEFORE UPDATE ON vinplay.game_commission_rate
FOR EACH ROW
BEGIN
    DECLARE v_parent_id INT;
    DECLARE v_parent_level TINYINT;
    DECLARE v_parent_rate DECIMAL(5,2);
    DECLARE v_max_descendant_rate DECIMAL(5,2);
    DECLARE v_msg VARCHAR(500);

    -- Check 1: child rate not above parent rate (skip if rate unchanged or zero).
    IF NEW.rate > 0 AND NEW.rate <> OLD.rate THEN
        SELECT u.parentid, u.level INTO v_parent_id, v_parent_level
        FROM vinplay_admin.useragent u
        WHERE u.id = NEW.agent_user_id;

        IF v_parent_id IS NOT NULL AND v_parent_id > 0 THEN
            SELECT IFNULL(MAX(rate), 0) INTO v_parent_rate
            FROM vinplay.game_commission_rate
            WHERE agent_user_id = v_parent_id AND game_key = NEW.game_key;

            IF NEW.rate > v_parent_rate THEN
                SET v_msg = CONCAT(
                    'gcr_guard: agent_id=', NEW.agent_user_id,
                    ' rate=', NEW.rate,
                    ' game_key=', NEW.game_key,
                    ' exceeds parent_id=', v_parent_id,
                    ' rate=', v_parent_rate);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;
        END IF;
    END IF;

    -- Check 2: parent lowering rate below an existing descendant's rate.
    -- Only when rate is decreasing.
    IF NEW.rate < OLD.rate THEN
        SELECT IFNULL(MAX(cr.rate), 0) INTO v_max_descendant_rate
        FROM vinplay.game_commission_rate cr
        JOIN vinplay_admin.useragent u ON u.id = cr.agent_user_id
        WHERE cr.game_key = NEW.game_key
          AND FIND_IN_SET(NEW.agent_user_id, IFNULL(u.ancestors, '')) > 0;

        IF v_max_descendant_rate > NEW.rate THEN
            SET v_msg = CONCAT(
                'gcr_guard: cannot lower agent_id=', NEW.agent_user_id,
                ' game_key=', NEW.game_key,
                ' to rate=', NEW.rate,
                ' (descendant has rate=', v_max_descendant_rate, ')');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    END IF;
END$$

DELIMITER ;

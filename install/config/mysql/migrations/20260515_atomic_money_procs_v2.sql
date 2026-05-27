-- =============================================================================
-- 20260515_atomic_money_procs_v2.sql
--
-- Follow-up to 20260515_atomic_money_procs.sql.
--
-- The first version used a CAS retry loop with `WHERE vin = v_curr_vin AND
-- vin_total = v_curr_vin_total`. Smoke test surfaced a regression: MySQL's
-- ROW_COUNT() returns 0 for a no-op UPDATE (when the new value equals the
-- old, like p_money=0). The retry loop interpreted that as a lost race and
-- SIGNALed after exhausting 5 attempts. Real production calls always have
-- non-zero delta so this would not have fired under load, but it's brittle.
--
-- V2 swaps to **SELECT … FOR UPDATE** pessimistic locking:
--
--   START TRANSACTION;
--   SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
--     FROM users WHERE id = p_user_id FOR UPDATE;   -- acquires row lock
--   UPDATE users SET vin = vin + p_money, vin_total = vin_total + p_money
--     WHERE id = p_user_id;                          -- safe under lock
--   COMMIT;                                          -- releases lock
--
-- Why this is race-safe:
--   - InnoDB's `SELECT … FOR UPDATE` takes an exclusive row lock that
--     blocks every other writer (including the MoneyGateway atomic UPDATE
--     path) on the same row until COMMIT.
--   - Concurrent calls serialize on the same user_id row, applying their
--     deltas in commit-order. No silent stomp possible.
--   - No CAS retry loop needed; eliminates the no-op edge case.
--
-- Lock contention is the only downside, and it's the correct trade-off:
--   - Same-user concurrent writes WANT to serialize (race-stomp is the bug
--     we're fixing).
--   - Different-user writes don't contend (different row locks).
--   - Typical lock-hold time is <1ms (one UPDATE + a few inserts).
--
-- Drift detection + money_gateway_log audit row preserved from V1.
-- =============================================================================

-- ─── 1. update_money_user ────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_user;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_user`(
    IN p_user_id     INT(11),
    IN p_money       BIGINT(20),
    IN p_money_use   BIGINT(20),
    IN p_money_total BIGINT(20),
    IN p_money_type  VARCHAR(5),
    IN p_fee         BIGINT(20),
    IN p_action_name NVARCHAR(45),
    IN p_money_vp    INT(11),
    IN p_vp          INT(11),
    IN p_type        INT(11))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_user_exists    TINYINT DEFAULT 0;
    DECLARE v_new_vin        BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_type = 'vin' THEN
        SELECT 1, vin, vin_total INTO v_user_exists, v_curr_vin, v_curr_vin_total
          FROM users WHERE id = p_user_id FOR UPDATE;
    ELSE
        SELECT 1, xu, xu_total INTO v_user_exists, v_curr_xu, v_curr_xu_total
          FROM users WHERE id = p_user_id FOR UPDATE;
    END IF;

    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_user: user not found';
    END IF;

    IF p_money_type = 'vin' THEN
        IF p_type = 2 THEN
            IF p_vp >= 0 THEN
                UPDATE users
                   SET vin            = vin + p_money,
                       vin_total      = vin_total + p_money,
                       vip_point      = vip_point + p_vp,
                       vip_point_save = vip_point_save + p_vp,
                       money_vp       = p_money_vp
                 WHERE id = p_user_id;
            ELSE
                UPDATE users
                   SET vin       = vin + p_money,
                       vin_total = vin_total + p_money,
                       vip_point = 0
                 WHERE id = p_user_id;
            END IF;
        ELSEIF p_type = 1 THEN
            UPDATE users
               SET vin            = vin + p_money,
                   vin_total      = vin_total + p_money,
                   recharge_money = recharge_money + p_money,
                   nap_times      = nap_times + 1,
                   t_nap          = t_nap + p_money
             WHERE id = p_user_id;
        ELSEIF p_type = 0 THEN
            IF p_action_name = 'REQUEST_CASHOUT' THEN
                UPDATE users
                   SET vin       = vin + p_money,
                       vin_total = vin_total + p_money,
                       t_rut     = t_rut + p_money,
                       rut_times = rut_times + 1
                 WHERE id = p_user_id;
            ELSEIF p_action_name = 'REFUND_RECHARGE' THEN
                UPDATE users
                   SET vin       = vin + p_money,
                       vin_total = vin_total + p_money,
                       t_rut     = t_rut - p_money,
                       rut_times = rut_times - 1
                 WHERE id = p_user_id;
            ELSE
                UPDATE users
                   SET vin       = vin + p_money,
                       vin_total = vin_total + p_money
                 WHERE id = p_user_id;
            END IF;
        ELSEIF p_type = 3 THEN
            UPDATE users
               SET vin       = vin + p_money,
                   vin_total = vin_total + p_money
             WHERE id = p_user_id;
        END IF;
        SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;
    ELSE
        UPDATE users
           SET xu       = xu + p_money,
               xu_total = xu_total + p_money
         WHERE id = p_user_id;
        SELECT xu INTO v_new_vin FROM users WHERE id = p_user_id;
    END IF;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, p_money_type,
         CONCAT('PROC_UPDATE_MONEY_USER_T', p_type),
         CONCAT('proc-umu-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         IFNULL(p_action_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_user', p_user_id, p_money_use, v_new_vin, p_money_total, NULL, p_money, p_action_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 2. update_money_in_game (11 params) ─────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_in_game;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_in_game`(
    IN p_session_id    NVARCHAR(100),
    IN p_user_id       INT(11),
    IN p_game_name     NVARCHAR(45),
    IN p_money_use     BIGINT(20),
    IN p_money_total   BIGINT(20),
    IN p_money_freeze  BIGINT(20),
    IN p_money_type    VARCHAR(5),
    IN p_fee           BIGINT(20),
    IN p_money_vp      INT(11),
    IN p_vp            INT(11),
    IN p_money         BIGINT(20))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_user_exists    TINYINT DEFAULT 0;
    DECLARE v_new_vin        BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_type = 'vin' THEN
        SELECT 1, vin, vin_total INTO v_user_exists, v_curr_vin, v_curr_vin_total
          FROM users WHERE id = p_user_id FOR UPDATE;
        IF v_user_exists = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_in_game: user not found';
        END IF;
        UPDATE users
           SET vin            = vin + p_money,
               vin_total      = vin_total + p_money,
               vip_point      = vip_point + p_vp,
               vip_point_save = vip_point_save + p_vp,
               money_vp       = p_money_vp
         WHERE id = p_user_id;
        SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;
    ELSE
        SELECT 1, xu, xu_total INTO v_user_exists, v_curr_xu, v_curr_xu_total
          FROM users WHERE id = p_user_id FOR UPDATE;
        IF v_user_exists = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_in_game: user not found';
        END IF;
        UPDATE users
           SET xu       = xu + p_money,
               xu_total = xu_total + p_money
         WHERE id = p_user_id;
        SELECT xu INTO v_new_vin FROM users WHERE id = p_user_id;
    END IF;

    IF p_money_freeze > -1 THEN
        UPDATE freeze_money SET money = p_money_freeze
         WHERE session_id = p_session_id AND user_id = p_user_id AND `status` = 1;
    END IF;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, p_money_type,
         'PROC_UPDATE_MONEY_IN_GAME',
         CONCAT('proc-umig-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         IFNULL(p_game_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_in_game', p_user_id, p_money_use, v_new_vin, p_money_total, NULL, p_money, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 3. restore_money (uses p_money_exchange as positive delta) ──────────────
DROP PROCEDURE IF EXISTS restore_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `restore_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_money_use      BIGINT(20),
    IN p_money_total    BIGINT(20),
    IN p_money_exchange BIGINT(20),
    IN p_money_type     VARCHAR(5))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_user_exists    TINYINT DEFAULT 0;
    DECLARE v_new_vin        BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_exchange > 0 THEN
        IF p_money_type = 'vin' THEN
            SELECT 1, vin, vin_total INTO v_user_exists, v_curr_vin, v_curr_vin_total
              FROM users WHERE id = p_user_id FOR UPDATE;
            IF v_user_exists = 0 THEN
                ROLLBACK;
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'restore_money: user not found';
            END IF;
            UPDATE users
               SET vin       = vin + p_money_exchange,
                   vin_total = vin_total + p_money_exchange
             WHERE id = p_user_id;
            SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;
        ELSE
            SELECT 1, xu, xu_total INTO v_user_exists, v_curr_xu, v_curr_xu_total
              FROM users WHERE id = p_user_id FOR UPDATE;
            IF v_user_exists = 0 THEN
                ROLLBACK;
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'restore_money: user not found';
            END IF;
            UPDATE users
               SET xu       = xu + p_money_exchange,
                   xu_total = xu_total + p_money_exchange
             WHERE id = p_user_id;
            SELECT xu INTO v_new_vin FROM users WHERE id = p_user_id;
        END IF;

        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money_exchange, p_money_type,
             'PROC_RESTORE_MONEY',
             CONCAT('proc-rm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
             IFNULL(p_session_id, ''), v_new_vin, NOW());

        IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_new_vin != p_money_use THEN
            INSERT INTO money_proc_drift
                (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
            VALUES
                ('restore_money', p_user_id, p_money_use, v_new_vin, p_money_total, NULL, p_money_exchange, p_session_id, 0);
        END IF;
    END IF;

    UPDATE freeze_money SET money = 0, `status` = 0
     WHERE session_id = p_session_id AND user_id = p_user_id;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 4. safe_money (5 params; p_money signed delta on vin) ───────────────────
DROP PROCEDURE IF EXISTS safe_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `safe_money`(
    IN p_user_id     INT(11),
    IN p_money_use   BIGINT(20),
    IN p_money_total BIGINT(20),
    IN p_money_safe  BIGINT(20),
    IN p_money       BIGINT(20))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_user_exists    TINYINT DEFAULT 0;
    DECLARE v_new_vin        BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin, vin_total INTO v_user_exists, v_curr_vin, v_curr_vin_total
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'safe_money: user not found';
    END IF;

    UPDATE users
       SET vin       = vin + p_money,
           vin_total = vin_total + p_money,
           safe      = p_money_safe
     WHERE id = p_user_id;

    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         'PROC_SAFE_MONEY',
         CONCAT('proc-sm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         'safe-box transfer', v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('safe_money', p_user_id, p_money_use, v_new_vin, p_money_total, NULL, p_money, 'safe', 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 5. freeze_money (debit via p_money_exchange, with floor check) ──────────
DROP PROCEDURE IF EXISTS freeze_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `freeze_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_game_name      NVARCHAR(45),
    IN p_room_id        NVARCHAR(100),
    IN p_money_use      BIGINT(20),
    IN p_money_total    BIGINT(20),
    IN p_money_exchange BIGINT(20),
    IN p_money_type     VARCHAR(5),
    IN p_nick_name      NVARCHAR(45))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_user_exists    TINYINT DEFAULT 0;
    DECLARE v_new_vin        BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_type = 'vin' THEN
        SELECT 1, vin, vin_total INTO v_user_exists, v_curr_vin, v_curr_vin_total
          FROM users WHERE id = p_user_id FOR UPDATE;
        IF v_user_exists = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: user not found';
        END IF;
        IF v_curr_vin < p_money_exchange THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: insufficient balance';
        END IF;
        UPDATE users
           SET vin       = vin - p_money_exchange,
               vin_total = vin_total - p_money_exchange
         WHERE id = p_user_id;
        SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;
    ELSE
        SELECT 1, xu, xu_total INTO v_user_exists, v_curr_xu, v_curr_xu_total
          FROM users WHERE id = p_user_id FOR UPDATE;
        IF v_user_exists = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: user not found';
        END IF;
        IF v_curr_xu < p_money_exchange THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: insufficient balance';
        END IF;
        UPDATE users
           SET xu       = xu - p_money_exchange,
               xu_total = xu_total - p_money_exchange
         WHERE id = p_user_id;
        SELECT xu INTO v_new_vin FROM users WHERE id = p_user_id;
    END IF;

    INSERT INTO freeze_money
        (session_id, user_id, game_name, room_id, money, money_type, create_time, status, nick_name)
    VALUES
        (p_session_id, p_user_id, p_game_name, p_room_id, p_money_exchange, p_money_type, NOW(), 1, p_nick_name);

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, IFNULL(p_nick_name, ''), -p_money_exchange, p_money_type,
         'PROC_FREEZE_MONEY',
         CONCAT('proc-fm-', p_user_id, '-', p_session_id),
         IFNULL(p_game_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('freeze_money', p_user_id, p_money_use, v_new_vin, p_money_total, NULL, -p_money_exchange, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

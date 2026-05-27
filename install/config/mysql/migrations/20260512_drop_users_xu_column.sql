-- SUN-13xx: drop users.xu column + remove xu ref from negative-wallet trigger
USE vinplay;

DROP TRIGGER IF EXISTS trg_users_alert_negative_wallet;

DELIMITER //
CREATE TRIGGER trg_users_alert_negative_wallet
AFTER UPDATE ON users FOR EACH ROW BEGIN
    IF NEW.vin < 0 AND OLD.vin >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES (NEW.id, NEW.user_name, NEW.nick_name, 'vin', OLD.vin, NEW.vin, NEW.vin - OLD.vin, CURRENT_USER(), CONNECTION_ID());
    END IF;
    IF NEW.safe < 0 AND OLD.safe >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES (NEW.id, NEW.user_name, NEW.nick_name, 'safe', OLD.safe, NEW.safe, NEW.safe - OLD.safe, CURRENT_USER(), CONNECTION_ID());
    END IF;
END//
DELIMITER ;

ALTER TABLE users DROP COLUMN xu;

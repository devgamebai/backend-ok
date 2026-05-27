-- Missing stored procedure used by deposit Telegram approval (DepositTransactionDao.creditUserBalance)
-- Without this, Telegram approve button silently fails to credit player balance
DELIMITER ;;
DROP PROCEDURE IF EXISTS `update_money_from_admin`;;
CREATE PROCEDURE `update_money_from_admin`(
    IN p_user_id INT,
    IN p_money BIGINT,
    IN p_money_type VARCHAR(5),
    IN p_action_name VARCHAR(100),
    IN p_description VARCHAR(255)
)
BEGIN
    IF (p_money_type = 'vin') THEN
        UPDATE users SET vin = vin + p_money, vin_total = vin_total + p_money WHERE id = p_user_id;
    ELSE
        UPDATE users SET xu = xu + p_money, xu_total = xu_total + p_money WHERE id = p_user_id;
    END IF;
END;;
DELIMITER ;

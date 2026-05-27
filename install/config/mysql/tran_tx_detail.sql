USE `vinplay_minigame`;
DROP procedure IF EXISTS `save_transaction_detail_tai_xiu`;

USE `vinplay_minigame`;
DROP procedure IF EXISTS `vinplay_minigame`.`save_transaction_detail_tai_xiu`;
;

DELIMITER $$
USE `vinplay_minigame`$$
CREATE DEFINER=`root`@`%` PROCEDURE `save_transaction_detail_tai_xiu`(IN reference_id BIGINT,
    IN transaction_code VARCHAR(45),
    IN user_id INT,
    IN user_name VARCHAR(45),
    IN bet_value BIGINT,
    IN bet_side TINYINT(4),
    IN prize BIGINT,
    IN refund BIGINT,
    IN input_time INT,
    IN money_type TINYINT(4))
BEGIN
  SET total = (SELECT count(*) from transaction_detail_tai_xiu);
  if total > 100 THEN
  DELETE FROM transaction_detail_tai_xiu WHERE id = (SELECT id FROM vinplay_minigame.transaction_detail_tai_xiu order by id ASC limit 1);
  end if;
  INSERT INTO transaction_detail_tai_xiu (`reference_id`, `transaction_code`, `user_id`, `user_name`, `bet_value`, `bet_side`, `prize`, `refund`, `input_time`, `money_type`)
    VALUES(reference_id, transaction_code, user_id, user_name, bet_value, bet_side, prize, refund, input_time, money_type);
END$$

DELIMITER ;
;


DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `save_transaction_detail_tai_xiu` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `save_transaction_detail_tai_xiu`(IN reference_id BIGINT,
    IN transaction_code VARCHAR(45),
    IN user_id INT,
    IN user_name VARCHAR(45),
    IN bet_value BIGINT,
    IN bet_side TINYINT(4),
    IN prize BIGINT,
    IN refund BIGINT,
    IN input_time INT,
    IN money_type TINYINT(4))
BEGIN
    SET total = (SELECT count(*) from transaction_detail_tai_xiu);
      if total > 100 THEN
      DELETE FROM transaction_detail_tai_xiu WHERE id = (SELECT id FROM vinplay_minigame.transaction_detail_tai_xiu order by id ASC limit 1);
      end if;
  INSERT INTO transaction_detail_tai_xiu (`reference_id`, `transaction_code`, `user_id`, `user_name`, `bet_value`, `bet_side`, `prize`, `refund`, `input_time`, `money_type`)
    VALUES(reference_id, transaction_code, user_id, user_name, bet_value, bet_side, prize, refund, input_time, money_type);
END ;;


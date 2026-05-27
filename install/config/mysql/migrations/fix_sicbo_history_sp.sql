USE vinplay_minigame;

DROP PROCEDURE IF EXISTS tx_get_lich_su_giao_dich_chi_tiet_sicbo;

DELIMITER $$

CREATE PROCEDURE tx_get_lich_su_giao_dich_chi_tiet_sicbo(
    IN nickname VARCHAR(45),
    IN page_number INT
)
BEGIN
    DECLARE num_start INT;
    DECLARE num_end INT;
    SET num_start = (page_number - 1) * 20;
    SET num_end = 20;

    SELECT
        detail_tx.reference_id,
        detail_tx.bet_side,
        SUM(detail_tx.bet_value) AS total_bet_value,
        SUM(detail_tx.prize) AS total_prize
    FROM (
        SELECT reference_id, bet_side, bet_value, prize
        FROM transaction_detail_tai_xiu_sicbo
        WHERE user_name = nickname
        ORDER BY reference_id DESC
        LIMIT num_start, num_end
    ) AS detail_tx
    GROUP BY detail_tx.reference_id, detail_tx.bet_side
    ORDER BY detail_tx.reference_id DESC;
END$$

DELIMITER ;

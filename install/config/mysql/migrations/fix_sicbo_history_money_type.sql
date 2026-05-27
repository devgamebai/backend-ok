-- ================================================================
-- FIX: tx_get_lich_su_giao_dich_chi_tiet_sicbo — Thêm money_type filter
--
-- Root cause:
--   SP không filter theo money_type → khi user chơi cả VND (coin) lẫn xu,
--   query trả về lẫn lộn 2 loại cược → bet_value bị sai (VD: 50k xu lẫn
--   vào 1M VND).
--
-- Fix:
--   1. Thêm param IN p_money_type TINYINT vào SP
--   2. Thêm AND money_type = p_money_type vào WHERE clause
--   3. Giữ nguyên fix ORDER BY timestamp DESC từ SUN-848
--
-- Idempotent: DROP IF EXISTS + CREATE.
-- ================================================================

USE vinplay_minigame;

DROP PROCEDURE IF EXISTS tx_get_lich_su_giao_dich_chi_tiet_sicbo;

DELIMITER $$

CREATE PROCEDURE tx_get_lich_su_giao_dich_chi_tiet_sicbo(
    IN nickname      VARCHAR(45),
    IN page_number   INT,
    IN p_money_type  TINYINT
)
BEGIN
    DECLARE num_start INT;
    SET num_start = GREATEST(0, (page_number - 1) * 20);

    SELECT
        detail_tx.reference_id,
        detail_tx.bet_side,
        SUM(detail_tx.bet_value)  AS total_bet_value,
        SUM(detail_tx.prize)      AS total_prize,
        MAX(detail_tx.ts_epoch)   AS bet_timestamp
    FROM (
        SELECT
            reference_id,
            bet_side,
            bet_value,
            prize,
            UNIX_TIMESTAMP(timestamp) AS ts_epoch
        FROM transaction_detail_tai_xiu_sicbo
        WHERE user_name   = nickname
          AND user_id    <> 0
          AND money_type  = p_money_type       -- ✅ FIX: filter đúng coin type
        ORDER BY timestamp DESC, id DESC
        LIMIT num_start, 20
    ) AS detail_tx
    GROUP BY detail_tx.reference_id, detail_tx.bet_side
    ORDER BY bet_timestamp DESC;
END$$

DELIMITER ;

-- ================================================================
-- SUN-848: Sicbo in-game history (c=118) shows wrong/duplicate rows
--
-- Root cause (two layers):
--
-- 1. tx_get_lich_su_giao_dich_chi_tiet_sicbo ORDER BY reference_id DESC.
--    reference_id RESETS DAILY (0 → ~1600 per day). So "top by ref_id"
--    returns the user's bets from the day with the highest rounds ever,
--    NOT their most-recent bets. Players whose busy day was weeks ago
--    get frozen in the past.
--
-- 2. The DAO (TaiXiuSicboDAOImpl.getHistorySicbo) then joins
--    result_tai_xiu_sicbo by `reference_id IN (...)` only. Because
--    reference_id cycles across days, the join matches MANY result rows
--    for the same id (e.g. id=32 has 26 matches across 7 days) → the
--    history list is duplicated with the wrong-day timestamp picked
--    from the last JOIN match.
--
-- Fix (this file covers layer 1):
--
-- Rewrite the SP to ORDER BY the real event time (timestamp DESC) and
-- return bet_timestamp (MAX(timestamp) over the group) as an extra
-- column. The DAO then uses bet_timestamp to narrow the
-- result_tai_xiu_sicbo JOIN to the correct round (same-day ±2 minutes).
--
-- Idempotent: DROP IF EXISTS + CREATE.
-- ================================================================

USE vinplay_minigame;

DROP PROCEDURE IF EXISTS tx_get_lich_su_giao_dich_chi_tiet_sicbo;

DELIMITER $$

CREATE PROCEDURE tx_get_lich_su_giao_dich_chi_tiet_sicbo(
    IN nickname VARCHAR(45),
    IN page_number INT
)
BEGIN
    DECLARE num_start INT;
    SET num_start = GREATEST(0, (page_number - 1) * 20);

    -- Fetch the most recent 20 raw bet rows for this user BY TIME,
    -- then aggregate by (reference_id, bet_side) and carry the bet's
    -- timestamp out so the caller can disambiguate the day.
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
        WHERE user_name = nickname
        ORDER BY timestamp DESC, id DESC
        LIMIT num_start, 20
    ) AS detail_tx
    GROUP BY detail_tx.reference_id, detail_tx.bet_side
    ORDER BY bet_timestamp DESC;
END$$

DELIMITER ;

-- Sanity check: print the first-page result for a known test user
-- so the apply log shows "SP works". Safe to run anytime.
CALL tx_get_lich_su_giao_dich_chi_tiet_sicbo('Kwon_DL1', 1);

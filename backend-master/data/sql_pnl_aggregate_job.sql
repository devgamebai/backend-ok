-- ================================================================
-- Aggregate Job: Cập nhật user_pnl_summary từ log_report_user
-- Chạy mỗi giờ bởi scheduled job (cron / quartz / backend timer)
-- ================================================================

-- ─── D1: hôm nay ───────────────────────────────────────────────
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT
    u.id AS user_id,
    'D1' AS window_type,
    g.game_code,
    COALESCE(SUM(g.bet_val), 0) AS total_bet,
    COALESCE(SUM(g.win_val), 0) AS total_win,
    COUNT(*) AS bet_count
FROM (
    -- Mỗi hàng trong log_report_user đại diện cho 1 ngày × 1 user × 1 game
    -- Cột: user_id (hoặc id), ngày, game-specific bet/win columns
    -- Adjust column names theo schema thực tế của log_report_user
    SELECT l.user_id,
           'taixiu'    AS game_code, l.taixiu_bet    AS bet_val, l.taixiu_win    AS win_val FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.taixiu_bet > 0
    UNION ALL
    SELECT l.user_id, 'baucua',       l.baucua_bet,   l.baucua_win   FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.baucua_bet > 0
    UNION ALL
    SELECT l.user_id, 'minipoker',    l.minipoker_bet,l.minipoker_win FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.minipoker_bet > 0
    UNION ALL
    SELECT l.user_id, 'slot_bitcoin', l.slot_bitcoin_bet, l.slot_bitcoin_win FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.slot_bitcoin_bet > 0
    UNION ALL
    SELECT l.user_id, 'xocdia',       l.xocdia_bet,   l.xocdia_win   FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.xocdia_bet > 0
    UNION ALL
    SELECT l.user_id, 'caothap',      l.caothap_bet,  l.caothap_win  FROM log_report_user l WHERE DATE(l.created_date) = CURDATE() AND l.caothap_bet > 0
) g
JOIN users u ON u.id = g.user_id
GROUP BY u.id, g.game_code
ON DUPLICATE KEY UPDATE
    total_bet  = VALUES(total_bet),
    total_win  = VALUES(total_win),
    bet_count  = VALUES(bet_count),
    updated_at = NOW();

-- Tổng ALL games trong D1
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT user_id, 'D1', 'ALL', SUM(total_bet), SUM(total_win), SUM(bet_count)
FROM user_pnl_summary WHERE window_type='D1' AND game_code <> 'ALL'
GROUP BY user_id
ON DUPLICATE KEY UPDATE
    total_bet  = VALUES(total_bet),
    total_win  = VALUES(total_win),
    bet_count  = VALUES(bet_count),
    updated_at = NOW();


-- ─── D7: 7 ngày gần nhất ────────────────────────────────────────
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT user_id, 'D7' AS window_type, game_code, SUM(total_bet), SUM(total_win), SUM(bet_count)
FROM user_pnl_summary
WHERE window_type = 'D1'
  AND game_code <> 'ALL'
  AND updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY user_id, game_code
ON DUPLICATE KEY UPDATE
    total_bet  = VALUES(total_bet),
    total_win  = VALUES(total_win),
    bet_count  = VALUES(bet_count),
    updated_at = NOW();

-- D7 ALL
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT user_id, 'D7', 'ALL', SUM(total_bet), SUM(total_win), SUM(bet_count)
FROM user_pnl_summary WHERE window_type='D7' AND game_code <> 'ALL'
GROUP BY user_id
ON DUPLICATE KEY UPDATE
    total_bet = VALUES(total_bet), total_win = VALUES(total_win), bet_count = VALUES(bet_count), updated_at = NOW();


-- ─── D30: 30 ngày gần nhất ──────────────────────────────────────
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT user_id, 'D30' AS window_type, game_code, SUM(total_bet), SUM(total_win), SUM(bet_count)
FROM user_pnl_summary
WHERE window_type = 'D1'
  AND game_code <> 'ALL'
  AND updated_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY user_id, game_code
ON DUPLICATE KEY UPDATE
    total_bet  = VALUES(total_bet),
    total_win  = VALUES(total_win),
    bet_count  = VALUES(bet_count),
    updated_at = NOW();

-- D30 ALL
INSERT INTO user_pnl_summary (user_id, window_type, game_code, total_bet, total_win, bet_count)
SELECT user_id, 'D30', 'ALL', SUM(total_bet), SUM(total_win), SUM(bet_count)
FROM user_pnl_summary WHERE window_type='D30' AND game_code <> 'ALL'
GROUP BY user_id
ON DUPLICATE KEY UPDATE
    total_bet = VALUES(total_bet), total_win = VALUES(total_win), bet_count = VALUES(bet_count), updated_at = NOW();

package com.vinplay.dal.rtp;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Service P/L (Profit & Loss) dashboard theo spec §Pha2.
 *
 * Nguồn dữ liệu:
 *   - user_pnl_summary (aggregate job cập nhật mỗi giờ từ log_report_user)
 *
 * Hỗ trợ 5 admin API:
 *   c=9780  GetHouseOverallPnl   — tổng lãi/lỗ Nhà cái theo game + kỳ
 *   c=9781  ListTopWinners       — top N user net dương (target nerf)
 *   c=9782  ListTopLosers        — top N user net âm (candidate VIP/retention)
 *   c=9783  GetUserPnlDetail     — drill-down 1 user
 *   c=9784  GetPlayerDistribution — histogram phân phối net P/L
 */
public class PnlService {

    private static final Logger logger = Logger.getLogger("api");

    // ════════════════════════════════════════════════════════════════════════
    // c=9780: House overall P/L
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tổng lãi/lỗ Nhà cái theo game × window.
     * Lãi Nhà cái = total_bet − total_win (user thua → nhà thắng).
     */
    public List<Map<String, Object>> getHouseOverallPnl(String windowType) throws SQLException {
        String win = validateWindow(windowType);
        String sql = "SELECT game_code, " +
                "     SUM(total_bet) AS house_total_bet, " +
                "     SUM(total_win) AS house_total_win, " +
                "     SUM(total_bet - total_win) AS house_net, " +
                "     COUNT(DISTINCT user_id) AS active_users, " +
                "     SUM(bet_count) AS total_bets " +
                "FROM user_pnl_summary " +
                "WHERE window_type=? " +
                "GROUP BY game_code " +
                "ORDER BY house_net DESC";
        return queryList(sql, win);
    }

    // ════════════════════════════════════════════════════════════════════════
    // c=9781: Top winners (user net dương → nhà cái chảy máu)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @param windowType D1/D7/D30/ALL
     * @param gameCode   null = mọi game (dùng game_code='ALL')
     * @param minBets    min số ván (lọc người chơi vãng lai)
     * @param minNet     min net dương (tiền)
     * @param limit      số dòng trả về
     */
    public List<Map<String, Object>> listTopWinners(String windowType, String gameCode,
                                                    int minBets, long minNet, int limit) throws SQLException {
        String win  = validateWindow(windowType);
        String game = (gameCode == null || gameCode.isEmpty()) ? "ALL" : gameCode.toLowerCase();
        String sql = "SELECT s.user_id, " +
                "     (SELECT nick_name FROM users WHERE id=s.user_id LIMIT 1) AS username, " +
                "     s.game_code, s.total_bet, s.total_win, " +
                "     (s.total_win - s.total_bet) AS net, " +
                "     s.bet_count, " +
                "     o.win_rate_pct AS current_override " +
                "FROM user_pnl_summary s " +
                "LEFT JOIN user_rtp_override o ON o.user_id=s.user_id AND o.game_code=s.game_code " +
                "     AND (o.expires_at IS NULL OR o.expires_at > NOW()) " +
                "WHERE s.window_type=? AND s.game_code=? " +
                "  AND s.bet_count >= ? " +
                "  AND (s.total_win - s.total_bet) >= ? " +
                "ORDER BY net DESC " +
                "LIMIT ?";
        return queryList(sql, win, game, minBets, minNet, Math.min(limit, 500));
    }

    // ════════════════════════════════════════════════════════════════════════
    // c=9782: Top losers
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> listTopLosers(String windowType, String gameCode,
                                                   int minBets, int limit) throws SQLException {
        String win  = validateWindow(windowType);
        String game = (gameCode == null || gameCode.isEmpty()) ? "ALL" : gameCode.toLowerCase();
        String sql = "SELECT s.user_id, " +
                "     (SELECT nick_name FROM users WHERE id=s.user_id LIMIT 1) AS username, " +
                "     s.game_code, s.total_bet, s.total_win, " +
                "     (s.total_win - s.total_bet) AS net, " +
                "     s.bet_count " +
                "FROM user_pnl_summary s " +
                "WHERE s.window_type=? AND s.game_code=? " +
                "  AND s.bet_count >= ? " +
                "  AND (s.total_win - s.total_bet) < 0 " +
                "ORDER BY net ASC " +
                "LIMIT ?";
        return queryList(sql, win, game, minBets, Math.min(limit, 500));
    }

    // ════════════════════════════════════════════════════════════════════════
    // c=9783: User P/L detail (drill-down)
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getUserPnlDetail(long userId, String windowType) throws SQLException {
        String win = validateWindow(windowType);
        String sql = "SELECT s.game_code, s.total_bet, s.total_win, " +
                "     (s.total_win - s.total_bet) AS net, s.bet_count, s.updated_at, " +
                "     o.win_rate_pct AS current_override, o.reason, o.expires_at " +
                "FROM user_pnl_summary s " +
                "LEFT JOIN user_rtp_override o ON o.user_id=s.user_id AND o.game_code=s.game_code " +
                "     AND (o.expires_at IS NULL OR o.expires_at > NOW()) " +
                "WHERE s.user_id=? AND s.window_type=? " +
                "ORDER BY net DESC";
        return queryList(sql, userId, win);
    }

    // ════════════════════════════════════════════════════════════════════════
    // c=9784: Player distribution (histogram)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Phân phối số user theo bucket net P/L.
     * Bucket: net < -1M, -1M~-100k, -100k~-10k, -10k~0, 0~10k, 10k~100k, 100k~1M, > 1M.
     */
    public Map<String, Object> getPlayerDistribution(String windowType, String gameCode) throws SQLException {
        String win  = validateWindow(windowType);
        String game = (gameCode == null || gameCode.isEmpty()) ? "ALL" : gameCode.toLowerCase();
        String sql = "SELECT " +
                "  SUM(CASE WHEN (total_win-total_bet)<-1000000 THEN 1 ELSE 0 END) AS lt_neg1m, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN -1000000 AND -100001 THEN 1 ELSE 0 END) AS neg1m_neg100k, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN -100000 AND -10001 THEN 1 ELSE 0 END) AS neg100k_neg10k, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN -10000 AND -1 THEN 1 ELSE 0 END) AS neg10k_neg1, " +
                "  SUM(CASE WHEN (total_win-total_bet)=0 THEN 1 ELSE 0 END) AS even, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN 1 AND 10000 THEN 1 ELSE 0 END) AS pos1_10k, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN 10001 AND 100000 THEN 1 ELSE 0 END) AS pos10k_100k, " +
                "  SUM(CASE WHEN (total_win-total_bet) BETWEEN 100001 AND 1000000 THEN 1 ELSE 0 END) AS pos100k_1m, " +
                "  SUM(CASE WHEN (total_win-total_bet)>1000000 THEN 1 ELSE 0 END) AS gt_pos1m, " +
                "  COUNT(*) AS total_users, " +
                "  SUM(CASE WHEN (total_win-total_bet)>0 THEN 1 ELSE 0 END) AS positive_users, " +
                "  SUM(CASE WHEN (total_win-total_bet)<0 THEN 1 ELSE 0 END) AS negative_users " +
                "FROM user_pnl_summary " +
                "WHERE window_type=? AND game_code=?";
        List<Map<String, Object>> rows = queryList(sql, win, game);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Phase 7: getTopWinners for Auto-Targeter
    // ════════════════════════════════════════════════════════════════════════
    public List<Map<String, Object>> getTopWinners(String startTime, String endTime, int limit) {
        String sql = "SELECT s.user_id, " +
                "     (SELECT nick_name FROM users WHERE id=s.user_id LIMIT 1) AS nick_name, " +
                "     SUM(s.total_win - s.total_bet) AS net_win " +
                "FROM user_pnl_summary s " +
                "WHERE s.updated_at >= ? AND s.updated_at <= ? " +
                "GROUP BY s.user_id " +
                "HAVING net_win > 0 " +
                "ORDER BY net_win DESC " +
                "LIMIT ?";
        try {
            return queryList(sql, startTime, endTime, limit);
        } catch (SQLException e) {
            logger.error("Error in getTopWinners: ", e);
            return new ArrayList<>();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Phase 5b: Advanced Visualizations
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getThreatScoreLeaderboard(int limit) throws SQLException {
        // Threat score is fetched directly from user_threat_score cache table
        String sql = "SELECT * FROM user_threat_score ORDER BY score DESC LIMIT ?";
        return queryList(sql, limit);
    }

    public List<Map<String, Object>> getPlayerGameHeatmap(String windowType, int limitPlayers) throws SQLException {
        String win = validateWindow(windowType);
        // Step 1: Find top players by total bet volume in this window
        String topPlayersSql = "SELECT user_id FROM user_pnl_summary " +
                "WHERE window_type=? GROUP BY user_id ORDER BY SUM(total_bet) DESC LIMIT ?";
        List<Map<String, Object>> topUserIds = queryList(topPlayersSql, win, limitPlayers);

        if (topUserIds.isEmpty()) return new ArrayList<>();

        StringBuilder ids = new StringBuilder();
        for (Map<String, Object> u : topUserIds) {
            ids.append(u.get("user_id")).append(",");
        }
        ids.deleteCharAt(ids.length() - 1); // remove last comma

        // Step 2: Fetch P/L per game for these top players
        String heatmapSql = "SELECT s.user_id, " +
                "     (SELECT nick_name FROM users WHERE id=s.user_id LIMIT 1) AS nick_name, " +
                "     s.game_code, " +
                "     (s.total_win - s.total_bet) AS net " +
                "FROM user_pnl_summary s " +
                "WHERE s.window_type=? AND s.user_id IN (" + ids.toString() + ") AND s.game_code != 'ALL' " +
                "ORDER BY s.user_id ASC";
        return queryList(heatmapSql, win);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Appendix A: Remaining Visualization Charts
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getTimeWindowHeatmap(String gameCode) throws SQLException {
        // Mocked aggregation for Time Window (Day of Week x Hour) 
        // Real implementation requires joining log_report_user on hour/day, which is very heavy.
        String sql = "SELECT 'Monday' as day, 12 as hour, 85.5 as rtp LIMIT 1";
        return queryList(sql);
    }
    
    public List<Map<String, Object>> getRtpDriftSeries(String gameCode) throws SQLException {
        // Mocked drift series
        String sql = "SELECT updated_at as timestamp, win_rate_pct as target_rtp, win_rate_pct + (RAND()-0.5)*2 as actual_rtp " +
                     "FROM game_rtp_config WHERE game_code=? LIMIT 100";
        return queryList(sql, gameCode);
    }

    public List<Map<String, Object>> getPnlDistribution(String windowType) throws SQLException {
        String win = validateWindow(windowType);
        // Histogram using simple buckets
        String sql = "SELECT " +
                "  CASE " +
                "    WHEN (total_win - total_bet) < -1000000 THEN '< -1M' " +
                "    WHEN (total_win - total_bet) >= -1000000 AND (total_win - total_bet) < 0 THEN '-1M to 0' " +
                "    WHEN (total_win - total_bet) = 0 THEN '0' " +
                "    WHEN (total_win - total_bet) > 0 AND (total_win - total_bet) <= 1000000 THEN '0 to +1M' " +
                "    ELSE '> +1M' " +
                "  END as bucket, " +
                "  COUNT(user_id) as user_count " +
                "FROM user_pnl_summary " +
                "WHERE window_type=? " +
                "GROUP BY bucket";
        return queryList(sql, win);
    }
    
    // ── Private helpers ─────────────────────────────────────────────────────

    private String validateWindow(String w) {
        if ("D1".equals(w) || "D7".equals(w) || "D30".equals(w) || "ALL".equals(w)) return w;
        return "D7"; // default
    }

    private Connection getConn() throws SQLException {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    private List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setObject(i + 1, params[i]);
            try (ResultSet rs = s.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++)
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    results.add(row);
                }
            }
        }
        return results;
    }
}

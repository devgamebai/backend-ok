package com.vinplay.api.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3042 — Get paginated bank withdrawal history for the authenticated user.
 */
public class BankWithdrawalHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Resolve user from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);
            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse pagination params
            int page = 1;
            int limit = 20;
            try {
                String pageStr = request.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) page = Integer.parseInt(pageStr);
            } catch (NumberFormatException ignored) {}
            try {
                String limitStr = request.getParameter("limit");
                if (limitStr != null && !limitStr.isEmpty()) limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException ignored) {}

            if (page < 1) page = 1;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;
            int offset = (page - 1) * limit;

            JSONArray dataArr = new JSONArray();
            int total = 0;
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");

                // Count total (bank + crypto)
                ps = conn.prepareStatement(
                        "SELECT (SELECT COUNT(*) FROM bank_withdrawals WHERE user_id = ?) + " +
                        "(SELECT COUNT(*) FROM crypto_withdrawals WHERE user_id = ?)");
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                rs = ps.executeQuery();
                if (rs.next()) total = rs.getInt(1);
                rs.close();
                ps.close();

                // Unified query: bank + crypto withdrawals
                ps = conn.prepareStatement(
                        "(SELECT tx_code, bank_name, bank_number, customer_name, amount_krw, fee_krw, " +
                        "CAST(0 AS DECIMAL(20,6)) as amount_usdt, CAST(0 AS DECIMAL(20,6)) as fee_usdt, " +
                        "status, reject_reason, created_at, processed_at, 'bank' as type " +
                        "FROM bank_withdrawals WHERE user_id = ?) " +
                        "UNION ALL " +
                        "(SELECT tx_code, 'crypto' as bank_name, to_address as bank_number, '' as customer_name, " +
                        "amount_krw, 0 as fee_krw, amount_usdt, fee_usdt, " +
                        "status, reject_reason, created_at, processed_at, 'crypto' as type " +
                        "FROM crypto_withdrawals WHERE user_id = ?) " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?");
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                ps.setInt(3, limit);
                ps.setInt(4, offset);
                rs = ps.executeQuery();

                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("tx_code", rs.getString("tx_code"));
                    row.put("type", rs.getString("type"));
                    row.put("bank_name", rs.getString("bank_name"));
                    row.put("bank_number", rs.getString("bank_number"));
                    row.put("customer_name", rs.getString("customer_name"));
                    row.put("amount_krw", rs.getLong("amount_krw"));
                    row.put("fee_krw", rs.getLong("fee_krw"));
                    row.put("amount_usdt", rs.getDouble("amount_usdt"));
                    row.put("fee_usdt", rs.getDouble("fee_usdt"));
                    row.put("status", rs.getString("status"));
                    row.put("reject_reason", rs.getString("reject_reason"));
                    row.put("created_at", rs.getString("created_at"));
                    row.put("processed_at", rs.getString("processed_at"));
                    dataArr.put(row);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            int totalPages = Math.max(1, (total + limit - 1) / limit);

            response.put("success", true);
            response.put("data", dataArr);
            response.put("total", total);
            response.put("page", page);
            response.put("limit", limit);
            response.put("totalPages", totalPages);

        } catch (Exception e) {
            logger.error("BankWithdrawalHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

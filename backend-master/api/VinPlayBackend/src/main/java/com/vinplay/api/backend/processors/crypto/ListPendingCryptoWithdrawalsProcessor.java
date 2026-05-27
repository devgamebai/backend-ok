package com.vinplay.api.backend.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9630 — List crypto withdrawals filtered by status (admin).
 */
public class ListPendingCryptoWithdrawalsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse params
            String status = request.getParameter("status");
            if (status == null || status.isEmpty()) status = "PENDING";

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
            PreparedStatement psCount = null;
            ResultSet rs = null;
            ResultSet rsCount = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");

                // Count total
                psCount = conn.prepareStatement(
                        "SELECT COUNT(*) FROM crypto_withdrawals WHERE status = ?");
                psCount.setString(1, status);
                rsCount = psCount.executeQuery();
                if (rsCount.next()) {
                    total = rsCount.getInt(1);
                }

                // Fetch page
                ps = conn.prepareStatement(
                        "SELECT id, user_id, nick_name, to_address, amount_krw, amount_usdt, tx_code, " +
                        "gateway_tx_id, status, admin_by, reject_reason, created_at, processed_at " +
                        "FROM crypto_withdrawals WHERE status = ? ORDER BY id DESC LIMIT ? OFFSET ?");
                ps.setString(1, status);
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                rs = ps.executeQuery();

                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getLong("id"));
                    row.put("user_id", rs.getLong("user_id"));
                    row.put("nick_name", rs.getString("nick_name"));
                    row.put("to_address", rs.getString("to_address"));
                    row.put("amount_krw", rs.getLong("amount_krw"));
                    row.put("amount_usdt", rs.getDouble("amount_usdt"));
                    row.put("tx_code", rs.getString("tx_code"));
                    row.put("gateway_tx_id", rs.getString("gateway_tx_id"));
                    row.put("status", rs.getString("status"));
                    row.put("admin_by", rs.getString("admin_by"));
                    row.put("reject_reason", rs.getString("reject_reason"));
                    row.put("created_at", rs.getString("created_at"));
                    row.put("processed_at", rs.getString("processed_at"));
                    dataArr.put(row);
                }
            } finally {
                if (rsCount != null) try { rsCount.close(); } catch (Exception ignored) {}
                if (psCount != null) try { psCount.close(); } catch (Exception ignored) {}
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("data", dataArr);
            response.put("total", total);

        } catch (Exception e) {
            logger.error("ListPendingCryptoWithdrawalsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

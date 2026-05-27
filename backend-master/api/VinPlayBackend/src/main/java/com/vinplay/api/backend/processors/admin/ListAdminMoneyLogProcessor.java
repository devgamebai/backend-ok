package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9906 - List admin money adjustment logs (cộng/trừ tiền).
 * Queries vinplay_admin.log_admin with filters and pagination.
 */
public class ListAdminMoneyLogProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String accountName = request.getParameter("nn");
            String adminUser = request.getParameter("admin");
            String action = request.getParameter("action");
            String dateFrom = request.getParameter("ts");
            String dateTo = request.getParameter("te");
            String pageStr = request.getParameter("page");
            String limitStr = request.getParameter("limit");

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                page = Math.max(1, Integer.parseInt(pageStr));
            }
            int limit = DEFAULT_LIMIT;
            if (limitStr != null && !limitStr.isEmpty()) {
                limit = Math.min(MAX_LIMIT, Math.max(1, Integer.parseInt(limitStr)));
            }
            int offset = (page - 1) * limit;

            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");

            // Build WHERE clause
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<Object>();

            if (accountName != null && !accountName.isEmpty()) {
                where.append(" AND account_name LIKE ?");
                params.add("%" + accountName + "%");
            }
            if (adminUser != null && !adminUser.isEmpty()) {
                where.append(" AND username LIKE ?");
                params.add("%" + adminUser + "%");
            }
            if (action != null && !action.isEmpty()) {
                where.append(" AND action LIKE ?");
                params.add("%" + action + "%");
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                where.append(" AND timestamp >= ?");
                params.add(dateFrom);
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                where.append(" AND timestamp <= ?");
                params.add(dateTo + " 23:59:59");
            }

            // Count
            ps = conn.prepareStatement("SELECT COUNT(*) FROM log_admin" + where);
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            int totalRecords = 0;
            if (rs.next()) totalRecords = rs.getInt(1);
            rs.close();
            ps.close();

            // Fetch
            ps = conn.prepareStatement(
                    "SELECT id, action, quantity, money, account_name, money_type, username, timestamp, reason, status " +
                    "FROM log_admin" + where + " ORDER BY id DESC LIMIT ? OFFSET ?");
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.setInt(params.size() + 1, limit);
            ps.setInt(params.size() + 2, offset);
            rs = ps.executeQuery();

            JSONArray data = new JSONArray();
            while (rs.next()) {
                JSONObject row = new JSONObject();
                row.put("id", rs.getInt("id"));
                row.put("action", rs.getString("action") != null ? rs.getString("action") : "");
                row.put("quantity", rs.getInt("quantity"));
                row.put("money", rs.getLong("money"));
                row.put("account_name", rs.getString("account_name") != null ? rs.getString("account_name") : "");
                row.put("money_type", rs.getString("money_type") != null ? rs.getString("money_type") : "");
                row.put("username", rs.getString("username") != null ? rs.getString("username") : "");
                row.put("timestamp", rs.getString("timestamp") != null ? rs.getString("timestamp") : "");
                row.put("reason", rs.getString("reason") != null ? rs.getString("reason") : "");
                row.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                data.put(row);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("page", page);
            response.put("limit", limit);
            response.put("totalPages", (int) Math.ceil((double) totalRecords / limit));

        } catch (NumberFormatException e) {
            logger.error("ListAdminMoneyLogProcessor: invalid param", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("ListAdminMoneyLogProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
        return response.toString();
    }
}

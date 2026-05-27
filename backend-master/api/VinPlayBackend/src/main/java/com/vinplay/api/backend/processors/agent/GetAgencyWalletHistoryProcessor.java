package com.vinplay.api.backend.processors.agent;

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
 * c=9891 — Agency wallet transaction history.
 * Params: rc (agent nickname), ft/et (date range), type (optional filter), pg/size
 */
public class GetAgencyWalletHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");
            String typeFilter = request.getParameter("type");
            String ft = request.getParameter("ft");
            String et = request.getParameter("et");
            int page = 1, limit = 20;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("size") != null) limit = Integer.parseInt(request.getParameter("size")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            if (rc == null || rc.isEmpty()) return err(response, "1001", "rc required");

            // Resolve agent
            int agentId = -1;
            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = adminConn.prepareStatement(
                         "SELECT id FROM useragent WHERE nickname = ? OR code = ? OR username = ? LIMIT 1")) {
                ps.setString(1, rc);
                ps.setString(2, rc);
                ps.setString(3, rc);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) agentId = rs.getInt("id");
                }
            }
            if (agentId <= 0) return err(response, "1002", "Agent not found");

            // Build query
            StringBuilder where = new StringBuilder(" WHERE agent_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(agentId);

            if (typeFilter != null && !typeFilter.isEmpty()) {
                where.append(" AND type = ?");
                params.add(typeFilter);
            }
            if (ft != null && !ft.isEmpty()) {
                where.append(" AND created_at >= ?");
                params.add(ft.contains(" ") ? ft : ft + " 00:00:00");
            }
            if (et != null && !et.isEmpty()) {
                where.append(" AND created_at <= ?");
                params.add(et.contains(" ") ? et : et + " 23:59:59");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                int total = 0;
                try (PreparedStatement countPs = conn.prepareStatement(
                        "SELECT COUNT(*) FROM agency_wallet_transactions" + where)) {
                    for (int i = 0; i < params.size(); i++) {
                        Object p = params.get(i);
                        if (p instanceof Integer) countPs.setInt(i + 1, (Integer) p);
                        else countPs.setString(i + 1, p.toString());
                    }
                    try (ResultSet rs = countPs.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                JSONArray arr = new JSONArray();
                String sql = "SELECT * FROM agency_wallet_transactions" + where +
                        " ORDER BY created_at DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int idx = 1;
                    for (Object p : params) {
                        if (p instanceof Integer) ps.setInt(idx++, (Integer) p);
                        else ps.setString(idx++, p.toString());
                    }
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, (page - 1) * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("type", rs.getString("type"));
                            row.put("amount", rs.getLong("amount"));
                            row.put("direction", rs.getString("direction"));
                            row.put("balance_after", rs.getLong("balance_after"));
                            row.put("related_user", rs.getString("related_user") != null ? rs.getString("related_user") : "");
                            row.put("game_action", rs.getString("game_action") != null ? rs.getString("game_action") : "");
                            row.put("note", rs.getString("note") != null ? rs.getString("note") : "");
                            row.put("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
                            arr.put(row);
                        }
                    }
                }

                // Get current balance
                long currentBalance = com.vinplay.dal.rebate.RebateService.getAgencyWalletBalance(agentId);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("page", page);
                response.put("limit", limit);
                response.put("current_balance", currentBalance);
            }
        } catch (Exception e) {
            logger.error("GetAgencyWalletHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}

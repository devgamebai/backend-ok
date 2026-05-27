package com.vinplay.api.backend.processors.agentcode;

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
 * c=9832 — List agent code requests for admin review.
 * Params: aat, status (default 'PENDING'), agent_nickname?, pn, l.
 */
public class ListPendingCodeRequestsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String status = request.getParameter("status");
            if (status == null || status.isEmpty()) status = "PENDING";
            String agentFilter = request.getParameter("agent_nickname");
            int page = 0, limit = 50;
            try { if (request.getParameter("pn") != null) page = Integer.parseInt(request.getParameter("pn")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 0) page = 0; if (limit < 1 || limit > 200) limit = 50;

            StringBuilder where = new StringBuilder(" WHERE status = ?");
            List<Object> params = new ArrayList<>();
            params.add(status);
            if (agentFilter != null && !agentFilter.isEmpty()) {
                where.append(" AND agent_nickname LIKE ?");
                params.add("%" + agentFilter + "%");
            }

            int total = 0;
            JSONArray arr = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM agent_code_request" + where)) {
                    bind(ps, params);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getInt(1); }
                }
                String sql = "SELECT id, agent_id, agent_nickname, desired_code, normalized_code, status, " +
                        "reviewed_by, DATE_FORMAT(reviewed_at,'%Y-%m-%d %H:%i:%s') reviewed_at, " +
                        "reject_reason, DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s') created_at " +
                        "FROM agent_code_request" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = bind(ps, params);
                    ps.setInt(i++, limit);
                    ps.setInt(i, page * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("agent_id", rs.getInt("agent_id"));
                            row.put("agent_nickname", rs.getString("agent_nickname"));
                            row.put("desired_code", rs.getString("desired_code"));
                            row.put("normalized_code", rs.getString("normalized_code"));
                            row.put("status", rs.getString("status"));
                            row.put("reviewed_by", rs.getString("reviewed_by") != null ? rs.getString("reviewed_by") : "");
                            row.put("reviewed_at", rs.getString("reviewed_at") != null ? rs.getString("reviewed_at") : "");
                            row.put("reject_reason", rs.getString("reject_reason") != null ? rs.getString("reject_reason") : "");
                            row.put("created_at", rs.getString("created_at"));
                            arr.put(row);
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("total", total);
            response.put("page", page);
            response.put("limit", limit);
        } catch (Exception e) {
            logger.error("ListPendingCodeRequestsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static int bind(PreparedStatement ps, List<Object> params) throws java.sql.SQLException {
        int i = 1;
        for (Object p : params) ps.setString(i++, String.valueOf(p));
        return i;
    }
}

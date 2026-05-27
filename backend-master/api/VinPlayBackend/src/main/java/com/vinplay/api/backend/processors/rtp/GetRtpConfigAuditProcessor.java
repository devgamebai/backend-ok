package com.vinplay.api.backend.processors.rtp;

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
 * c=9775 — Read RTP config audit trail.
 * Params: aat, scope ('GAME'|'USER'), game_code, user_id, from (yyyy-MM-dd), to (yyyy-MM-dd), pn, l.
 */
public class GetRtpConfigAuditProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String scope = request.getParameter("scope");
            String gameCode = request.getParameter("game_code");
            String userIdStr = request.getParameter("user_id");
            String from = request.getParameter("from");
            String to = request.getParameter("to");
            int page = 0, limit = 50;
            try { if (request.getParameter("pn") != null) page = Integer.parseInt(request.getParameter("pn")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 0) page = 0;
            if (limit < 1 || limit > 500) limit = 50;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (scope != null && !scope.isEmpty()) { where.append(" AND scope = ?"); params.add(scope); }
            if (gameCode != null && !gameCode.isEmpty()) { where.append(" AND game_code = ?"); params.add(gameCode); }
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try { long uid = Long.parseLong(userIdStr); where.append(" AND user_id = ?"); params.add(uid); }
                catch (NumberFormatException ignored) {}
            }
            if (from != null && !from.isEmpty()) { where.append(" AND at >= ?"); params.add(from + " 00:00:00"); }
            if (to != null && !to.isEmpty()) { where.append(" AND at <= ?"); params.add(to + " 23:59:59"); }

            int total = 0;
            JSONArray arr = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM rtp_config_audit" + where)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getInt(1); }
                }
                String sql = "SELECT id, scope, game_code, user_id, action, old_value, new_value, actor, reason, " +
                        "DATE_FORMAT(at,'%Y-%m-%d %H:%i:%s') AS at " +
                        "FROM rtp_config_audit" + where +
                        " ORDER BY at DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = bindParams(ps, params);
                    ps.setInt(i++, limit);
                    ps.setInt(i, page * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("scope", rs.getString("scope"));
                            row.put("game_code", rs.getString("game_code") != null ? rs.getString("game_code") : "");
                            row.put("user_id", rs.getObject("user_id"));
                            row.put("action", rs.getString("action"));
                            row.put("old_value", rs.getObject("old_value"));
                            row.put("new_value", rs.getObject("new_value"));
                            row.put("actor", rs.getString("actor"));
                            row.put("reason", rs.getString("reason") != null ? rs.getString("reason") : "");
                            row.put("at", rs.getString("at"));
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
            logger.error("GetRtpConfigAuditProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static int bindParams(PreparedStatement ps, List<Object> params) throws java.sql.SQLException {
        int i = 1;
        for (Object p : params) {
            if (p instanceof Long) ps.setLong(i++, (Long) p);
            else ps.setString(i++, String.valueOf(p));
        }
        return i;
    }
}

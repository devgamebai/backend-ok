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
 * c=9772 — List user RTP overrides with optional filters + pagination.
 * Params: aat, game_code (optional), user_id (optional), pn (page, default 0), l (limit, default 50).
 */
public class ListUserRtpOverrideProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String gameCode = request.getParameter("game_code");
            String userIdStr = request.getParameter("user_id");
            int page = 0, limit = 50;
            try { if (request.getParameter("pn") != null) page = Integer.parseInt(request.getParameter("pn")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 0) page = 0;
            if (limit < 1 || limit > 500) limit = 50;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (gameCode != null && !gameCode.isEmpty()) {
                where.append(" AND game_code = ?");
                params.add(gameCode);
            }
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    long uid = Long.parseLong(userIdStr);
                    where.append(" AND user_id = ?");
                    params.add(uid);
                } catch (NumberFormatException ignored) {}
            }

            int total = 0;
            JSONArray arr = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM user_rtp_override" + where)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getInt(1); }
                }
                String sql = "SELECT user_id, game_code, win_rate_pct, reason, " +
                        "DATE_FORMAT(expires_at,'%Y-%m-%d %H:%i:%s') AS expires_at, " +
                        "created_by, DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s') AS created_at " +
                        "FROM user_rtp_override" + where +
                        " ORDER BY created_at DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = bindParams(ps, params);
                    ps.setInt(i++, limit);
                    ps.setInt(i, page * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("user_id", rs.getLong("user_id"));
                            row.put("game_code", rs.getString("game_code"));
                            // SUN-1098: 2-decimal string for RTP override display.
                            row.put("win_rate_pct", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "win_rate_pct"));
                            row.put("reason", rs.getString("reason"));
                            row.put("expires_at", rs.getString("expires_at") != null ? rs.getString("expires_at") : "");
                            row.put("created_by", rs.getString("created_by"));
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
            logger.error("ListUserRtpOverrideProcessor error", e);
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

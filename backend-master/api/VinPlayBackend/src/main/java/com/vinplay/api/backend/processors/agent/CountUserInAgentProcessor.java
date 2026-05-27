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
 * c=8840 -- Count users per agent.
 * Params: nn (agent nickname filter), p, l
 */
public class CountUserInAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE parrentUser IS NOT NULL AND parrentUser != ''");
            List<Object> params = new ArrayList<>();

            if (nickname != null && !nickname.isEmpty()) {
                where.append(" AND parrentUser LIKE ?");
                params.add("%" + nickname + "%");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count distinct agents
                String countSql = "SELECT COUNT(DISTINCT parrentUser) FROM users" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data: group by agent
                String dataSql = "SELECT parrentUser as agent, COUNT(*) as user_count, " +
                        "SUM(t_nap) as total_nap, SUM(t_rut) as total_rut " +
                        "FROM users" + where +
                        " GROUP BY parrentUser ORDER BY user_count DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, String.valueOf(p));
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("agent", rs.getString("agent") != null ? rs.getString("agent") : "");
                            item.put("user_count", rs.getInt("user_count"));
                            item.put("total_nap", rs.getLong("total_nap"));
                            item.put("total_rut", rs.getLong("total_rut"));
                            arr.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("totalRecords", total);
            }

        } catch (Exception e) {
            logger.error("CountUserInAgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

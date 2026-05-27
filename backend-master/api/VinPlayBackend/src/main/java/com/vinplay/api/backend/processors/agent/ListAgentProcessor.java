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
 * c=8826 -- List agents (users with dai_ly > 0).
 * Params: nn (nickname filter), ft (from time), et (to time), p (page), l (limit)
 */
public class ListAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE dai_ly > 0");
            List<Object> params = new ArrayList<>();

            if (nickname != null && !nickname.isEmpty()) {
                where.append(" AND (nick_name LIKE ? OR parrentUser LIKE ?)");
                params.add("%" + nickname + "%");
                params.add("%" + nickname + "%");
            }
            if (fromTime != null && !fromTime.isEmpty()) {
                where.append(" AND create_time >= ?");
                params.add(fromTime);
            }
            if (toTime != null && !toTime.isEmpty()) {
                where.append(" AND create_time <= ?");
                params.add(toTime);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM users" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, user_name, nick_name, dai_ly, vin, 0 AS xu, 0 AS recharge_money, " + // SUN-13xx: xu dropped, kept alias for response parity
                        "referral_code, mobile, create_time, last_login, status " +
                        "FROM users" + where + " ORDER BY dai_ly DESC, id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, String.valueOf(p));
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("user_name", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                            item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("dai_ly", rs.getInt("dai_ly"));
                            item.put("vin", rs.getLong("vin"));
                            item.put("xu", 0L);
                            item.put("recharge_money", 0L);
                            item.put("referral_code", rs.getString("referral_code") != null ? rs.getString("referral_code") : "");
                            item.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                            item.put("create_time", rs.getString("create_time") != null ? rs.getString("create_time") : "");
                            item.put("last_login", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                            item.put("status", rs.getInt("status"));
                            arr.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("totalData", total);
                response.put("totalRecords", total);
            }

        } catch (Exception e) {
            logger.error("ListAgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

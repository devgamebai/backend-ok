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

public class SearchUsersProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String q = request.getParameter("q");

            if (q == null || q.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int limit = 10;
            try { String s = request.getParameter("l"); if (s != null && !s.isEmpty()) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                JSONArray list = new JSONArray();
                String sql = "SELECT id, user_name, nick_name, vin, 0 AS xu, 0 AS safe, 0 AS vip_point FROM users WHERE nick_name LIKE ? OR user_name LIKE ? OR parrentUser LIKE ? LIMIT ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    String pattern = "%" + q + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                    ps.setString(3, pattern);
                    ps.setInt(4, limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("userName", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                            item.put("nickName", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("vin", rs.getLong("vin"));
                            item.put("xu", 0L);
                            item.put("safe", 0L);
                            item.put("vipPoint", 0);
                            list.put(item);
                        }
                    }
                }

                JSONObject data = new JSONObject();
                data.put("list", list);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data.toString());
            }
        } catch (Exception e) {
            logger.error("SearchUsersProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

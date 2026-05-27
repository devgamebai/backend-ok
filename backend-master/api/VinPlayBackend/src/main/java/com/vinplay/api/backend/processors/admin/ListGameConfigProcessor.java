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

public class ListGameConfigProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            int page = 1;
            int limit = 10;
            try { String s = request.getParameter("pn"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null && !s.isEmpty()) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 10;
            if (limit > 200) limit = 200;

            int offset = (page - 1) * limit;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int total = 0;
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM game_config")) {
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) total = countRs.getInt(1);
                    }
                }

                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM game_config ORDER BY id DESC LIMIT ?,?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, offset);
                    ps.setInt(2, limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("name", rs.getString("name") != null ? rs.getString("name") : "");
                            item.put("value", rs.getString("value") != null ? rs.getString("value") : "");
                            item.put("version", rs.getString("version") != null ? rs.getString("version") : "");
                            item.put("platform", rs.getString("platform") != null ? rs.getString("platform") : "");
                            list.put(item);
                        }
                    }
                }

                JSONObject data = new JSONObject();
                data.put("total", total);
                data.put("list", list);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data.toString());
            }
        } catch (Exception e) {
            logger.error("ListGameConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

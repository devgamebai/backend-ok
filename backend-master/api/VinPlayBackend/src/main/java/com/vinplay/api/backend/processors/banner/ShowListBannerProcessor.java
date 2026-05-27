package com.vinplay.api.backend.processors.banner;

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

/**
 * c=9403 — Show list of banners with pagination.
 * Params: p (page), l (limit)
 */
public class ShowListBannerProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM banner")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, title, status, image_path, action, url FROM banner ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("title", rs.getString("title") != null ? rs.getString("title") : "");
                            item.put("status", rs.getInt("status"));
                            item.put("image_path", rs.getString("image_path") != null ? rs.getString("image_path") : "");
                            item.put("action", rs.getString("action") != null ? rs.getString("action") : "");
                            item.put("url", rs.getString("url") != null ? rs.getString("url") : "");
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
            logger.error("ShowListBannerProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

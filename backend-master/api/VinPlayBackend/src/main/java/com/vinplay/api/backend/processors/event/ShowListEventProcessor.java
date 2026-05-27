package com.vinplay.api.backend.processors.event;

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
 * c=9413 — Show list of events with pagination.
 * Params: p (page), l (limit)
 */
public class ShowListEventProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM event")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, name, created_date, amount, expired_date, create_by FROM event ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("name", rs.getString("name") != null ? rs.getString("name") : "");
                            item.put("created_date", rs.getString("created_date") != null ? rs.getString("created_date") : "");
                            item.put("amount", rs.getLong("amount"));
                            item.put("expired_date", rs.getString("expired_date") != null ? rs.getString("expired_date") : "");
                            item.put("create_by", rs.getString("create_by") != null ? rs.getString("create_by") : "");
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
            logger.error("ShowListEventProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.giftcode;

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
 * c=9001 — Show gift codes with pagination and filters.
 * Params: p (page), l (limit), code (filter), type (filter)
 */
public class ShowGiftCodeProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String code = request.getParameter("code");
            String type = request.getParameter("type");
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (code != null && !code.isEmpty()) {
                where.append(" AND giftcode LIKE ?");
                params.add("%" + code + "%");
            }
            if (type != null && !type.isEmpty()) {
                where.append(" AND type = ?");
                params.add(type);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM gift_codes" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, (String) params.get(i));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, giftcode, type, money, time_used, max_use, `from`, exprired, created_at, created_by, event, user_name, bundle_id FROM gift_codes"
                        + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, (String) p);
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("giftcode", rs.getString("giftcode") != null ? rs.getString("giftcode") : "");
                            item.put("type", rs.getInt("type"));
                            item.put("money", rs.getLong("money"));
                            item.put("time_used", rs.getInt("time_used"));
                            item.put("max_use", rs.getInt("max_use"));
                            item.put("from", rs.getString("from") != null ? rs.getString("from") : "");
                            item.put("exprired", rs.getString("exprired") != null ? rs.getString("exprired") : "");
                            item.put("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
                            item.put("created_by", rs.getString("created_by") != null ? rs.getString("created_by") : "");
                            item.put("event", rs.getString("event") != null ? rs.getString("event") : "");
                            item.put("user_name", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                            item.put("bundle_id", rs.getInt("bundle_id"));
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
            logger.error("ShowGiftCodeProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

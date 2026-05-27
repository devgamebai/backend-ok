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
 * c=9002 — Show used gift codes with pagination and filter.
 * Params: p (page), l (limit), code (giftcode search — full DB), nn (username exact), ts (start date yyyy-MM-dd), te (end date yyyy-MM-dd)
 */
public class ShowGiftCodeUsedProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String code      = request.getParameter("code");
            String nickName  = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime   = request.getParameter("te");
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (code != null && !code.isEmpty()) {
                where.append(" AND g.giftcode LIKE ?");
                params.add("%" + code + "%");
            }
            if (nickName != null && !nickName.isEmpty()) {
                where.append(" AND u.username = ?");
                params.add(nickName);
            }
            if (startTime != null && !startTime.isEmpty()) {
                where.append(" AND u.created_at >= ?");
                params.add(startTime + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                where.append(" AND u.created_at <= ?");
                params.add(endTime + " 23:59:59");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM gift_code_useds u JOIN gift_codes g ON u.giftcode_id = g.id" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, (String) params.get(i));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT u.giftcode_id, u.username, u.created_at, u.event, g.giftcode, g.money, g.type " +
                        "FROM gift_code_useds u JOIN gift_codes g ON u.giftcode_id = g.id" +
                        where + " ORDER BY u.created_at DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, (String) p);
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("giftcode_id", rs.getInt("giftcode_id"));
                            item.put("username", rs.getString("username") != null ? rs.getString("username") : "");
                            item.put("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
                            item.put("event", rs.getString("event") != null ? rs.getString("event") : "");
                            item.put("giftcode", rs.getString("giftcode") != null ? rs.getString("giftcode") : "");
                            item.put("money", rs.getLong("money"));
                            item.put("type", rs.getInt("type"));
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
            logger.error("ShowGiftCodeUsedProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.hoantra;

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
 * c=8902 -- Get hoan tra data (current/pending).
 * Params: nn (nickname), ts (time start), te (time end), p (page)
 */
public class GetDataHoanTraProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (nickname != null && !nickname.isEmpty()) {
                where.append(" AND nick_name LIKE ?");
                params.add("%" + nickname + "%");
            }
            if (timeStart != null && !timeStart.isEmpty()) {
                where.append(" AND time >= ?");
                params.add(timeStart);
            }
            if (timeEnd != null && !timeEnd.isEmpty()) {
                where.append(" AND time <= ?");
                params.add(timeEnd);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM log_hoan_tra" + where)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, nick_name, time, vip_point, " +
                        "total_money_sport, hoan_tra_sport, " +
                        "total_money_casino, hoan_tra_casino, " +
                        "total_money_game, hoan_tra_game, " +
                        "vip_index, send_success, message, created_at, updated_at " +
                        "FROM log_hoan_tra" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, String.valueOf(p));
                    ps.setInt(idx++, PAGE_SIZE);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("time", rs.getString("time") != null ? rs.getString("time") : "");
                            item.put("vip_point", 0);
                            item.put("total_money_sport", rs.getInt("total_money_sport"));
                            item.put("hoan_tra_sport", rs.getInt("hoan_tra_sport"));
                            item.put("total_money_casino", rs.getInt("total_money_casino"));
                            item.put("hoan_tra_casino", rs.getInt("hoan_tra_casino"));
                            item.put("total_money_game", rs.getInt("total_money_game"));
                            item.put("hoan_tra_game", rs.getInt("hoan_tra_game"));
                            item.put("vip_index", rs.getInt("vip_index"));
                            item.put("send_success", rs.getObject("send_success") != null ? rs.getBoolean("send_success") : false);
                            item.put("message", rs.getString("message") != null ? rs.getString("message") : "");
                            item.put("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
                            item.put("updated_at", rs.getString("updated_at") != null ? rs.getString("updated_at") : "");
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
            logger.error("GetDataHoanTraProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

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
 * c=8829 -- List Child Agents (TĐL -> ĐL1, ĐL1 -> ĐL2).
 * Params: rc (nguoi dang nhap), nn (nickname filter), ft, et, p, l
 */
public class ListChildAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String agentNick = request.getParameter("rc"); // User dang dang nhap (hoac dang xem)
            String nickname = request.getParameter("nn");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");
            int page = 1, limit = 20;

            if (agentNick == null || agentNick.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Missing rc (agent identifier)");
                return response.toString();
            }

            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                
                // 1. Kiem tra cap do (dai_ly) cua thang dang goi
                int currentLevel = -1;
                String checkLevelSql = "SELECT dai_ly FROM users WHERE nick_name = ?";
                try (PreparedStatement psCheck = conn.prepareStatement(checkLevelSql)) {
                    psCheck.setString(1, agentNick);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            currentLevel = rsCheck.getInt("dai_ly");
                        }
                    }
                }

                if (currentLevel < 1 || currentLevel > 2) {
                    // Neu khong phai TDL (=1) hoac DL1 (=2), tra ve rong (DL2 hoac User thuong khong co doi tac con)
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", new JSONArray());
                    response.put("total", 0);
                    return response.toString();
                }

                int targetLevel = currentLevel + 1; // Thang con cua no

                // 2. Build Where
                StringBuilder where = new StringBuilder(" WHERE parrentUser = ? AND dai_ly = ?");
                List<Object> params = new ArrayList<>();
                params.add(agentNick);
                params.add(targetLevel);

                if (nickname != null && !nickname.isEmpty()) {
                    where.append(" AND nick_name LIKE ?");
                    params.add("%" + nickname + "%");
                }
                if (fromTime != null && !fromTime.isEmpty()) {
                    where.append(" AND create_time >= ?");
                    params.add(fromTime + " 00:00:00");
                }
                if (toTime != null && !toTime.isEmpty()) {
                    where.append(" AND create_time <= ?");
                    params.add(toTime + " 23:59:59");
                }

                // 3. Dem so luong
                int total = 0;
                String countSql = "SELECT COUNT(*) FROM users" + where;
                try (PreparedStatement psCount = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) psCount.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rsCount = psCount.executeQuery()) {
                        if (rsCount.next()) total = rsCount.getInt(1);
                    }
                }

                // 4. Lay danh sach
                String dataSql = "SELECT id, user_name, nick_name, dai_ly, vin, 0 AS xu, 0 AS recharge_money, " + // SUN-13xx: xu dropped, kept alias for response parity
                        "t_nap, t_rut, referral_code, mobile, create_time, last_login, status " +
                        "FROM users" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement psData = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) psData.setString(idx++, String.valueOf(p));
                    psData.setInt(idx++, limit);
                    psData.setInt(idx, offset);
                    try (ResultSet rs = psData.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("user_name", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                            item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("dai_ly", rs.getInt("dai_ly"));
                            item.put("vin", rs.getLong("vin"));
                            item.put("xu", 0L);
                            item.put("recharge_money", 0L);
                            item.put("t_nap", rs.getLong("t_nap"));
                            item.put("t_rut", rs.getLong("t_rut"));
                            item.put("profit", rs.getLong("t_nap") - rs.getLong("t_rut"));
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
                response.put("totalRecords", total);
            }

        } catch (Exception e) {
            logger.error("ListChildAgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

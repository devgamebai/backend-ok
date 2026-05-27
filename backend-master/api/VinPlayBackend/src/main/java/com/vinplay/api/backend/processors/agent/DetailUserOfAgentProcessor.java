package com.vinplay.api.backend.processors.agent;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=8828 -- Detail of a user under an agent.
 * Params: nn (nickname)
 */
public class DetailUserOfAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");

            if (nickname == null || nickname.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Missing nickname parameter");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String sql = "SELECT id, user_name, nick_name, vin, 0 AS xu, 0 AS safe, 0 AS recharge_money, " + // SUN-13xx: xu, safe dropped, kept aliases for response parity
                        "dai_ly, 0 AS vip_point, t_nap, t_rut, nap_times, rut_times, " + // SUN-13xx: vip_point dropped, kept alias for response parity
                        "parrentUser, referral_code, mobile, email, " +
                        "create_time, last_login, status " +
                        "FROM users WHERE nick_name = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            JSONObject data = new JSONObject();
                            data.put("id", rs.getLong("id"));
                            data.put("user_name", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                            data.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            data.put("vin", rs.getLong("vin"));
                            data.put("xu", 0L);
                            data.put("safe", 0L);
                            data.put("recharge_money", 0L);
                            data.put("dai_ly", rs.getInt("dai_ly"));
                            data.put("vip_point", 0);
                            data.put("t_nap", rs.getLong("t_nap"));
                            data.put("t_rut", rs.getLong("t_rut"));
                            data.put("nap_times", rs.getInt("nap_times"));
                            data.put("rut_times", rs.getInt("rut_times"));
                            data.put("parrentUser", rs.getString("parrentUser") != null ? rs.getString("parrentUser") : "");
                            data.put("referral_code", rs.getString("referral_code") != null ? rs.getString("referral_code") : "");
                            data.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                            data.put("email", rs.getString("email") != null ? rs.getString("email") : "");
                            data.put("create_time", rs.getString("create_time") != null ? rs.getString("create_time") : "");
                            data.put("last_login", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                            data.put("status", rs.getInt("status"));

                            // Get agent transfer summary
                            long totalSent = 0, totalReceived = 0;
                            String agentSql = "SELECT " +
                                    "COALESCE(SUM(CASE WHEN nick_name_send = ? THEN money_send ELSE 0 END), 0) as total_sent, " +
                                    "COALESCE(SUM(CASE WHEN nick_name_receive = ? THEN money_receive ELSE 0 END), 0) as total_received " +
                                    "FROM log_tranfer_agent WHERE nick_name_send = ? OR nick_name_receive = ?";
                            try (PreparedStatement ps2 = conn.prepareStatement(agentSql)) {
                                ps2.setString(1, nickname);
                                ps2.setString(2, nickname);
                                ps2.setString(3, nickname);
                                ps2.setString(4, nickname);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) {
                                        totalSent = rs2.getLong("total_sent");
                                        totalReceived = rs2.getLong("total_received");
                                    }
                                }
                            }
                            data.put("total_agent_sent", totalSent);
                            data.put("total_agent_received", totalReceived);

                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("data", data);
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "1002");
                            response.put("message", "User not found");
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("DetailUserOfAgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

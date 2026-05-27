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
import java.sql.Timestamp;

/**
 * c=711 - Admin OTP Action: cancel user's 2FA/OTP security or list pending OTP requests.
 * Params: te (target nick_name), ad (admin username) for cancel action.
 *         No params = list users with active OTP.
 */
public class AdminOtpProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String target = request.getParameter("te");
            String adminUser = request.getParameter("ad");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                if (target != null && !target.isEmpty()) {
                    // Cancel user's OTP security: reset login_otp to -1, clear security_time and otp_sender
                    String sql = "UPDATE users SET login_otp = -1, security_time = NULL, otp_sender = NULL " +
                            "WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, target);
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            response.put("success", true);
                            response.put("errorCode", "0");
                            logger.info("Admin [" + (adminUser != null ? adminUser : "unknown") + "] cancelled OTP for user: " + target);
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "1002");
                            response.put("message", "User not found");
                        }
                    }
                } else {
                    // List users with active OTP (login_otp != -1 and security_time is set)
                    JSONArray dataArray = new JSONArray();
                    String sql = "SELECT id, nick_name, user_name, mobile, login_otp, security_time, otp_sender " +
                            "FROM users WHERE login_otp != -1 AND security_time IS NOT NULL " +
                            "ORDER BY security_time DESC LIMIT 100";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                JSONObject item = new JSONObject();
                                item.put("id", rs.getLong("id"));
                                item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                                item.put("user_name", rs.getString("user_name") != null ? rs.getString("user_name") : "");
                                item.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                                item.put("login_otp", rs.getLong("login_otp"));
                                Timestamp secTime = rs.getTimestamp("security_time");
                                item.put("security_time", secTime != null ? secTime.toString() : "");
                                item.put("otp_sender", rs.getString("otp_sender") != null ? rs.getString("otp_sender") : "");
                                dataArray.put(item);
                            }
                        }
                    }
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", dataArray);
                }
            }
        } catch (Exception e) {
            logger.error("AdminOtpProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

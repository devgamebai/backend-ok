package com.vinplay.api.backend.processors.user;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GetUserDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            if (AdminUserSupport.requireAdmin(request, response) == null) {
                return response.toString();
            }
            String id = request.getParameter("id");

            if (!AdminUserSupport.isNotBlank(id)) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "id is required");
                return response.toString();
            }
            String validationError = AdminUserSupport.validateInteger(id, "id");
            if (validationError != null) {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", validationError);
                return response.toString();
            }

            try (Connection conn = AdminUserSupport.getUserConnection()) {
                String sql = "SELECT u.*, ua.nickname AS parent_agent_nickname, ua.code AS parent_agent_code "
                        + "FROM users u "
                        + "LEFT JOIN vinplay_admin.useragent ua ON ua.id = u.parent_agent_id "
                        + "WHERE u.id = ?"
                        + " LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, Long.parseLong(id.trim()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            response.put("success", false);
                            response.put("errorCode", "1002");
                            response.put("message", "User not found");
                            return response.toString();
                        }

                        JSONObject data = new JSONObject();
                        data.put("id", rs.getLong("id"));
                        data.put("user_name", safe(rs.getString("user_name")));
                        data.put("nick_name", safe(rs.getString("nick_name")));
                        data.put("email", safe(rs.getString("email")));
                        data.put("google_id", safe(rs.getString("google_id")));
                        data.put("facebook_id", safe(rs.getString("facebook_id")));
                        data.put("mobile", safe(rs.getString("mobile")));
                        data.put("identification", safe(rs.getString("identification")));
                        data.put("avatar", rs.getInt("avatar"));
                        data.put("birthday", safe(rs.getString("birthday")));
                        data.put("gender", rs.getObject("gender") == null ? 0 : rs.getInt("gender"));
                        data.put("address", safe(rs.getString("address")));
                        data.put("vin", rs.getLong("vin"));
                        data.put("xu", 0L);
                        data.put("vin_total", 0L);
                        data.put("xu_total", 0L);
                        data.put("safe", 0L);
                        data.put("recharge_money", 0L);
                        data.put("vip_point", 0);
                        data.put("vip_point_save", 0);
                        data.put("money_vp", 0);
                        data.put("dai_ly", rs.getInt("dai_ly"));
                        data.put("status", rs.getInt("status"));
                        data.put("security_time", safe(rs.getString("security_time")));
                        data.put("login_otp", rs.getLong("login_otp"));
                        data.put("is_bot", rs.getInt("is_bot"));
                        data.put("update_pw_time", safe(rs.getString("update_pw_time")));
                        data.put("client", safe(rs.getString("client")));
                        data.put("is_verify_mobile", rs.getObject("is_verify_mobile") == null ? 0 : rs.getInt("is_verify_mobile"));
                        data.put("referral_code", safe(rs.getString("referral_code")));
                        data.put("t_nap", rs.getLong("t_nap"));
                        data.put("t_rut", rs.getLong("t_rut"));
                        data.put("rut_times", rs.getInt("rut_times"));
                        data.put("nap_times", rs.getInt("nap_times"));
                        data.put("last_login", safe(rs.getString("last_login")));
                        data.put("usertype", rs.getInt("usertype"));
                        data.put("telegram_id", safe(rs.getString("telegram_id")));
                        data.put("parrentUser", safe(rs.getString("parrentUser")));
                        data.put("otp_sender", "");
                        data.put("is_live", 0);
                        data.put("create_time", safe(rs.getString("create_time")));
                        data.put("parent_agent_id", rs.getObject("parent_agent_id") == null ? 0 : rs.getInt("parent_agent_id"));
                        data.put("parent_agent_nickname", safe(rs.getString("parent_agent_nickname")));
                        data.put("parent_agent_code", safe(rs.getString("parent_agent_code")));

                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("data", data);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("GetUserDetailProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", e.getMessage());
        }
        return response.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

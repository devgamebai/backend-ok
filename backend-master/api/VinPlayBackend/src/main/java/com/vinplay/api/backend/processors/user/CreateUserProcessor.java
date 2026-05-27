package com.vinplay.api.backend.processors.user;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CreateUserProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String adminActor = AdminUserSupport.requireAdmin(request, response);
            if (adminActor == null) {
                return response.toString();
            }

            String userName = AdminUserSupport.nullableTrim(request.getParameter("un"));
            String nickName = AdminUserSupport.nullableTrim(request.getParameter("nn"));
            String password = request.getParameter("pw");

            if (!AdminUserSupport.isNotBlank(userName) || !AdminUserSupport.isNotBlank(password)) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "un and pw are required");
                return response.toString();
            }

            if (!AdminUserSupport.isNotBlank(nickName)) {
                nickName = userName;
            }

            String validationError = AdminUserSupport.validateUserName(userName);
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validatePassword(password);
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateNickName(nickName);
            if (validationError != null) {
                return error(response, "4002", validationError);
            }

            String email = AdminUserSupport.nullableTrim(request.getParameter("email"));
            String mobile = AdminUserSupport.nullableTrim(request.getParameter("mobile"));
            String address = AdminUserSupport.nullableTrim(request.getParameter("address"));
            String identification = AdminUserSupport.nullableTrim(request.getParameter("identification"));
            String referralCode = AdminUserSupport.nullableTrim(request.getParameter("referral_code"));
            String parentAgentId = AdminUserSupport.nullableTrim(request.getParameter("parent_agent_id"));
            String isBotRaw = request.getParameter("is_bot");
            String daiLyRaw = request.getParameter("dai_ly");
            String isVerifyMobileRaw = request.getParameter("is_verify_mobile");
            String isLiveRaw = request.getParameter("is_live");

            validationError = AdminUserSupport.validateEmail(email);
            if (validationError != null) {
                return error(response, "4003", validationError);
            }
            validationError = AdminUserSupport.validateMobile(mobile);
            if (validationError != null) {
                return error(response, "4004", validationError);
            }
            validationError = AdminUserSupport.validateIdentification(identification);
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateAddress(address);
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateTinyIntFlag(isBotRaw, "is_bot");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateZeroOnlyFlag(daiLyRaw, "dai_ly");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateTinyIntFlag(isVerifyMobileRaw, "is_verify_mobile");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateUnsupportedField(isLiveRaw, "is_live");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }

            int isBot = AdminUserSupport.parseTinyInt(isBotRaw, 0);
            int daiLy = AdminUserSupport.parseTinyInt(daiLyRaw, 0);
            int isVerifyMobile = AdminUserSupport.parseTinyInt(isVerifyMobileRaw, 0);
            try (Connection conn = AdminUserSupport.getUserConnection()) {
                if (AdminUserSupport.existsByUserName(conn, userName, null)) {
                    return error(response, "4005", "Username already exists");
                }
                if (AdminUserSupport.existsByUserName(conn, nickName, null)) {
                    return error(response, "4006", "Nickname already exists");
                }
                if (AdminUserSupport.existsByNickName(conn, nickName, null)) {
                    return error(response, "4006", "Nickname already exists");
                }

                AdminUserSupport.AgentRef agentRef;
                try {
                    agentRef = AdminUserSupport.resolveBinding(conn, referralCode, parentAgentId, false);
                } catch (IllegalArgumentException e) {
                    return error(response, "4007", e.getMessage());
                }

                String insertSql = "INSERT INTO users (user_name, nick_name, password, email, mobile, identification, address, "
                        + "vin, xu, vin_total, xu_total, safe, 0 AS recharge_money, vip_point, vip_point_save, money_vp, "
                        + "dai_ly, status, create_time, is_bot, is_verify_mobile, referral_code, t_nap, t_rut, rut_times, nap_times, "
                        + "parrentUser, parent_agent_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, ?, 0, NOW(), ?, ?, ?, 0, 0, 0, 0, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    int idx = 1;
                    ps.setString(idx++, userName);
                    ps.setString(idx++, nickName);
                    ps.setString(idx++, AdminUserSupport.normalizePassword(password));
                    ps.setString(idx++, email);
                    ps.setString(idx++, mobile);
                    ps.setString(idx++, identification);
                    ps.setString(idx++, address);
                    ps.setInt(idx++, daiLy);
                    ps.setInt(idx++, isBot);
                    ps.setInt(idx++, isVerifyMobile);
                    ps.setString(idx++, agentRef == null ? null : agentRef.code);
                    ps.setString(idx++, agentRef == null ? null : agentRef.nickname);
                    if (agentRef == null || agentRef.id == null) {
                        ps.setNull(idx, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(idx, agentRef.id);
                    }
                    ps.executeUpdate();

                    long userId = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            userId = rs.getLong(1);
                        }
                    }

                    JSONObject data = new JSONObject();
                    data.put("id", userId);
                    data.put("user_name", userName);
                    data.put("nick_name", nickName);
                    data.put("referral_code", agentRef == null ? "" : agentRef.code);
                    data.put("parent_agent_id", agentRef == null || agentRef.id == null ? 0 : agentRef.id);
                    data.put("parent_agent_nickname", agentRef == null ? "" : agentRef.nickname);

                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data);
                    AdminUserSupport.logUserCrudAction(
                            adminActor,
                            "Create user",
                            userName,
                            "created user id=" + userId + ", nick=" + nickName,
                            "1");
                }
            }
        } catch (Exception e) {
            logger.error("CreateUserProcessor error", e);
            return error(response, "9999", e.getMessage());
        }
        return response.toString();
    }

    private String error(JSONObject response, String code, String message) {
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        return response.toString();
    }
}

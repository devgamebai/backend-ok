package com.vinplay.api.backend.processors.user;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class UpdateUserProcessor implements BaseProcessor<HttpServletRequest, String> {
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
            String id = request.getParameter("id");

            if (!AdminUserSupport.isNotBlank(id)) {
                return error(response, "4001", "id is required");
            }
            String validationError = AdminUserSupport.validateInteger(id, "id");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }

            try (Connection conn = AdminUserSupport.getUserConnection()) {
                AdminUserSupport.UserRef userRef = AdminUserSupport.findUser(conn, id, null, null);
                if (userRef == null) {
                    return error(response, "1002", "User not found");
                }

                String newNickName = AdminUserSupport.nullableTrim(request.getParameter("nn_new"));
                String email = request.getParameter("email");
                String mobile = request.getParameter("mobile");
                String address = request.getParameter("address");
                String identification = request.getParameter("identification");
                String referralCode = request.getParameter("referral_code");
                String parentAgentId = request.getParameter("parent_agent_id");
                String isVerifyMobile = request.getParameter("is_verify_mobile");
                String isLive = request.getParameter("is_live");
                String daiLy = request.getParameter("dai_ly");
                String status = request.getParameter("status");
                String clearAgent = request.getParameter("clear_agent");

                if (AdminUserSupport.isNotBlank(newNickName)) {
                    validationError = AdminUserSupport.validateNickName(newNickName);
                    if (validationError != null) {
                        return error(response, "4002", validationError);
                    }
                    if (AdminUserSupport.existsByUserName(conn, newNickName, userRef.id)) {
                        return error(response, "4006", "Nickname already exists");
                    }
                    if (AdminUserSupport.existsByNickName(conn, newNickName, userRef.id)) {
                        return error(response, "4006", "Nickname already exists");
                    }
                }

                validationError = AdminUserSupport.validateEmail(AdminUserSupport.nullableTrim(email));
                if (validationError != null) {
                    return error(response, "4003", validationError);
                }
                validationError = AdminUserSupport.validateMobile(AdminUserSupport.nullableTrim(mobile));
                if (validationError != null) {
                    return error(response, "4004", validationError);
                }
                validationError = AdminUserSupport.validateIdentification(AdminUserSupport.nullableTrim(identification));
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }
                validationError = AdminUserSupport.validateAddress(AdminUserSupport.nullableTrim(address));
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }
                validationError = AdminUserSupport.validateTinyIntFlag(isVerifyMobile, "is_verify_mobile");
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }
                validationError = AdminUserSupport.validateUnsupportedField(isLive, "is_live");
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }
                validationError = AdminUserSupport.validateZeroOnlyFlag(daiLy, "dai_ly");
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }
                validationError = AdminUserSupport.validateUserStatus(status);
                if (validationError != null) {
                    return error(response, "4002", validationError);
                }

                boolean touchBinding = AdminUserSupport.isNotBlank(referralCode)
                        || AdminUserSupport.isNotBlank(parentAgentId)
                        || "1".equals(clearAgent);

                AdminUserSupport.AgentRef agentRef = null;
                if (touchBinding && !"1".equals(clearAgent)) {
                    try {
                        agentRef = AdminUserSupport.resolveBinding(conn, referralCode, parentAgentId, false);
                    } catch (IllegalArgumentException e) {
                        return error(response, "4007", e.getMessage());
                    }
                }

                StringBuilder sql = new StringBuilder("UPDATE users SET ");
                List<Object> params = new ArrayList<Object>();
                boolean changed = false;

                if (AdminUserSupport.isNotBlank(newNickName)) {
                    sql.append("nick_name = ?");
                    params.add(newNickName);
                    changed = true;
                }
                if (email != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("email = ?");
                    params.add(AdminUserSupport.nullableTrim(email));
                    changed = true;
                }
                if (mobile != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("mobile = ?");
                    params.add(AdminUserSupport.nullableTrim(mobile));
                    changed = true;
                }
                if (address != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("address = ?");
                    params.add(AdminUserSupport.nullableTrim(address));
                    changed = true;
                }
                if (identification != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("identification = ?");
                    params.add(AdminUserSupport.nullableTrim(identification));
                    changed = true;
                }
                if (isVerifyMobile != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("is_verify_mobile = ?");
                    params.add(Integer.parseInt(isVerifyMobile.trim()));
                    changed = true;
                }
                if (daiLy != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("dai_ly = ?");
                    params.add(Integer.parseInt(daiLy.trim()));
                    changed = true;
                }
                if (status != null) {
                    sqlAppendComma(sql, changed);
                    sql.append("status = ?");
                    params.add(Integer.parseInt(status.trim()));
                    changed = true;
                }
                if (touchBinding) {
                    sqlAppendComma(sql, changed);
                    if ("1".equals(clearAgent)) {
                        sql.append("referral_code = NULL, parent_agent_id = NULL, parrentUser = NULL");
                    } else {
                        sql.append("referral_code = ?, parent_agent_id = ?, parrentUser = ?");
                        params.add(agentRef == null ? null : agentRef.code);
                        params.add(agentRef == null || agentRef.id == null ? null : agentRef.id);
                        params.add(agentRef == null ? null : agentRef.nickname);
                    }
                    changed = true;
                }

                if (!changed) {
                    return error(response, "4001", "No update field provided");
                }

                sql.append(" WHERE id = ?");
                params.add(userRef.id);

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int idx = 1;
                    for (Object value : params) {
                        ps.setObject(idx++, value);
                    }
                    int rows = ps.executeUpdate();
                    if (rows <= 0) {
                        return error(response, "1002", "User not found");
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("message", "User updated");
                AdminUserSupport.logUserCrudAction(
                        adminActor,
                        "Update user",
                        userRef.userName,
                        "updated user id=" + userRef.id + (AdminUserSupport.isNotBlank(newNickName) ? ", nn_new=" + newNickName : ""),
                        "1");
            }
        } catch (Exception e) {
            logger.error("UpdateUserProcessor error", e);
            return error(response, "9999", e.getMessage());
        }
        return response.toString();
    }

    private static void sqlAppendComma(StringBuilder sql, boolean changed) {
        if (changed) {
            sql.append(", ");
        }
    }

    private String error(JSONObject response, String code, String message) {
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        return response.toString();
    }
}

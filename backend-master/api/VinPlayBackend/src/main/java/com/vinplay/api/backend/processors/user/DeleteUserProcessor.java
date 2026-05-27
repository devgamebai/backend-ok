package com.vinplay.api.backend.processors.user;

import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;

public class DeleteUserProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            String targetNickName;
            try (Connection conn = AdminUserSupport.getUserConnection()) {
                AdminUserSupport.UserRef ref = AdminUserSupport.findUser(conn, id, null, null);
                if (ref == null) {
                    return error(response, "1002", "User not found");
                }
                targetNickName = ref.nickName;
            }

            boolean ok = new SecurityServiceImpl().updateStatusUser(targetNickName, 0, "1");
            if (!ok) {
                return error(response, "1003", "Soft delete failed");
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", "User deactivated (soft delete by ban login)");
            response.put("nick_name", targetNickName);
            AdminUserSupport.logUserCrudAction(
                    adminActor,
                    "Delete user",
                    targetNickName,
                    "soft delete by ban login",
                    "1");
        } catch (Exception e) {
            logger.error("DeleteUserProcessor error", e);
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

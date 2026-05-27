package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.UserForAdminService;
import com.vinplay.usercore.service.impl.UserForAdminServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

public class ChangePasswordUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() ||
           (!serPath.equals("/api_agent") && !serPath.equals("/api_backend"))){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }
        
        String nick_name = request.getParameter("nn");
        String old_password = request.getParameter("op");
        String new_password = request.getParameter("np");

        // Admin CMS sends id + ps (reset password without old password)
        String idParam = request.getParameter("id");
        String psParam = request.getParameter("ps");
        boolean isAdminReset = (idParam != null && !idParam.isEmpty() && psParam != null && !psParam.isEmpty());

        if (isAdminReset) {
            // Admin reset: look up nickname by agent id, store as MD5 (login sends MD5)
            try {
                AgentDAO dao = new AgentDAOImpl();
                String nickname = dao.getNicknameById(Integer.parseInt(idParam));
                if (nickname == null || nickname.isEmpty()) {
                    return BaseResponse.error("-1", "Không tìm thấy agent với id=" + idParam);
                }
                // Hash password to MD5 if not already hashed (32 hex chars = already MD5)
                String hashedPs = psParam;
                if (psParam.length() != 32 || !psParam.matches("[a-fA-F0-9]+")) {
                    hashedPs = md5(psParam);
                }
                String status = dao.resetPasswordUserAgent(nickname, hashedPs);
                if ("success".equals(status)) {
                    return BaseResponse.success(null, 1);
                }
                return BaseResponse.error("-1", status);
            } catch (Exception e) {
                return BaseResponse.error("-1", e.getMessage());
            }
        }

        // Agent self-change: nn + op + np
        if(nick_name == null || nick_name.trim().isEmpty()) {
            return BaseResponse.error("-1", "Thiếu nickname");
        }

        AgentDAO dao = new AgentDAOImpl();
        try {
            String status = dao.changePasswordUserAgent(nick_name, old_password, new_password);
            switch (status) {
                case "success":
                    return BaseResponse.success(null, 1);
                case "not_found":
                    return BaseResponse.error("-1", "Không tìm thấy user");
                case "not_same":
                    return BaseResponse.error("-1", "Mật khẩu cũ nhập sai");
                default:
                    return BaseResponse.error("-1", status);
            }
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 error", e);
        }
    }
}
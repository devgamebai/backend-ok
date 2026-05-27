package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class BulkClearOverridesProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");
        String userIds = request.getParameter("users"); // comma-separated
        String gameCode = request.getParameter("game");

        if (userIds == null || userIds.isEmpty()) return "{\"success\":false,\"message\":\"No users\"}";

        String[] users = userIds.split(",");
        RtpConfigService svc = new RtpConfigService();
        int successCount = 0;

        for (String u : users) {
            try {
                long userId = Long.parseLong(u.trim());
                if (svc.deleteUserOverride(userId, gameCode, admin)) {
                    successCount++;
                }
            } catch (Exception e) {}
        }
        return "{\"success\":true,\"errorCode\":0,\"message\":\"Cleared for " + successCount + " users\"}";
    }
}

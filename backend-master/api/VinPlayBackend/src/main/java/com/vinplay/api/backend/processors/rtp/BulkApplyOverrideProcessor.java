package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;

public class BulkApplyOverrideProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");
        String userIds = request.getParameter("users"); // comma-separated
        String gameCode = request.getParameter("game");
        String reason = request.getParameter("reason");
        double pct = Double.parseDouble(request.getParameter("pct"));
        String exp = request.getParameter("expiresAt"); // optional

        if (userIds == null || userIds.isEmpty()) return "{\"success\":false,\"message\":\"No users\"}";

        String[] users = userIds.split(",");
        RtpConfigService svc = new RtpConfigService();
        int successCount = 0;

        for (String u : users) {
            try {
                long userId = Long.parseLong(u.trim());
                // Bug fix: correct param order is (userId, gameCode, pct, reason, expiresAt, actor)
                if (svc.setUserOverride(userId, gameCode, pct, reason, exp, admin)) {
                    successCount++;
                }
            } catch (Exception e) { logger.error("BulkApply error uid=" + u, e); }
        }
        return "{\"success\":true,\"errorCode\":0,\"message\":\"Applied to " + successCount + " users\"}";
    }
}

package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class SetUserSessionRtpProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");
        String gameCode = request.getParameter("game");
        long userId = Long.parseLong(request.getParameter("userId"));
        double pct = Double.parseDouble(request.getParameter("pct"));

        try {
            RtpConfigService svc = new RtpConfigService();
            if (svc.setSessionOverride(userId, gameCode, pct, admin)) {
                return "{\"success\":true,\"errorCode\":0,\"message\":\"Session Override active\"}";
            }
        } catch (Exception e) {}
        
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed\"}";
    }
}

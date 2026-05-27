package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class DisableGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");
        String gameCode = request.getParameter("game");
        boolean disabled = Boolean.parseBoolean(request.getParameter("disabled"));

        if (gameCode == null || gameCode.isEmpty()) return "{\"success\":false,\"message\":\"Missing game\"}";

        try {
            RtpConfigService svc = new RtpConfigService();
            if (svc.setGameDisabled(gameCode, disabled, admin)) {
                return "{\"success\":true,\"errorCode\":0,\"message\":\"Game Disabled: " + disabled + "\"}";
            }
        } catch (Exception e) {}
        
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed\"}";
    }
}

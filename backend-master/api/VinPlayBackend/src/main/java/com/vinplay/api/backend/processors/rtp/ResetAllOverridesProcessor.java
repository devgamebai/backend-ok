package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import javax.servlet.http.HttpServletRequest;

public class ResetAllOverridesProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");

        try {
            RtpConfigService svc = new RtpConfigService();
            boolean res = svc.resetAllUserOverrides(admin);
            if (res) return "{\"success\":true,\"errorCode\":0,\"message\":\"All overrides cleared\"}";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed\"}";
    }
}

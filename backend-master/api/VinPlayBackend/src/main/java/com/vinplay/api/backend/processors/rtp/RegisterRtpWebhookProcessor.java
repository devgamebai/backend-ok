package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpWebhookService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class RegisterRtpWebhookProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String url = request.getParameter("url");
        if (url != null && !url.isEmpty()) {
            RtpWebhookService.registerWebhook(url);
            return "{\"success\":true,\"errorCode\":0,\"message\":\"Webhook registered\"}";
        }
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Invalid URL\"}";
    }
}

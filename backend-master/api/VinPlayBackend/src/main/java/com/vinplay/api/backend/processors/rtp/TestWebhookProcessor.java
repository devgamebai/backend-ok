package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpWebhookService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class TestWebhookProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        RtpWebhookService.testPayload();
        return "{\"success\":true,\"errorCode\":0,\"message\":\"Test queued\"}";
    }
}

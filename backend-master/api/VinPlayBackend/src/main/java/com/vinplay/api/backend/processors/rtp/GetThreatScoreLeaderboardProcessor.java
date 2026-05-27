package com.vinplay.api.backend.processors.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.rtp.PnlService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import javax.servlet.http.HttpServletRequest;

public class GetThreatScoreLeaderboardProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        int limit = request.getParameter("limit") != null ? Integer.parseInt(request.getParameter("limit")) : 50;

        try {
            PnlService svc = new PnlService();
            ObjectMapper mapper = new ObjectMapper();
            return "{\"success\":true,\"errorCode\":0,\"data\":" + mapper.writeValueAsString(svc.getThreatScoreLeaderboard(limit)) + "}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"errorCode\":1,\"message\":\"Exception occurred\"}";
        }
    }
}

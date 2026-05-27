package com.vinplay.api.backend.processors.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.rtp.PnlService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class GetPnlDistributionProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        String window = param.get().getParameter("window") != null ? param.get().getParameter("window") : "D7";
        try {
            return "{\"success\":true,\"data\":" + new ObjectMapper().writeValueAsString(new PnlService().getPnlDistribution(window)) + "}";
        } catch (Exception e) { return "{\"success\":false}"; }
    }
}

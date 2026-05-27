package com.vinplay.api.backend.processors.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.rtp.RtpExperimentService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.rtp.RtpExperiment;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class ListExperimentsProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        RtpExperimentService svc = new RtpExperimentService();
        List<RtpExperiment> rules = svc.listExperiments();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return "{\"success\":true,\"errorCode\":0,\"data\":" + mapper.writeValueAsString(rules) + "}";
        } catch(Exception e) {
            return "{\"success\":false,\"errorCode\":1,\"message\":\"Exception\"}";
        }
    }
}

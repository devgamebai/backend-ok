package com.vinplay.api.backend.processors.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.rtp.RtpScheduleService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.rtp.GameRtpSchedule;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class ListRtpSchedulesProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        RtpScheduleService svc = new RtpScheduleService();
        List<GameRtpSchedule> rules = svc.listActiveSchedules();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return "{\"success\":true,\"errorCode\":0,\"data\":" + mapper.writeValueAsString(rules) + "}";
        } catch(Exception e) {
            return "{\"success\":false,\"errorCode\":1,\"message\":\"Exception\"}";
        }
    }
}

package com.vinplay.api.backend.processors.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.dal.rtp.RtpScheduleService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class ExportRtpConfigSnapshotProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        try {
            RtpConfigService configService = new RtpConfigService();
            RtpScheduleService scheduleService = new RtpScheduleService();

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("games", configService.listGameConfigs());
            snapshot.put("overrides", configService.listUserOverrides(null, null, 1, 10000));
            snapshot.put("schedules", scheduleService.listActiveSchedules());
            snapshot.put("emergency_rtp", configService.getEmergencyRtp());

            ObjectMapper mapper = new ObjectMapper();
            return "{\"success\":true,\"errorCode\":0,\"data\":" + mapper.writeValueAsString(snapshot) + "}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"errorCode\":1,\"message\":\"Exception occurred\"}";
        }
    }
}

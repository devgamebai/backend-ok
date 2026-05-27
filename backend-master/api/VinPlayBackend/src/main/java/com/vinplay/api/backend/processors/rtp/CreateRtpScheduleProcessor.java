package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpScheduleService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.rtp.GameRtpSchedule;

import javax.servlet.http.HttpServletRequest;

public class CreateRtpScheduleProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String gameCode = request.getParameter("game");
        String cronExpr = request.getParameter("cron");
        double winRatePct = Double.parseDouble(request.getParameter("pct"));
        int durationMin = Integer.parseInt(request.getParameter("duration"));
        String createdBy = request.getParameter("admin");
        String description = request.getParameter("desc");

        GameRtpSchedule s = new GameRtpSchedule();
        s.setGameCode(gameCode);
        s.setCronExpr(cronExpr);
        s.setWinRatePct(winRatePct);
        s.setDurationMin(durationMin);
        s.setActive(1);
        s.setCreatedBy(createdBy);
        s.setDescription(description);

        RtpScheduleService svc = new RtpScheduleService();
        boolean res = svc.createSchedule(s);

        if (res) return "{\"success\":true,\"errorCode\":0,\"message\":\"Created\"}";
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed to create\"}";
    }
}

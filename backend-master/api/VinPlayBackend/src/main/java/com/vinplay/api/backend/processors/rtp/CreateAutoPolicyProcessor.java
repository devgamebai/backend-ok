package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpAutoTargeterService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.rtp.RtpAutoPolicy;

import javax.servlet.http.HttpServletRequest;

public class CreateAutoPolicyProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String policyName = request.getParameter("name");
        long maxWinAmount = Long.parseLong(request.getParameter("maxWin"));
        int timeWindowMin = Integer.parseInt(request.getParameter("window"));
        double actionRtpPct = Double.parseDouble(request.getParameter("actionPct"));
        int actionDuration = Integer.parseInt(request.getParameter("actionDuration"));
        String desc = request.getParameter("desc");

        RtpAutoPolicy p = new RtpAutoPolicy();
        p.setPolicyName(policyName);
        p.setMaxWinAmount(maxWinAmount);
        p.setTimeWindowMin(timeWindowMin);
        p.setActionRtpPct(actionRtpPct);
        p.setActionDuration(actionDuration);
        p.setIsActive(1);
        p.setDescription(desc);

        RtpAutoTargeterService svc = new RtpAutoTargeterService();
        boolean res = svc.createPolicy(p);

        if (res) return "{\"success\":true,\"errorCode\":0,\"message\":\"Created policy\"}";
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed to create policy\"}";
    }
}

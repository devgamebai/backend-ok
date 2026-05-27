package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpExperimentService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.rtp.RtpExperiment;

import javax.servlet.http.HttpServletRequest;

public class CreateRtpExperimentProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String name = request.getParameter("name");
        String gameCode = request.getParameter("game");
        String bucketJson = request.getParameter("buckets"); // e.g. [{"bucket":"A","pct":80,"share":0.5}]
        String status = request.getParameter("status"); // DRAFT or RUNNING
        String admin = request.getParameter("admin");

        try {
            RtpExperiment exp = new RtpExperiment();
            exp.setName(name);
            exp.setGameCode(gameCode);
            exp.setBucketJson(bucketJson);
            exp.setStatus(status);
            exp.setCreatedBy(admin);

            RtpExperimentService svc = new RtpExperimentService();
            boolean res = svc.createExperiment(exp);
            if (res) return "{\"success\":true,\"errorCode\":0,\"message\":\"Experiment Created\"}";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{\"success\":false,\"errorCode\":1,\"message\":\"Failed\"}";
    }
}

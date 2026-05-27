package com.vinplay.api.backend.processors.rtp;

import com.vinplay.dal.rtp.RtpConfigService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class BulkUpdateGameRateProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String admin = request.getParameter("admin");
        String games = request.getParameter("games"); // comma-separated
        double pct = Double.parseDouble(request.getParameter("pct"));
        String note = request.getParameter("note");

        if (games == null || games.isEmpty()) return "{\"success\":false,\"message\":\"No games\"}";

        String[] gameArr = games.split(",");
        RtpConfigService svc = new RtpConfigService();
        int successCount = 0;

        for (String g : gameArr) {
            try {
                if (svc.updateGameConfig(g.trim(), pct, note, admin)) {
                    successCount++;
                }
            } catch (Exception e) {}
        }
        return "{\"success\":true,\"errorCode\":0,\"message\":\"Updated " + successCount + " games\"}";
    }
}

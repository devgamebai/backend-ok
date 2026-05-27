package com.vinplay.api.backend.processors.rebate;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9760 — Manually trigger the commission cron job.
 * Also starts the scheduler if not already running.
 */
public class TriggerCommissionCronProcessor implements BaseProcessor<HttpServletRequest, String> {

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            // Ensure cron is started (idempotent)
            CommissionCronJob.start(60); // every 60 minutes

            String result = CommissionCronJob.run();
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("result", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Error: " + e.getMessage());
        }
        return response.toString();
    }
}

package com.vinplay.api.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Portal c=3052 — Agent (F0) sets share percentage for F1.
 * Params: agent_user_id or nn, share_percentage
 */
public class SetRebateShareProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        // Phase 1: Agency self-service rate setting is disabled. Only admin CMS can set rates.
        response.put("success", false);
        response.put("errorCode", "4010");
        response.put("message", "Rate setting disabled — contact admin");
        if (true) return response.toString();

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            Integer agentUserId = RebatePortalAuthHelper.resolveAuthorizedAgentId(request, response);
            if (agentUserId == null) {
                return response.toString();
            }

            String sharePctStr = request.getParameter("share_percentage");
            if (sharePctStr == null || sharePctStr.isEmpty()) {
                response.put("success", false);
                response.put("message", "share_percentage is required");
                return response.toString();
            }

            double sharePct;
            try {
                sharePct = Double.parseDouble(sharePctStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("message", "Invalid share_percentage");
                return response.toString();
            }

            if (sharePct < 0) {
                response.put("success", false);
                response.put("message", "share_percentage must be >= 0");
                return response.toString();
            }

            boolean ok = RebateService.setSharePercentage(agentUserId, sharePct);
            if (!ok) {
                response.put("success", false);
                response.put("message", "Failed: share_percentage must be <= rebate_percentage, or config not found");
                return response.toString();
            }

            response.put("success", true);
            logger.info("SetRebateShareProcessor agentUserId=" + agentUserId + " sharePct=" + sharePct);
        } catch (Exception e) {
            logger.error("SetRebateShareProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

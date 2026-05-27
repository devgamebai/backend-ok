package com.vinplay.api.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Portal c=3050 — Agent views rebate summary.
 * Params: agent_user_id or nn (nickname)
 * Returns: total volume, rebate %, earned, pending, paid
 */
public class GetAgentRebateSummaryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            Integer agentUserId = RebatePortalAuthHelper.resolveAuthorizedAgentId(request, response);
            if (agentUserId == null) {
                return response.toString();
            }

            Map<String, Object> summary = RebateService.getAgentSummary(agentUserId);
            if (summary.isEmpty()) {
                response.put("success", false);
                response.put("message", "No rebate data for this agent");
                return response.toString();
            }

            response.put("success", true);
            response.put("data", new JSONObject(summary));
        } catch (Exception e) {
            logger.error("GetAgentRebateSummaryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

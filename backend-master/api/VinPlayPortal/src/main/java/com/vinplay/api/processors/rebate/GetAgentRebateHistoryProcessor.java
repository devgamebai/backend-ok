package com.vinplay.api.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Portal c=3051 — Agent views rebate history and payout logs.
 * Params: agent_user_id or nn, date_from, date_to, page, limit
 */
public class GetAgentRebateHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            Integer agentUserId = RebatePortalAuthHelper.resolveAuthorizedAgentId(request, response);
            if (agentUserId == null) {
                return response.toString();
            }

            String dateFrom = request.getParameter("date_from");
            String dateTo = request.getParameter("date_to");
            int page = 1, limit = 20;
            try { page = Integer.parseInt(request.getParameter("page")); } catch (Exception ignored) {}
            try { limit = Integer.parseInt(request.getParameter("limit")); } catch (Exception ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            // Rebate logs
            List<Map<String, Object>> logs = RebateService.queryLogs(agentUserId, null, dateFrom, dateTo, null, null, page, limit);
            JSONArray logsArr = new JSONArray();
            for (Map<String, Object> l : logs) {
                logsArr.put(new JSONObject(l));
            }

            // Payout history
            List<Map<String, Object>> payouts = RebateService.getPayouts(agentUserId, page, limit);
            JSONArray payoutsArr = new JSONArray();
            for (Map<String, Object> p : payouts) {
                payoutsArr.put(new JSONObject(p));
            }

            response.put("success", true);
            response.put("rebate_logs", logsArr);
            response.put("payouts", payoutsArr);
            response.put("page", page);
            response.put("limit", limit);
        } catch (Exception e) {
            logger.error("GetAgentRebateHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

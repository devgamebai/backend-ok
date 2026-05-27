package com.vinplay.api.backend.processors.rebate;

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
 * c=9752 — Admin: query rebate logs with filters.
 * Params: agent_user_id, status, date_from, date_to, page, limit
 */
public class GetRebateLogsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            int agentUserId = RebateProcessorHelper.intParam(request, "agent_user_id", 0);
            String status = RebateProcessorHelper.strParam(request, "status");
            String dateFrom = RebateProcessorHelper.strParam(request, "date_from");
            String dateTo = RebateProcessorHelper.strParam(request, "date_to");
            int page = RebateProcessorHelper.intParam(request, "page", 1);
            int limit = RebateProcessorHelper.intParam(request, "limit", 20);
            String sort = RebateProcessorHelper.strParam(request, "sort");
            String dir = RebateProcessorHelper.strParam(request, "dir");

            Integer agentId = agentUserId > 0 ? agentUserId : null;
            List<Map<String, Object>> logs = RebateService.queryLogs(agentId, status, dateFrom, dateTo, sort, dir, page, limit);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> l : logs) {
                arr.put(new JSONObject(l));
            }
            response.put("success", true);
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);
        } catch (Exception e) {
            logger.error("GetRebateLogsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

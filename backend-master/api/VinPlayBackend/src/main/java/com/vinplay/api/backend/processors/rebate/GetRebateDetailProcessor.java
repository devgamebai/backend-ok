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
 * c=9751 — Admin: get rebate detail for a specific agent.
 * Params: agent_user_id (required)
 * Returns: agent config + F1 volume breakdown + rebate logs
 */
public class GetRebateDetailProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            int agentUserId = RebateProcessorHelper.intParam(request, "agent_user_id", -1);
            if (agentUserId <= 0) {
                response.put("success", false);
                response.put("message", "agent_user_id is required");
                return response.toString();
            }

            // Config
            Map<String, Object> config = RebateService.getConfig(agentUserId);
            if (config != null) {
                response.put("config", new JSONObject(config));
            }

            // F1 volume details
            List<Map<String, Object>> f1Details = RebateService.getF1VolumeDetails(agentUserId);
            JSONArray f1Arr = new JSONArray();
            for (Map<String, Object> d : f1Details) {
                f1Arr.put(new JSONObject(d));
            }
            response.put("f1_details", f1Arr);
            response.put("f1_count", f1Details.size());

            // Recent logs
            List<Map<String, Object>> logs = RebateService.queryLogs(agentUserId, null, null, null, null, null, 1, 20);
            JSONArray logsArr = new JSONArray();
            for (Map<String, Object> l : logs) {
                logsArr.put(new JSONObject(l));
            }
            response.put("recent_logs", logsArr);

            response.put("success", true);
        } catch (Exception e) {
            logger.error("GetRebateDetailProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

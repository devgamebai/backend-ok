package com.vinplay.api.backend.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * c=9756 — Admin: get config for a specific agent.
 * Params: agent_user_id (required)
 */
public class GetRebateConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            Map<String, Object> config = RebateService.getConfig(agentUserId);
            if (config == null) {
                response.put("success", false);
                response.put("message", "Config not found for agent_user_id=" + agentUserId);
                return response.toString();
            }

            response.put("success", true);
            response.put("data", new JSONObject(config));
        } catch (Exception e) {
            logger.error("GetRebateConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9755 — Admin: create or update rebate config for an agent.
 * Params: agent_user_id, agent_nickname, rebate_percentage (default 1.25),
 *         share_percentage (default 0), period_type (DAILY/WEEKLY/MONTHLY),
 *         min_volume (default 0), is_active (0/1)
 */
public class UpdateRebateConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            int agentUserId = RebateProcessorHelper.intParam(request, "agent_user_id", -1);
            String nickname = RebateProcessorHelper.strParam(request, "agent_nickname");

            if (agentUserId <= 0 || nickname == null) {
                response.put("success", false);
                response.put("message", "agent_user_id and agent_nickname are required");
                return response.toString();
            }

            double rebatePct = RebateProcessorHelper.doubleParam(request, "rebate_percentage", 1.25);
            double sharePct = RebateProcessorHelper.doubleParam(request, "share_percentage", 0);
            String periodType = RebateProcessorHelper.strParam(request, "period_type");
            if (periodType == null) periodType = "WEEKLY";
            long minVolume = RebateProcessorHelper.longParam(request, "min_volume", 0);
            boolean active = RebateProcessorHelper.intParam(request, "is_active", 1) == 1;

            // Validate
            if (rebatePct < 0 || rebatePct > 100) {
                response.put("success", false);
                response.put("message", "rebate_percentage must be 0-100");
                return response.toString();
            }
            if (sharePct < 0 || sharePct > rebatePct) {
                response.put("success", false);
                response.put("message", "share_percentage must be 0 to rebate_percentage");
                return response.toString();
            }

            boolean ok = RebateService.upsertConfig(agentUserId, nickname, rebatePct,
                    sharePct, periodType.toUpperCase(), minVolume, active);

            response.put("success", ok);
            if (!ok) response.put("message", "Upsert failed");

            logger.info("UpdateRebateConfigProcessor agentUserId=" + agentUserId
                    + " rebatePct=" + rebatePct + " sharePct=" + sharePct + " result=" + ok);
        } catch (Exception e) {
            logger.error("UpdateRebateConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

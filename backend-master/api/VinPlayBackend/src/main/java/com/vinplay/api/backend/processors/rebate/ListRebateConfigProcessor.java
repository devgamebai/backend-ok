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
 * c=9757 — Admin: list all rebate configs.
 * Params: active_only (0/1, default 0)
 */
public class ListRebateConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            boolean activeOnly = RebateProcessorHelper.intParam(request, "active_only", 0) == 1;

            List<Map<String, Object>> configs = RebateService.listConfigs(activeOnly);
            JSONArray arr = new JSONArray();
            for (Map<String, Object> c : configs) {
                arr.put(new JSONObject(c));
            }
            response.put("success", true);
            response.put("data", arr);
        } catch (Exception e) {
            logger.error("ListRebateConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

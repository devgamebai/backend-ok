package com.vinplay.api.backend.processors.cashback;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.cashback.CashbackService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Admin API: List all cashback (loss rebate) configurations.
 * Command ID: 9801
 *
 * Params: at (access token), active_only (optional, default false)
 */
public class ListCashbackConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String activeOnlyStr = request.getParameter("active_only");
            boolean activeOnly = "true".equalsIgnoreCase(activeOnlyStr) || "1".equals(activeOnlyStr);

            CashbackService service = new CashbackService();
            List<Map<String, Object>> configs = service.listConfigs(activeOnly);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : configs) {
                arr.put(new JSONObject(row));
            }

            response.put("success", true);
            response.put("data", arr);

        } catch (Exception e) {
            logger.error("ListCashbackConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

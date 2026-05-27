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
 * Admin API: Get cashback (loss rebate) changelog entries.
 * Command ID: 9815
 *
 * Params: at, entity_type (optional), entity_id (optional), page (default 1), limit (default 20, max 100)
 */
public class GetCashbackChangelogProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Parse parameters
            String entityType = request.getParameter("entity_type");
            String entityIdStr = request.getParameter("entity_id");
            long entityId = 0;
            if (entityIdStr != null && !entityIdStr.isEmpty()) {
                entityId = Long.parseLong(entityIdStr);
            }

            String pageStr = request.getParameter("page");
            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            if (page < 1) page = 1;

            String limitStr = request.getParameter("limit");
            int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 20;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            CashbackService service = new CashbackService();
            List<Map<String, Object>> changelog = service.getChangelog(entityType, entityId, page, limit);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : changelog) {
                arr.put(new JSONObject(row));
            }

            response.put("success", true);
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("GetCashbackChangelogProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

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
 * Admin API: List cashback (loss rebate) logs with filters and pagination.
 * Command ID: 9804
 *
 * Params: at, nick_name (optional), status (optional), date_from (optional),
 *         date_to (optional), config_id (optional), page (default 1), limit (default 20, max 100)
 */
public class ListCashbackLogsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = request.getParameter("aat");
            }

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

            // Parse filter parameters
            String nickName = request.getParameter("nick_name");
            String status = request.getParameter("status");
            String dateFrom = request.getParameter("date_from");
            String dateTo = request.getParameter("date_to");

            String configIdStr = request.getParameter("config_id");
            int configId = 0;
            if (configIdStr != null && !configIdStr.isEmpty()) {
                configId = Integer.parseInt(configIdStr);
            }

            String pageStr = request.getParameter("page");
            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            if (page < 1) page = 1;

            String limitStr = request.getParameter("limit");
            int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 20;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            CashbackService service = new CashbackService();
            List<Map<String, Object>> logs = service.queryLogs(nickName, status, dateFrom, dateTo,
                    configId, page, limit);
            int total = service.countLogs(nickName, status, dateFrom, dateTo, configId);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : logs) {
                arr.put(new JSONObject(row));
            }

            response.put("success", true);
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);

        } catch (Exception e) {
            logger.error("ListCashbackLogsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

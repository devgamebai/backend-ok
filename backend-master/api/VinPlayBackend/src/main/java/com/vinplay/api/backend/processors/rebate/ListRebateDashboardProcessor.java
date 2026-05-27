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
 * c=9750 — Admin dashboard: list agents + rebate stats.
 * Params: date_from, date_to, page (default 1), limit (default 20)
 */
public class ListRebateDashboardProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String dateFrom = request.getParameter("date_from");
            String dateTo = request.getParameter("date_to");
            int page = RebateProcessorHelper.intParam(request, "page", 1);
            int limit = RebateProcessorHelper.intParam(request, "limit", 20);

            // SUN-1108 Wave 2 Tier 4: response cache. Admin dashboard — cache
            // by (date range, paging). 30s for current period, 300s historical.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "9750", dateFrom, dateTo, page, limit);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(dateTo);

            List<Map<String, Object>> data = RebateService.getDashboard(dateFrom, dateTo, page, limit);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : data) {
                arr.put(new JSONObject(row));
            }
            response.put("success", true);
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);

            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;
        } catch (Exception e) {
            logger.error("ListRebateDashboardProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

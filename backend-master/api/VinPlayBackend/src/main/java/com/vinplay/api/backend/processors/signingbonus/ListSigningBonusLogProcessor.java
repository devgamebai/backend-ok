package com.vinplay.api.backend.processors.signingbonus;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.SigningBonusDao;
import com.vinplay.dal.dao.impl.SigningBonusDaoImpl;
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
 * Admin API: List signing bonus claim logs.
 * Command ID: 9763
 *
 * Params: at, nick_name (optional), date_from (optional), date_to (optional),
 *         page (default 1), limit (default 20, max 100)
 */
public class ListSigningBonusLogProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Parse filter parameters
            String nickName = request.getParameter("nick_name");
            String dateFrom = request.getParameter("date_from");
            String dateTo = request.getParameter("date_to");

            String pageStr = request.getParameter("page");
            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            if (page < 1) page = 1;

            String limitStr = request.getParameter("limit");
            int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 20;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            SigningBonusDao dao = new SigningBonusDaoImpl();
            List<Map<String, Object>> logs = dao.listBonusLogs(nickName, dateFrom, dateTo, page, limit);
            int total = dao.countBonusLogs(nickName, dateFrom, dateTo);

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
            logger.error("ListSigningBonusLogProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

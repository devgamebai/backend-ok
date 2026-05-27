package com.vinplay.api.backend.processors.slot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class ListForceSlotResultsProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Missing access token");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Invalid access token");
                return response.toString();
            }

            // Get all entries from cacheForceResult via DistCache
            com.vinplay.vbee.common.cache.DistCache<String, String> forceResultMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheForceResult", String.class);
            JSONArray dataArray = new JSONArray();

            for (Map.Entry<String, String> entry : forceResultMap.entrySet()) {
                try {
                    JSONObject entryObj = new JSONObject(entry.getValue());
                    entryObj.put("key", entry.getKey());
                    dataArray.put(entryObj);
                } catch (Exception e) {
                    logger.warn("ListForceSlotResults: failed to parse entry key=" + entry.getKey(), e);
                }
            }

            response.put("success", true);
            response.put("data", dataArray);
        } catch (Exception e) {
            logger.error("ListForceSlotResultsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }
}

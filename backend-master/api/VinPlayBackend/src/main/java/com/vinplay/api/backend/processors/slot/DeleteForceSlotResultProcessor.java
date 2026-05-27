package com.vinplay.api.backend.processors.slot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

public class DeleteForceSlotResultProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            String adminNickname = (String) tokenMap.get(accessToken);

            // Get key parameter - support both query param and JSON POST body
            String key = request.getParameter("key");

            if (key == null || key.isEmpty()) {
                try {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String body = sb.toString().trim();
                    if (!body.isEmpty()) {
                        JSONObject bodyJson = new JSONObject(body);
                        if (bodyJson.has("key")) {
                            key = bodyJson.getString("key");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("DeleteForceSlotResultProcessor: failed to parse JSON body", e);
                }
            }

            if (key == null || key.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Missing key parameter");
                return response.toString();
            }

            // Remove via DistCache (Hazelcast or Redis per routing flag)
            com.vinplay.vbee.common.cache.DistCache<String, String> forceResultMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheForceResult", String.class);
            String removed = forceResultMap.remove(key);

            if (removed == null) {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Key not found: " + key);
                return response.toString();
            }

            logger.info("DeleteForceSlotResult: admin=" + adminNickname + " deleted force result key=" + key);

            response.put("success", true);
        } catch (Exception e) {
            logger.error("DeleteForceSlotResultProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.jackpot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class DeleteSetDataResultProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    private static final String[] GAME_KEYS = {"TX", "BC", "XD", "SB", "TXmd5"};

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String actionStr = request.getParameter("action");

            if (actionStr == null || actionStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            int action;
            try {
                action = Integer.parseInt(actionStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "4002");
                return response.toString();
            }

            if (action < 0 || action > 4) {
                response.put("success", false);
                response.put("errorCode", "4003");
                return response.toString();
            }

            String gameKey = GAME_KEYS[action] + "_result";
            com.vinplay.vbee.common.cache.DistCache<String, Object> map =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheResultMiniGame", Object.class);
            map.remove(gameKey);

            logger.info("DeleteSetDataResult: removed " + gameKey);
            response.put("success", true);
        } catch (Exception e) {
            logger.error("DeleteSetDataResultProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

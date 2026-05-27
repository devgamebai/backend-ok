package com.vinplay.api.backend.processors.jackpot;

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

public class GetSetDataUserWinProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickName = request.getParameter("nick_name");
            String actionStr = request.getParameter("action");
            String winRateStr = request.getParameter("win_rate");

            com.vinplay.vbee.common.cache.DistCache<String, Object> map =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheSetUserWinRate", Object.class);

            int action = 0;
            if (actionStr != null && !actionStr.isEmpty()) {
                try {
                    action = Integer.parseInt(actionStr);
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("errorCode", "4002");
                    return response.toString();
                }
            }

            if (action == 1) {
                // Set win rate
                if (nickName == null || nickName.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    return response.toString();
                }
                if (winRateStr == null || winRateStr.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "4004");
                    return response.toString();
                }
                int winRate;
                try {
                    winRate = Integer.parseInt(winRateStr);
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("errorCode", "4005");
                    return response.toString();
                }
                map.put(nickName, winRate);
                logger.info("SetUserWinRate: set " + nickName + "=" + winRate);
                response.put("success", true);
            } else if (action == 2) {
                // Delete win rate
                if (nickName == null || nickName.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    return response.toString();
                }
                map.remove(nickName);
                logger.info("SetUserWinRate: removed " + nickName);
                response.put("success", true);
            } else {
                // List all entries (action=0 or default)
                JSONArray dataArray = new JSONArray();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    JSONObject item = new JSONObject();
                    item.put("nickName", entry.getKey());
                    item.put("winRate", entry.getValue());
                    dataArray.put(item);
                }
                response.put("success", true);
                response.put("data", dataArray);
            }
        } catch (Exception e) {
            logger.error("GetSetDataUserWinProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

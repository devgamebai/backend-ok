package com.vinplay.api.backend.processors.jackpot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class SetJackpotTaiXiuProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String result = request.getParameter("result");
            String ip = request.getParameter("ip");

            if (result == null || result.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            com.vinplay.vbee.common.cache.DistCache<String, Object> map =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheResultMiniGame", Object.class);

            String gameKey = "TXmd5_result";
            JSONObject valueObj = new JSONObject();
            valueObj.put("result", result);
            if (ip != null && !ip.isEmpty()) {
                valueObj.put("ip", ip);
            }
            map.put(gameKey, valueObj.toString());

            logger.info("SetJackpotTaiXiu: set " + gameKey + " result=" + result + " ip=" + ip);
            response.put("success", true);
        } catch (Exception e) {
            logger.error("SetJackpotTaiXiuProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

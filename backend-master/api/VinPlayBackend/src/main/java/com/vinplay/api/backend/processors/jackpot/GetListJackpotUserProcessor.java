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

public class GetListJackpotUserProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, Object> map = instance.getMap("cacheSetUserJackpot");

            JSONArray dataArray = new JSONArray();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try {
                    String key = entry.getKey();
                    // Key format: nickname_betLevel_gameName
                    String[] parts = key.split("_", 3);
                    JSONObject item = new JSONObject();
                    item.put("key", key);
                    if (parts.length >= 3) {
                        item.put("nickname", parts[0]);
                        item.put("betLevel", Integer.parseInt(parts[1]));
                        item.put("gameName", parts[2]);
                    } else {
                        item.put("nickname", key);
                        item.put("betLevel", 0);
                        item.put("gameName", "");
                    }
                    item.put("value", entry.getValue());
                    dataArray.put(item);
                } catch (Exception e) {
                    logger.warn("GetListJackpotUser: failed to parse entry key=" + entry.getKey(), e);
                }
            }

            response.put("success", true);
            response.put("data", dataArray);
        } catch (Exception e) {
            logger.error("GetListJackpotUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

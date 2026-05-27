package com.vinplay.api.backend.processors.jackpot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class SetJackpotProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickName = request.getParameter("nickName");
            String gameID = request.getParameter("gameID");
            String betValue = request.getParameter("betValue");
            String action = request.getParameter("action");

            if (nickName == null || nickName.isEmpty() || gameID == null || gameID.isEmpty()
                    || betValue == null || betValue.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            String key = nickName + "_" + betValue + "_" + gameID;
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, Object> map = instance.getMap("cacheSetUserJackpot");

            if ("delete".equalsIgnoreCase(action)) {
                map.remove(key);
                logger.info("SetJackpot: deleted key=" + key);
            } else {
                map.put(key, 1);
                logger.info("SetJackpot: set key=" + key);
            }

            response.put("success", true);
        } catch (Exception e) {
            logger.error("SetJackpotProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

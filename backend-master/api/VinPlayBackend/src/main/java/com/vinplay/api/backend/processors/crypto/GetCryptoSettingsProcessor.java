package com.vinplay.api.backend.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.crypto.TronGatewayClient;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9633 — Get USDT contract settings from the gateway (admin).
 */
public class GetCryptoSettingsProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Call gateway
            JSONObject gwResp = TronGatewayClient.getInstance().getContractSettings();

            if (gwResp.optBoolean("isSuccess", false)) {
                response.put("success", true);
                response.put("data", gwResp.opt("data"));
            } else {
                JSONObject err = gwResp.optJSONObject("error");
                String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                logger.warn("GetCryptoSettingsProcessor gateway error: " + errMsg);
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "Gateway error");
            }

        } catch (Exception e) {
            logger.error("GetCryptoSettingsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

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
 * c=9634 — Update USDT contract settings via the gateway (admin).
 */
public class UpdateCryptoSettingsProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Build settings JSON from params
            JSONObject settingsJson = new JSONObject();

            String minDeposit = request.getParameter("minDeposit");
            String minWithdraw = request.getParameter("minWithdraw");
            String maxWithdraw = request.getParameter("maxWithdraw");
            String withdrawFee = request.getParameter("withdrawFee");

            if (minDeposit != null && !minDeposit.isEmpty()) {
                settingsJson.put("minDeposit", Double.parseDouble(minDeposit));
            }
            if (minWithdraw != null && !minWithdraw.isEmpty()) {
                settingsJson.put("minWithdraw", Double.parseDouble(minWithdraw));
            }
            if (maxWithdraw != null && !maxWithdraw.isEmpty()) {
                settingsJson.put("maxWithdraw", Double.parseDouble(maxWithdraw));
            }
            if (withdrawFee != null && !withdrawFee.isEmpty()) {
                settingsJson.put("withdrawFee", Double.parseDouble(withdrawFee));
            }

            if (settingsJson.length() == 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "At least one setting parameter is required");
                return response.toString();
            }

            // Call gateway
            JSONObject gwResp = TronGatewayClient.getInstance().updateContractSettings(settingsJson);

            if (gwResp.optBoolean("isSuccess", false)) {
                response.put("success", true);
            } else {
                JSONObject err = gwResp.optJSONObject("error");
                String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                logger.warn("UpdateCryptoSettingsProcessor gateway error: " + errMsg);
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "Gateway error: " + errMsg);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid numeric setting value");
        } catch (Exception e) {
            logger.error("UpdateCryptoSettingsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

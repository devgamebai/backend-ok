package com.vinplay.api.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.crypto.TronGatewayClient;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=3020 — Get crypto deposit address for the authenticated user.
 * Calls the TRON USDT gateway to generate/retrieve a deposit address.
 */
public class GetDepositAddressProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Resolve user from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);
            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Call gateway
            JSONObject gwResp = TronGatewayClient.getInstance().getDepositAddress(userId);

            if (gwResp.optBoolean("isSuccess", false)) {
                JSONObject data = new JSONObject();
                JSONObject gwData = gwResp.optJSONObject("data");
                if (gwData != null) {
                    data.put("address", gwData.optString("address", ""));
                    data.put("network", "TRC20");
                    data.put("symbol", "USDT");
                } else {
                    data.put("address", "");
                    data.put("network", "TRC20");
                    data.put("symbol", "USDT");
                }
                response.put("success", true);
                response.put("data", data);
            } else {
                JSONObject err = gwResp.optJSONObject("error");
                String errCode = err != null ? err.optString("code", "UNKNOWN") : "UNKNOWN";
                String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                logger.warn("GetDepositAddressProcessor gateway error: " + errCode + " — " + errMsg);
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "Gateway error");
            }

        } catch (Exception e) {
            logger.error("GetDepositAddressProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

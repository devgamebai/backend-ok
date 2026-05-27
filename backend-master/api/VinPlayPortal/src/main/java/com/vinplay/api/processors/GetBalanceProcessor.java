package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=3098 — Lightweight balance check (Hazelcast only, no DB).
 *
 * SUN-962: FE calls this when returning from a provider game webview
 * to refresh the displayed balance. Returns current vin + xu from cache.
 */
public class GetBalanceProcessor implements BaseProcessor<HttpServletRequest, String> {

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String token = request.getParameter("at");
            if (token == null || token.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String nickname = tokenMap.get(token);
            if (nickname == null || nickname.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            IMap<String, UserCacheModel> userMap = HazelcastClientFactory.getInstance().getMap("users");
            UserCacheModel user = userMap.get(nickname);
            if (user == null) {
                response.put("success", false);
                response.put("errorCode", "1002");
                return response.toString();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("vin", user.getVin());
            response.put("xu", user.getXu());
            response.put("nickname", nickname);
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}

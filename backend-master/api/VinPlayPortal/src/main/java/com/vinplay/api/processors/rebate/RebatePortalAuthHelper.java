package com.vinplay.api.processors.rebate;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Auth helper for rebate portal APIs.
 * Enforces:
 * 1) valid access token session
 * 2) session user must be F0 agent
 * 3) cannot access other agent's rebate data
 */
final class RebatePortalAuthHelper {

    private RebatePortalAuthHelper() {}

    static Integer resolveAuthorizedAgentId(HttpServletRequest request, JSONObject response) {
        String accessToken = request.getParameter("at");
        if (accessToken == null || accessToken.isEmpty()) {
            fail(response, "1001", "Access token is required");
            return null;
        }

        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                fail(response, "1001", "Invalid access token");
                return null;
            }

            String sessionNickname = tokenMap.get(accessToken);
            if (sessionNickname == null || sessionNickname.isEmpty()) {
                fail(response, "1001", "Invalid session");
                return null;
            }

            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(sessionNickname);
            if (userCache == null) {
                fail(response, "1001", "Session expired");
                return null;
            }

            int sessionAgentId = RebateService.getAgentIdByNickname(sessionNickname);
            if (sessionAgentId <= 0) {
                fail(response, "1001", "Only F0 agent can access rebate API");
                return null;
            }

            Integer requestedAgentId = parseRequestedAgentId(request);
            if (requestedAgentId == null) {
                fail(response, "1001", "Invalid agent_user_id");
                return null;
            }
            if (requestedAgentId > 0 && requestedAgentId != sessionAgentId) {
                fail(response, "1001", "Forbidden: cannot access other agent data");
                return null;
            }
            return sessionAgentId;
        } catch (Exception e) {
            fail(response, "1001", "Authorization failed");
            return null;
        }
    }

    private static Integer parseRequestedAgentId(HttpServletRequest request) {
        String agentIdStr = request.getParameter("agent_user_id");
        if (agentIdStr != null && !agentIdStr.isEmpty()) {
            try {
                return Integer.parseInt(agentIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String nickname = request.getParameter("nn");
        if (nickname != null && !nickname.trim().isEmpty()) {
            return RebateService.getAgentIdByNickname(nickname.trim());
        }
        return 0;
    }

    private static void fail(JSONObject response, String errorCode, String message) {
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", message);
    }
}

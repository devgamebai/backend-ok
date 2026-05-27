package com.vinplay.api.processors.creditwallet;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3055: Get parent agent info for the authenticated user.
 *
 * FE calls this when the user opens the "Chuyển Tiền" tab to render
 * the agent name and code as fixed (read-only) display fields.
 *
 * Params:
 *   at — User Access Token (required)
 *
 * Success response:
 * {
 *   "success": true,
 *   "errorCode": "0",
 *   "data": {
 *     "agent_code": "VIP888",
 *     "agent_name": "DaiLySo1SunWin"
 *   }
 * }
 *
 * Error codes:
 *   1001 — Unauthorized / token invalid
 *   1003 — User has no parent agent (not referred by any agent)
 */
public class GetAgentInfoForUserProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
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

            // Resolve nickname from token
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);

            // Fetch user's referral_code from DB
            String referralCode = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT referral_code FROM users WHERE nick_name = ?")) {
                ps.setString(1, nickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        referralCode = rs.getString("referral_code");
                    }
                }
            }

            if (referralCode == null || referralCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1003");
                response.put("message", "Tài khoản không thuộc đại lý nào");
                return response.toString();
            }

            // Lookup agent info by referral code
            String agentName = null;
            String agentCode = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nickname, code FROM vinplay_admin.useragent WHERE code = ? AND active = 1 LIMIT 1")) {
                ps.setString(1, referralCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentName = rs.getString("nickname");
                        agentCode = rs.getString("code");
                    }
                }
            }

            if (agentName == null) {
                response.put("success", false);
                response.put("errorCode", "1003");
                response.put("message", "Đại lý tuyến trên không tồn tại hoặc đã bị khóa");
                return response.toString();
            }

            JSONObject data = new JSONObject();
            data.put("agent_code", agentCode);
            data.put("agent_name", agentName);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

        } catch (Exception e) {
            logger.error("GetAgentInfoForUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

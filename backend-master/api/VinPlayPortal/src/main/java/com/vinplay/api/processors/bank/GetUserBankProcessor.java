package com.vinplay.api.processors.bank;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.dal.dao.impl.UserBankDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class GetUserBankProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            // Resolve nickname from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = (String) tokenMap.get(accessToken);
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
            UserBankDao dao = new UserBankDaoImpl();
            Map<String, Object> bankInfo = dao.getUserBank(userId);

            if (bankInfo == null) {
                response.put("success", true);
                response.put("data", JSONObject.NULL);
            } else {
                JSONObject data = new JSONObject();
                data.put("id", bankInfo.get("id"));
                data.put("user_id", bankInfo.get("user_id"));
                data.put("nick_name", bankInfo.get("nick_name"));
                data.put("bank_name", bankInfo.get("bank_name"));
                data.put("customer_name", bankInfo.get("customer_name"));
                data.put("bank_number", bankInfo.get("bank_number"));
                data.put("status", bankInfo.get("status"));
                data.put("is_locked", bankInfo.get("is_locked"));
                data.put("create_date", bankInfo.get("create_date") != null ? bankInfo.get("create_date") : JSONObject.NULL);
                data.put("branch", bankInfo.get("branch") != null ? bankInfo.get("branch") : JSONObject.NULL);
                data.put("update_date", bankInfo.get("update_date") != null ? bankInfo.get("update_date") : JSONObject.NULL);
                data.put("last_editor", bankInfo.get("last_editor") != null ? bankInfo.get("last_editor") : JSONObject.NULL);
                data.put("code", bankInfo.get("code") != null ? bankInfo.get("code") : JSONObject.NULL);
                data.put("logo", bankInfo.get("logo") != null ? bankInfo.get("logo") : JSONObject.NULL);
                response.put("success", true);
                response.put("data", data);
            }
        } catch (Exception e) {
            logger.error("GetUserBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

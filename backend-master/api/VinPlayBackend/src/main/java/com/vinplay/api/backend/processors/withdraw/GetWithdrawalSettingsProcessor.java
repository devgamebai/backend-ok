package com.vinplay.api.backend.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9640 — Get all withdrawal settings (admin).
 * Returns key-value pairs from the withdrawal_settings table.
 */
public class GetWithdrawalSettingsProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Fetch all settings
            JSONObject data = new JSONObject();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                ps = conn.prepareStatement("SELECT setting_key, setting_value FROM withdrawal_settings");
                rs = ps.executeQuery();
                while (rs.next()) {
                    data.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            logger.error("GetWithdrawalSettingsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

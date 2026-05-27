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

/**
 * c=9646 — Update game volume config (admin).
 * Params: game_id (int), volume_percentage (0-100).
 */
public class UpdateGameVolumeConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Parse params
            String gameIdStr = request.getParameter("game_id");
            String volumePctStr = request.getParameter("volume_percentage");

            if (gameIdStr == null || gameIdStr.isEmpty() || volumePctStr == null || volumePctStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "game_id and volume_percentage are required");
                return response.toString();
            }

            int gameId;
            int volumePercentage;
            try {
                gameId = Integer.parseInt(gameIdStr);
                volumePercentage = Integer.parseInt(volumePctStr);
            } catch (NumberFormatException nfe) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "game_id and volume_percentage must be integers");
                return response.toString();
            }

            if (volumePercentage < 0 || volumePercentage > 100) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "volume_percentage must be between 0 and 100");
                return response.toString();
            }

            // Update the row
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                ps = conn.prepareStatement("UPDATE game_volume_config SET volume_percentage = ? WHERE game_id = ?");
                ps.setInt(1, volumePercentage);
                ps.setInt(2, gameId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    response.put("success", false);
                    response.put("errorCode", "4004");
                    response.put("message", "game_id not found");
                    return response.toString();
                }
            } finally {
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);

        } catch (Exception e) {
            logger.error("UpdateGameVolumeConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

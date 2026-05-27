package com.vinplay.api.backend.processors.rtp;

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
 * c=9774 — Delete a per-user RTP override.
 * Params: aat, user_id, game_code.
 */
public class DeleteUserRtpOverrideProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String userIdStr = request.getParameter("user_id");
            String gameCode = request.getParameter("game_code");
            String actor = UpdateGameRtpConfigProcessor.resolveActor(request);

            if (userIdStr == null || userIdStr.isEmpty()) return err(response, "4001", "user_id required");
            if (gameCode == null || gameCode.isEmpty()) return err(response, "4002", "game_code required");

            long userId;
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { return err(response, "4003", "user_id not numeric"); }

            int rows;
            Double oldPct = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT win_rate_pct FROM user_rtp_override WHERE user_id=? AND game_code=?")) {
                    ps.setLong(1, userId);
                    ps.setString(2, gameCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) oldPct = rs.getDouble(1);
                    }
                }
                if (oldPct == null) return err(response, "1002", "Override not found");

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM user_rtp_override WHERE user_id=? AND game_code=?")) {
                    ps.setLong(1, userId);
                    ps.setString(2, gameCode);
                    rows = ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rtp_config_audit (scope, game_code, user_id, action, old_value, new_value, actor, reason) " +
                        "VALUES ('USER', ?, ?, 'DELETE', ?, NULL, ?, ?)")) {
                    ps.setString(1, gameCode);
                    ps.setLong(2, userId);
                    ps.setDouble(3, oldPct);
                    ps.setString(4, actor);
                    ps.setString(5, "delete override");
                    ps.executeUpdate();
                }
            }

            // Remove from Hazelcast
            try {
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, Object> cache = hz.getMap("cacheUserRtpOverride");
                cache.remove(userId + ":" + gameCode);
            } catch (Exception e) {
                logger.warn("DeleteUserRtpOverrideProcessor: cache remove failed user=" + userId, e);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("deleted", rows);
            response.put("previousPct", oldPct);
        } catch (Exception e) {
            logger.error("DeleteUserRtpOverrideProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}

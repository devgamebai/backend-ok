package com.vinplay.api.backend.processors.rtp;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rtp.RtpResolver;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9773 — Set (upsert) a per-user RTP override.
 * Params: aat, user_id, game_code (or 'ALL'), win_rate_pct, reason, expires_at (optional, 'yyyy-MM-dd HH:mm:ss').
 */
public class SetUserRtpOverrideProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String userIdStr = request.getParameter("user_id");
            String gameCode = request.getParameter("game_code");
            String pctStr = request.getParameter("win_rate_pct");
            String reason = request.getParameter("reason");
            String expiresAt = request.getParameter("expires_at");
            String actor = UpdateGameRtpConfigProcessor.resolveActor(request);

            if (userIdStr == null || userIdStr.isEmpty()) return err(response, "4001", "user_id required");
            if (gameCode == null || gameCode.isEmpty()) return err(response, "4002", "game_code required");
            if (pctStr == null || pctStr.isEmpty()) return err(response, "4003", "win_rate_pct required");
            if (reason == null || reason.isEmpty()) reason = "manual";

            long userId;
            double pct;
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { return err(response, "4004", "user_id not numeric"); }
            try { pct = Double.parseDouble(pctStr); }
            catch (NumberFormatException e) { return err(response, "4005", "win_rate_pct not numeric"); }

            if (pct < RtpResolver.HARD_FLOOR_PCT || pct > RtpResolver.HARD_CEIL_PCT) {
                return err(response, "4006",
                        "win_rate_pct out of range [" + RtpResolver.HARD_FLOOR_PCT
                        + "," + RtpResolver.HARD_CEIL_PCT + "]");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                Double oldPct = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT win_rate_pct FROM user_rtp_override WHERE user_id=? AND game_code=?")) {
                    ps.setLong(1, userId);
                    ps.setString(2, gameCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) oldPct = rs.getDouble(1);
                    }
                }
                String action = (oldPct == null) ? "CREATE" : "UPDATE";

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO user_rtp_override (user_id, game_code, win_rate_pct, reason, expires_at, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE win_rate_pct=VALUES(win_rate_pct), reason=VALUES(reason), " +
                        "  expires_at=VALUES(expires_at), created_by=VALUES(created_by)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, gameCode);
                    ps.setDouble(3, pct);
                    ps.setString(4, reason);
                    if (expiresAt != null && !expiresAt.isEmpty()) ps.setString(5, expiresAt);
                    else ps.setNull(5, java.sql.Types.TIMESTAMP);
                    ps.setString(6, actor);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rtp_config_audit (scope, game_code, user_id, action, old_value, new_value, actor, reason) " +
                        "VALUES ('USER', ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, gameCode);
                    ps.setLong(2, userId);
                    ps.setString(3, action);
                    if (oldPct != null) ps.setDouble(4, oldPct); else ps.setNull(4, java.sql.Types.DECIMAL);
                    ps.setDouble(5, pct);
                    ps.setString(6, actor);
                    ps.setString(7, reason);
                    ps.executeUpdate();
                }
            }

            // Update Hazelcast
            try {
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, Object> cache = hz.getMap("cacheUserRtpOverride");
                cache.put(userId + ":" + gameCode, pct);
            } catch (Exception e) {
                logger.warn("SetUserRtpOverrideProcessor: cache update failed user=" + userId + " game=" + gameCode, e);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            data.put("game_code", gameCode);
            data.put("win_rate_pct", pct);
            data.put("reason", reason);
            data.put("created_by", actor);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("SetUserRtpOverrideProcessor error", e);
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

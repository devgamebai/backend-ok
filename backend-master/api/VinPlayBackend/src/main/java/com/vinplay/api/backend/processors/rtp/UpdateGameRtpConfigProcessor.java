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
 * c=9771 — Create/update a game RTP config row.
 * Params: aat, game_code, win_rate_pct, note (optional).
 * On success: writes MySQL row, inserts audit, invalidates Hazelcast cacheGameRtp.
 */
public class UpdateGameRtpConfigProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String gameCode = request.getParameter("game_code");
            String pctStr = request.getParameter("win_rate_pct");
            String note = request.getParameter("note");
            String actor = resolveActor(request);

            if (gameCode == null || gameCode.isEmpty()) {
                return err(response, "4001", "game_code required");
            }
            if (pctStr == null || pctStr.isEmpty()) {
                return err(response, "4002", "win_rate_pct required");
            }
            double pct;
            try { pct = Double.parseDouble(pctStr); }
            catch (NumberFormatException e) { return err(response, "4003", "win_rate_pct not numeric"); }

            if (pct < RtpResolver.HARD_FLOOR_PCT || pct > RtpResolver.HARD_CEIL_PCT) {
                return err(response, "4004",
                        "win_rate_pct out of range [" + RtpResolver.HARD_FLOOR_PCT
                        + "," + RtpResolver.HARD_CEIL_PCT + "]");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Fetch old value for audit
                Double oldPct = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT win_rate_pct FROM game_rtp_config WHERE game_code = ?")) {
                    ps.setString(1, gameCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) oldPct = rs.getDouble(1);
                    }
                }

                String action = (oldPct == null) ? "CREATE" : "UPDATE";

                // Upsert
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO game_rtp_config (game_code, win_rate_pct, status, updated_by, note) " +
                        "VALUES (?, ?, 1, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE win_rate_pct = VALUES(win_rate_pct), " +
                        "  updated_by = VALUES(updated_by), note = VALUES(note)")) {
                    ps.setString(1, gameCode);
                    ps.setDouble(2, pct);
                    ps.setString(3, actor);
                    ps.setString(4, note != null ? note : "");
                    ps.executeUpdate();
                }

                // Audit
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rtp_config_audit (scope, game_code, action, old_value, new_value, actor, reason) " +
                        "VALUES ('GAME', ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, gameCode);
                    ps.setString(2, action);
                    if (oldPct != null) ps.setDouble(3, oldPct); else ps.setNull(3, java.sql.Types.DECIMAL);
                    ps.setDouble(4, pct);
                    ps.setString(5, actor);
                    ps.setString(6, note != null ? note : "");
                    ps.executeUpdate();
                }
            }

            // Invalidate/update the game-RTP cache via routing-flagged backend.
            try {
                com.vinplay.vbee.common.cache.DistCache<String, Object> cache =
                        com.vinplay.vbee.common.cache.CacheFactory.get("cacheGameRtp", Object.class);
                cache.put(gameCode, pct);
            } catch (Exception e) {
                logger.warn("UpdateGameRtpConfigProcessor: cache update failed game=" + gameCode, e);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            JSONObject data = new JSONObject();
            data.put("game_code", gameCode);
            data.put("win_rate_pct", pct);
            data.put("updated_by", actor);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("UpdateGameRtpConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    static String resolveActor(HttpServletRequest request) {
        try {
            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return "system";
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String nick = tokenMap.get(aat);
            return (nick != null) ? nick : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}

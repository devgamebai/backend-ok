package com.vinplay.dal.rtp;

import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Service quản lý toàn bộ RTP config theo spec game_win_rate_implementation_plan_vi.md:
 *
 *  - game_rtp_config       → default % thắng theo từng game
 *  - user_rtp_override     → override riêng cho từng user × game
 *  - rtp_config_audit      → audit trail mọi thay đổi
 *
 * Hazelcast maps:
 *  - "cacheGameRtp"          : game_code → win_rate_pct (Double)
 *  - "cacheUserRtpOverride"  : "userId:game_code" → win_rate_pct (Double)
 */
public class RtpConfigService {

    private static final Logger logger = Logger.getLogger("api");

    public static final String MAP_GAME_RTP          = "cacheGameRtp";
    public static final String MAP_USER_RTP_OVERRIDE = "cacheUserRtpOverride";

    public static final String KEY_EMERGENCY_RTP     = "EMERGENCY_GLOBAL_RTP";

    // CacheFactory does its own per-name memoization, so we don't need the
    // local double-checked-locking IMap cache anymore. Routing flag in
    // cache.routing.properties decides Hazelcast vs Redis backend.
    private static DistCache<String, Double> getGameRtpMap() {
        return CacheFactory.get(MAP_GAME_RTP, Double.class);
    }

    private static DistCache<String, Double> getUserRtpMap() {
        return CacheFactory.get(MAP_USER_RTP_OVERRIDE, Double.class);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GAME RTP CONFIG (default per-game)
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> listGameConfigs() throws SQLException {
        String sql = "SELECT * FROM game_rtp_config ORDER BY game_code ASC";
        return queryList(sql);
    }

    /**
     * Cập nhật % thắng mặc định cho một game.
     * Ghi audit + đẩy Hazelcast ngay lập tức.
     */
    public boolean updateGameConfig(String gameCode, double winRatePct, String note, String actor) throws SQLException {
        validatePct(winRatePct);
        Double oldValue = getGameRtpFromCache(gameCode);

        String sql = "INSERT INTO game_rtp_config (game_code, win_rate_pct, updated_by, note) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE win_rate_pct=VALUES(win_rate_pct), " +
                "updated_by=VALUES(updated_by), note=VALUES(note)";

        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, gameCode.trim().toLowerCase());
            s.setDouble(2, winRatePct);
            s.setString(3, actor);
            s.setString(4, note);
            boolean ok = s.executeUpdate() > 0;
            if (ok) {
                pushGameRtpToCache(gameCode, winRatePct);
                writeAudit(conn, "GAME", gameCode, null, oldValue, winRatePct, actor, note);
            }
            return ok;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // USER RTP OVERRIDE (per-user × per-game)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * List override, filter theo game_code hoặc user_id (null = mọi).
     */
    public List<Map<String, Object>> listUserOverrides(String gameCode, Long userId, int page, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT o.*, DATE_FORMAT(o.expires_at,'%Y-%m-%d %H:%i:%s') AS expires_str " +
                "FROM user_rtp_override o WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (gameCode != null && !gameCode.isEmpty()) {
            sql.append(" AND o.game_code=?"); params.add(gameCode);
        }
        if (userId != null) {
            sql.append(" AND o.user_id=?"); params.add(userId);
        }
        // chỉ lấy active (chưa hết hạn hoặc không hết hạn)
        sql.append(" AND (o.expires_at IS NULL OR o.expires_at > NOW())");
        sql.append(" ORDER BY o.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);
        return queryList(sql.toString(), params.toArray());
    }

    /**
     * Đặt / cập nhật override % thắng cho user.
     * Nếu win_rate_pct = -1 → xoá override (tương đương gọi delete).
     */
    public boolean setUserOverride(long userId, String gameCode, double winRatePct,
                                   String reason, String expiresAt, String actor) throws SQLException {
        validatePct(winRatePct);
        String cacheKey = cacheKey(userId, gameCode);
        Double oldValue = getUserRtpFromCache(cacheKey);

        String sql = "INSERT INTO user_rtp_override (user_id, game_code, win_rate_pct, reason, expires_at, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE win_rate_pct=VALUES(win_rate_pct), reason=VALUES(reason), " +
                "expires_at=VALUES(expires_at), created_by=VALUES(created_by)";

        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            s.setLong(1, userId);
            s.setString(2, gameCode.trim().toLowerCase());
            s.setDouble(3, winRatePct);
            s.setString(4, reason);
            if (expiresAt != null && !expiresAt.isEmpty()) {
                s.setString(5, expiresAt);
            } else {
                s.setNull(5, Types.TIMESTAMP);
            }
            s.setString(6, actor);
            boolean ok = s.executeUpdate() > 0;
            if (ok) {
                pushUserRtpToCache(cacheKey, winRatePct);
                writeAudit(conn, "USER", gameCode, userId, oldValue, winRatePct, actor, reason);
                logger.info("RtpConfigService.setUserOverride: userId=" + userId +
                        " game=" + gameCode + " pct=" + winRatePct + " by=" + actor);
            }
            return ok;
        }
    }

    /**
     * Xoá override → user về default game.
     */
    public boolean deleteUserOverride(long userId, String gameCode, String actor) throws SQLException {
        String cacheKey = cacheKey(userId, gameCode);
        Double oldValue = getUserRtpFromCache(cacheKey);

        String sql = "DELETE FROM user_rtp_override WHERE user_id=? AND game_code=?";
        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            s.setLong(1, userId);
            s.setString(2, gameCode.trim().toLowerCase());
            boolean ok = s.executeUpdate() > 0;
            if (ok) {
                try { getUserRtpMap().remove(cacheKey); } catch (Exception e) { /* no-op: CacheFactory memoizes */; }
                writeAudit(conn, "USER", gameCode, userId, oldValue, null, actor, "override_deleted");
                logger.info("RtpConfigService.deleteUserOverride: userId=" + userId + " game=" + gameCode);
            }
            return ok;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // AUDIT
    // ════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getAuditLogs(String scope, String gameCode, Long userId,
                                                   String from, String to, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM rtp_config_audit WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (scope != null && !scope.isEmpty()) {
            sql.append(" AND scope=?"); params.add(scope.toUpperCase());
        }
        if (gameCode != null && !gameCode.isEmpty()) {
            sql.append(" AND game_code=?"); params.add(gameCode);
        }
        if (userId != null) {
            sql.append(" AND user_id=?"); params.add(userId);
        }
        if (from != null && !from.isEmpty()) {
            sql.append(" AND at >=?"); params.add(from);
        }
        if (to != null && !to.isEmpty()) {
            sql.append(" AND at <=?"); params.add(to);
        }
        sql.append(" ORDER BY at DESC LIMIT ?");
        params.add(Math.min(limit, 500));
        return queryList(sql.toString(), params.toArray());
    }

    // ════════════════════════════════════════════════════════════════════════
    // CACHE READ (static — dùng trong RtpResolver)
    // ════════════════════════════════════════════════════════════════════════

    /** Lấy default % game từ cache. Null = không có config. */
    public static Double getGameRtpFromCache(String gameCode) {
        try {
            return getGameRtpMap().get(gameCode);
        } catch (Exception e) {
            /* no-op: CacheFactory memoizes */;
            return null;
        }
    }

    /** Lấy override user+game từ cache. Null = không có override. */
    public static Double getUserRtpFromCache(String cacheKey) {
        try {
            return getUserRtpMap().get(cacheKey);
        } catch (Exception e) {
            /* no-op: CacheFactory memoizes */;
            return null;
        }
    }

    public static String cacheKey(long userId, String gameCode) {
        return userId + ":" + gameCode.toLowerCase();
    }

    /**
     * Reload toàn bộ config từ DB vào Hazelcast — gọi khi backend/game server khởi động.
     */
    public void reloadAllToCache() {
        try {
            // 1. Game defaults
            List<Map<String, Object>> games = listGameConfigs();
            DistCache<String, Double> gameMap = getGameRtpMap();
            for (Map<String, Object> row : games) {
                String code = (String) row.get("game_code");
                Object pctObj = row.get("win_rate_pct");
                Object statusObj = row.get("status");
                if (code != null && pctObj != null) {
                    boolean active = statusObj == null || ((Number) statusObj).intValue() == 1;
                    if (active) {
                        gameMap.put(code, ((Number) pctObj).doubleValue());
                    } else {
                        gameMap.remove(code);
                    }
                }
            }

            // 2. User overrides (chỉ load những cái còn hạn)
            String sql = "SELECT user_id, game_code, win_rate_pct FROM user_rtp_override " +
                    "WHERE expires_at IS NULL OR expires_at > NOW()";
            List<Map<String, Object>> overrides = queryList(sql);
            DistCache<String, Double> userMap = getUserRtpMap();
            for (Map<String, Object> row : overrides) {
                long uid = ((Number) row.get("user_id")).longValue();
                String code = (String) row.get("game_code");
                double pct = ((Number) row.get("win_rate_pct")).doubleValue();
                userMap.put(cacheKey(uid, code), pct);
            }

            logger.info("RtpConfigService.reloadAllToCache: " + games.size() +
                    " game defaults, " + overrides.size() + " user overrides");
        } catch (Exception e) {
            logger.error("RtpConfigService.reloadAllToCache failed", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SESSION LEVEL OVERRIDE & FREEZE GAME (Appendix B2 & B7)
    // ════════════════════════════════════════════════════════════════════════
    public boolean setSessionOverride(long userId, String gameCode, double pct, String actor) throws SQLException {
        validatePct(pct); // reuse existing validation (min 40%, max 100%)
        getUserRtpMap().put(cacheKey(userId, gameCode), pct);
        try (Connection conn = getConn()) {
            writeAudit(conn, "USER", gameCode, userId, null, pct, actor, "Session Volatile Override");
        }
        return true;
    }

    public boolean setGameDisabled(String gameCode, boolean disabled, String actor) throws SQLException {
        String key = "DISABLED_" + gameCode.toLowerCase();
        if (disabled) {
            getGameRtpMap().put(key, -1.0);
        } else {
            getGameRtpMap().remove(key); // Bug fix: cannot put null into IMap
        }
        try (Connection conn = getConn()) {
            writeAudit(conn, "GAME", gameCode, null, null, disabled ? -1.0 : null, actor, "Game Disabled Status Changed");
        }
        return true;
    }

    public boolean isGameDisabled(String gameCode) {
        Double disableFlag = getGameRtpMap().get("DISABLED_" + gameCode.toLowerCase());
        return disableFlag != null && disableFlag.doubleValue() == -1.0; // Bug fix: use .doubleValue() not ==
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHASE 8: EMERGENCY CONTROLS
    // ════════════════════════════════════════════════════════════════════════
    public Double getEmergencyRtp() {
        return getGameRtpMap().get(KEY_EMERGENCY_RTP);
    }

    public boolean setEmergencyRtp(double winRatePct, String actor) throws SQLException {
        getGameRtpMap().put(KEY_EMERGENCY_RTP, winRatePct);
        try (Connection conn = getConn()) {
            writeAudit(conn, "GAME", "ALL_EMERGENCY", null, null, winRatePct, actor, "Emergency RTP Activated");
        }
        return true;
    }

    public boolean resetEmergency(String actor) throws SQLException {
        getGameRtpMap().remove(KEY_EMERGENCY_RTP);
        try (Connection conn = getConn()) {
            writeAudit(conn, "GAME", "ALL_EMERGENCY", null, null, null, actor, "Emergency RTP Deactivated");
        }
        return true;
    }
    
    public boolean resetAllUserOverrides(String actor) throws SQLException {
        String sql = "UPDATE user_rtp_override SET expires_at = NOW() WHERE expires_at IS NULL OR expires_at > NOW()";
        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            boolean ok = s.executeUpdate() > 0;
            if (ok) {
                // Clear entire user map in hazelcast
                getUserRtpMap().clear();
                writeAudit(conn, "USER", "ALL", null, null, null, actor, "Nuclear Reset All Overrides");
            }
            return ok;
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void pushGameRtpToCache(String gameCode, double pct) {
        try { getGameRtpMap().put(gameCode, pct); }
        catch (Exception e) { /* no-op: CacheFactory memoizes */; }
    }

    private void pushUserRtpToCache(String cacheKey, double pct) {
        try { getUserRtpMap().put(cacheKey, pct); }
        catch (Exception e) { /* no-op: CacheFactory memoizes */; }
    }

    private void writeAudit(Connection conn, String scope, String gameCode, Long userId,
                            Double oldVal, Double newVal, String actor, String reason)
            throws SQLException {
        String sql = "INSERT INTO rtp_config_audit (scope, game_code, user_id, old_value, new_value, actor, reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, scope);
            s.setString(2, gameCode);
            if (userId != null) s.setLong(3, userId); else s.setNull(3, Types.BIGINT);
            if (oldVal != null) s.setDouble(4, oldVal); else s.setNull(4, Types.DECIMAL);
            if (newVal != null) s.setDouble(5, newVal); else s.setNull(5, Types.DECIMAL);
            s.setString(6, actor);
            s.setString(7, reason);
            s.executeUpdate();
        }
    }

    private void validatePct(double pct) {
        if (pct < 0 || pct > 100) throw new IllegalArgumentException("win_rate_pct must be 0–100, got: " + pct);
        if (pct < 40) throw new IllegalArgumentException("win_rate_pct floor is 40% (safeguard)");
    }

    private Connection getConn() throws SQLException {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    private List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConn(); PreparedStatement s = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                s.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = s.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++)
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    results.add(row);
                }
            }
        }
        return results;
    }
}

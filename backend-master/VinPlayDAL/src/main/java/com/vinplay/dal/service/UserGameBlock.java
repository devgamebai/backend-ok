package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user block list — refuses bets for a player on a specific
 * provider / vendor_platform / game_code / table_tag / category. The
 * row's non-NULL predicates must ALL match the incoming bet; NULL
 * means "any". Mirrors {@link GscBetWhitelist}'s posture: in-process
 * cache with a short TTL, fail-open on DB errors.
 *
 * <p>Cache strategy: a per-user list of active blocks is loaded on
 * first hit and cached for {@link #CACHE_TTL_MS}. The matcher walks
 * the list locally — small N (a few rows per user at most), no SQL
 * per bet.
 */
public final class UserGameBlock {

    private static final Logger logger = Logger.getLogger("backend");
    private static final long CACHE_TTL_MS = 60_000L;

    /** Single block row. */
    public static final class BlockRow {
        public final long id;
        public final Long userId;
        public final String nickName;
        public final String provider;        // null = any
        public final String vendorPlatform;  // null = any
        public final String gameCode;        // null = any
        public final String tableTag;        // null = any
        public final Integer categoryId;     // null = any
        public final String reason;
        public final String blockedBy;
        public final java.sql.Timestamp blockedAt;
        public final java.sql.Timestamp expiresAt;
        public final boolean active;

        BlockRow(long id, Long userId, String nickName, String provider, String vendorPlatform,
                 String gameCode, String tableTag, Integer categoryId, String reason,
                 String blockedBy, java.sql.Timestamp blockedAt, java.sql.Timestamp expiresAt,
                 boolean active) {
            this.id = id; this.userId = userId; this.nickName = nickName;
            this.provider = provider; this.vendorPlatform = vendorPlatform;
            this.gameCode = gameCode; this.tableTag = tableTag;
            this.categoryId = categoryId; this.reason = reason;
            this.blockedBy = blockedBy; this.blockedAt = blockedAt;
            this.expiresAt = expiresAt; this.active = active;
        }

        boolean matchesGame(String prov, String vp, String gc, String tt, Integer cat) {
            return predicateOK(provider, prov)
                && predicateOK(vendorPlatform, vp)
                && predicateOK(gameCode, gc)
                && predicateOK(tableTag, tt)
                && predicateOKInt(categoryId, cat);
        }
        private static boolean predicateOK(String row, String bet) {
            if (row == null || row.isEmpty()) return true;
            if (bet == null) return false;
            return row.equalsIgnoreCase(bet);
        }
        private static boolean predicateOKInt(Integer row, Integer bet) {
            if (row == null) return true;
            if (bet == null) return false;
            return row.equals(bet);
        }
    }

    private static final class CacheEntry {
        final long expiresAt;
        final List<BlockRow> rows;
        CacheEntry(long expiresAt, List<BlockRow> rows) { this.expiresAt = expiresAt; this.rows = rows; }
    }
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private UserGameBlock() {}

    /**
     * @return {@code true} when the user is blocked for the given bet
     *         dimensions. {@code false} when free to bet (including DB
     *         errors — fail-open like {@link GscBetWhitelist}).
     */
    public static boolean isBlocked(long userId, String nickName,
                                    String provider, String vendorPlatform,
                                    String gameCode, String tableTag, Integer categoryId) {
        if (userId <= 0 && (nickName == null || nickName.isEmpty())) return false;
        long now = System.currentTimeMillis();
        String key = userId > 0 ? ("u:" + userId) : ("n:" + nickName);
        CacheEntry c = CACHE.get(key);
        List<BlockRow> rows;
        if (c != null && c.expiresAt > now) {
            rows = c.rows;
        } else {
            rows = loadActiveBlocks(userId, nickName);
            CACHE.put(key, new CacheEntry(now + CACHE_TTL_MS, rows));
        }
        if (rows.isEmpty()) return false;
        long nowMs = System.currentTimeMillis();
        for (BlockRow r : rows) {
            if (!r.active) continue;
            if (r.expiresAt != null && r.expiresAt.getTime() <= nowMs) continue;
            if (r.matchesGame(provider, vendorPlatform, gameCode, tableTag, categoryId)) {
                return true;
            }
        }
        return false;
    }

    /** List active blocks for a user (DB scan, no cache — for admin views). */
    public static List<BlockRow> listForUser(long userId, String nickName) {
        return loadActiveBlocks(userId, nickName);
    }

    /** Insert a new block row. Returns the generated id (-1 on error). */
    public static long add(Long userId, String nickName, String provider, String vendorPlatform,
                            String gameCode, String tableTag, Integer categoryId, String reason,
                            String blockedBy, java.sql.Timestamp expiresAt) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO user_game_block (user_id, nick_name, provider, vendor_platform, "
                             + "game_code, table_tag, category_id, reason, blocked_by, expires_at, is_active) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (userId == null) ps.setNull(1, java.sql.Types.BIGINT); else ps.setLong(1, userId);
            ps.setString(2, nickName);
            ps.setString(3, provider);
            ps.setString(4, vendorPlatform);
            ps.setString(5, gameCode);
            ps.setString(6, tableTag);
            if (categoryId == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, categoryId);
            ps.setString(8, reason);
            ps.setString(9, blockedBy);
            if (expiresAt == null) ps.setNull(10, java.sql.Types.TIMESTAMP); else ps.setTimestamp(10, expiresAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    invalidateCache(userId, nickName);
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            logger.warn("UserGameBlock.add failed: " + e.getMessage());
        }
        return -1L;
    }

    /** Soft-deactivate a block by id. Returns true if a row was modified. */
    public static boolean deactivate(long id, String adminNick) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement select = conn.prepareStatement(
                     "SELECT user_id, nick_name FROM user_game_block WHERE id = ?")) {
            select.setLong(1, id);
            Long uid = null; String nick = null;
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) { uid = (Long) rs.getObject("user_id"); nick = rs.getString("nick_name"); }
            }
            if (uid == null && nick == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(
                     "UPDATE user_game_block SET is_active = 0 WHERE id = ?")) {
                ps.setLong(1, id);
                int n = ps.executeUpdate();
                if (n > 0) { invalidateCache(uid, nick); return true; }
            }
        } catch (Exception e) {
            logger.warn("UserGameBlock.deactivate failed for id=" + id + ": " + e.getMessage());
        }
        return false;
    }

    private static void invalidateCache(Long userId, String nickName) {
        if (userId != null && userId > 0) CACHE.remove("u:" + userId);
        if (nickName != null && !nickName.isEmpty()) CACHE.remove("n:" + nickName);
    }

    private static List<BlockRow> loadActiveBlocks(long userId, String nickName) {
        List<BlockRow> out = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE is_active = 1 AND (expires_at IS NULL OR expires_at > NOW())");
        boolean hasUser = userId > 0;
        boolean hasNick = nickName != null && !nickName.isEmpty();
        if (hasUser && hasNick)       where.append(" AND (user_id = ? OR nick_name = ?)");
        else if (hasUser)             where.append(" AND user_id = ?");
        else if (hasNick)             where.append(" AND nick_name = ?");
        else                          return out;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, nick_name, provider, vendor_platform, game_code, "
                             + "table_tag, category_id, reason, blocked_by, blocked_at, expires_at, is_active "
                             + "FROM user_game_block " + where)) {
            int idx = 1;
            if (hasUser) ps.setLong(idx++, userId);
            if (hasNick) ps.setString(idx, nickName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object cidObj = rs.getObject("category_id");
                    Integer cid = cidObj == null ? null : ((Number) cidObj).intValue();
                    Object uidObj = rs.getObject("user_id");
                    Long uid = uidObj == null ? null : ((Number) uidObj).longValue();
                    out.add(new BlockRow(
                            rs.getLong("id"),
                            uid,
                            rs.getString("nick_name"),
                            rs.getString("provider"),
                            rs.getString("vendor_platform"),
                            rs.getString("game_code"),
                            rs.getString("table_tag"),
                            cid,
                            rs.getString("reason"),
                            rs.getString("blocked_by"),
                            rs.getTimestamp("blocked_at"),
                            rs.getTimestamp("expires_at"),
                            rs.getInt("is_active") == 1
                    ));
                }
            }
        } catch (Exception e) {
            logger.warn("UserGameBlock.loadActiveBlocks failed: " + e.getMessage());
        }
        return out;
    }

    /** Test-only — drop cache. */
    static void resetCacheForTests() { CACHE.clear(); }
}

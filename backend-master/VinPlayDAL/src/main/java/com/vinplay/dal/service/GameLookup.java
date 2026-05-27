package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve a {@code (game_id, category_id)} pair from a string {@code gameKey}
 * for the FK-driven commission/rebate path (SUN-GAME-FK Phase 2 dual-write).
 *
 * <p>Two key shapes are recognised:
 * <ul>
 *   <li>{@code awc_<platform>_<gameCode>} — exact match on
 *       {@code games(provider='AWC', vendor_platform=PLATFORM, game_code=GC)},
 *       falling back to the platform stub ({@code game_code='*'}) when
 *       no per-game catalog row exists yet.</li>
 *   <li>{@code gsc_<productCode>_<gameCode>} — exact match on
 *       {@code games(provider='GSC', vendor_platform=PC, game_code=GC)}.</li>
 * </ul>
 *
 * <p>Both lookups also return {@code category_id} so the rebate row can
 * be filtered/grouped by category without joining {@code games} again.
 *
 * <p>Cache: per-process {@link ConcurrentHashMap}. Game catalog rows are
 * effectively immutable once seeded so a hot-path bet doesn't pay a
 * round-trip to MySQL on every callback.
 */
public final class GameLookup {

    private static final Logger logger = Logger.getLogger("dal");

    /** Cache of gameKey → resolved (game_id, category_id). */
    private static final ConcurrentHashMap<String, Result> CACHE = new ConcurrentHashMap<>();

    public static final class Result {
        public final long gameId;       // 0 = not resolved
        public final int  categoryId;   // 0 = not resolved

        Result(long gameId, int categoryId) {
            this.gameId = gameId;
            this.categoryId = categoryId;
        }

        public boolean hasGameId()     { return gameId > 0; }
        public boolean hasCategoryId() { return categoryId > 0; }
    }

    public static final Result NONE = new Result(0L, 0);

    private GameLookup() {}

    public static Result resolve(String gameKey) {
        if (gameKey == null || gameKey.isEmpty()) return NONE;
        Result cached = CACHE.get(gameKey);
        if (cached != null) return cached;

        Result r;
        if (gameKey.startsWith("awc_")) r = resolveAwc(gameKey);
        else if (gameKey.startsWith("gsc_")) r = resolveGsc(gameKey);
        else r = NONE;

        if (r != NONE) CACHE.put(gameKey, r);
        return r;
    }

    private static Result resolveAwc(String gameKey) {
        int p1 = gameKey.indexOf('_', 4);
        if (p1 < 0) return NONE;
        String platform = gameKey.substring(4, p1);
        String gc       = gameKey.substring(p1 + 1);
        // Exact per-game row first, then platform stub (game_code='*').
        Result r = querySingle("AWC", platform, gc);
        if (r != NONE) return r;
        return querySingle("AWC", platform, "*");
    }

    private static Result resolveGsc(String gameKey) {
        int p1 = gameKey.indexOf('_', 4);
        if (p1 < 0) return NONE;
        String pc = gameKey.substring(4, p1);
        String gc = gameKey.substring(p1 + 1);
        return querySingle("GSC", pc, gc);
    }

    private static Result querySingle(String provider, String platform, String gameCode) {
        String sql = "SELECT id, category_id FROM vinplay.games "
                   + "WHERE provider = ? "
                   + "  AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                   + "  AND game_code COLLATE utf8mb4_unicode_ci = ? "
                   + "LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, provider);
            ps.setString(2, platform);
            ps.setString(3, gameCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long gid = rs.getLong("id");
                    int cid = rs.getInt("category_id");
                    return new Result(gid, cid);
                }
            }
        } catch (Exception e) {
            logger.warn("GameLookup query failed provider=" + provider
                    + " platform=" + platform + " gc=" + gameCode + " err=" + e.getMessage());
        }
        return NONE;
    }
}

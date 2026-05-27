package com.vinplay.dal.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * VipPointsService — canonical reader/writer for player VIP point state.
 *
 * <p>Backs the Phase 6 migration off of {@code vinplay.users.vip_point} /
 * {@code users.vip_point_save} / {@code users.money_vp} onto the dedicated
 * {@code vip_points} table + {@code vip_point_log} append-only audit
 * trail.  Mirrors the MoneyGateway feature-flag pattern: every public
 * method has a safe legacy-column fallback so callers can switch one at
 * a time without breaking the read path.
 *
 * <p>VIP points are NOT money — no double-entry, no balance integrity
 * invariants vs money_transaction.  The append-only log is sufficient
 * for analytics + audit per RFC v2 addendum open-question #4.
 *
 * <p><b>Idempotency:</b> every mutating call requires a non-null
 * {@code (reason, sourceTxId)} pair which is UNIQUEd in
 * {@code vip_point_log}.  Re-posting the same pair short-circuits at
 * the log layer and leaves the counters untouched.
 *
 * <p><b>Cache:</b> hot-path reads ({@link #getCurrent}, {@link #getLifetime})
 * consult Hazelcast first.  The {@code cacheVipPoints} map is keyed by
 * userId and holds {@link VipPointsSnapshot}.  Writes invalidate the
 * cache entry; subsequent reads repopulate from MySQL.
 */
public final class VipPointsService {

    private static final Logger logger = Logger.getLogger("backend");

    /** Hazelcast map name. Same map name across services for shared cache. */
    private static final String CACHE_MAP_NAME = "cacheVipPoints";

    /** MySQL pool name. Same as MoneyGateway / UserDaoImpl. */
    private static final String POOL = "mysqlpoolname";

    /** Cache TTL — Hazelcast doesn't drive expiry here; invalidation is explicit. */
    @SuppressWarnings("unused")
    private static final long CACHE_TTL_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    // Canonical reason tags — use these instead of raw strings so a typo
    // becomes a compile error rather than a silent new log category.
    public static final String SOURCE_GAME_WIN         = "GAME_WIN";
    public static final String SOURCE_GAME_BET         = "GAME_BET";
    public static final String SOURCE_VIP_CASHOUT      = "VIP_CASHOUT";
    public static final String SOURCE_HOAN_TRA         = "HOAN_TRA";
    public static final String SOURCE_ADMIN_ADJUST     = "ADMIN_ADJUST";
    public static final String SOURCE_AGENT_TRANSFER   = "AGENT_TRANSFER";
    public static final String SOURCE_DEPOSIT_BONUS    = "DEPOSIT_BONUS";
    /** Reason used by the SQL seed migration; do NOT reuse from app code. */
    public static final String SOURCE_PHASE6_BACKFILL  = "PHASE6_BACKFILL";

    private VipPointsService() {
        // static-only utility
    }

    /**
     * Immutable snapshot of a user's VIP point state.  Returned from
     * {@link #getCurrent} / {@link #getLifetime} and cached in Hazelcast.
     */
    public static final class VipPointsSnapshot implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final long userId;
        public final long currentPoints;
        public final long lifetimePoints;
        public final long updatedAtMs;

        public VipPointsSnapshot(long userId, long currentPoints, long lifetimePoints, long updatedAtMs) {
            this.userId = userId;
            this.currentPoints = currentPoints;
            this.lifetimePoints = lifetimePoints;
            this.updatedAtMs = updatedAtMs;
        }
    }

    /**
     * Apply a (possibly negative) delta to a user's VIP point counters
     * and write one row to {@code vip_point_log}.
     *
     * <p>Idempotency: callers MUST provide a stable
     * {@code (reason, sourceTxId)} pair.  Re-posting the same pair is a
     * no-op (returns {@code true} without mutating either counter).
     *
     * <p>If {@code delta > 0}, both {@code current_points} and
     * {@code lifetime_points} increase.  If {@code delta < 0}, only
     * {@code current_points} decreases ({@code lifetime_points} is
     * monotonic by design — RFC §Phase 6).
     *
     * @param userId       users.id (must be > 0)
     * @param delta        signed change in points
     * @param reason       canonical {@link #SOURCE_GAME_WIN} / etc tag
     * @param sourceTxId   stable idempotency key (e.g. message id, log id)
     * @return {@code true} on success or idempotent-hit, {@code false} on error
     */
    public static boolean addPoints(long userId, long delta, String reason, String sourceTxId) {
        if (userId <= 0 || reason == null || reason.isEmpty()) return false;
        if (delta == 0) return true; // no-op
        // Null sourceTxId would defeat idempotency — synthesise one so the
        // unique key still rejects accidental duplicates within the same ms.
        String txId = (sourceTxId == null || sourceTxId.isEmpty())
                ? ("auto:" + userId + ":" + System.nanoTime())
                : sourceTxId;

        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            conn.setAutoCommit(false);

            // 1. Idempotent log insert.  Unique (reason, sourceTxId) → second
            //    attempt yields 0 rows affected and we short-circuit.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO vip_point_log "
                  + "(user_id, delta, reason, source_tx_id, created_at) "
                  + "VALUES (?,?,?,?, CURRENT_TIMESTAMP(6))")) {
                ps.setLong(1, userId);
                ps.setLong(2, delta);
                ps.setString(3, reason);
                ps.setString(4, txId);
                int inserted = ps.executeUpdate();
                if (inserted == 0) {
                    // Duplicate (reason, sourceTxId) — already applied. Commit
                    // the (empty) tx and report success so callers can retry
                    // freely.
                    conn.commit();
                    logger.info("VipPointsService.addPoints idempotent-hit user=" + userId
                            + " reason=" + reason + " tx=" + txId);
                    return true;
                }
            }

            // 2. Upsert vip_points.  current bumps by delta; lifetime only
            //    bumps on positive deltas (monotonic invariant).
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO vip_points "
                  + "(user_id, current_points, lifetime_points, updated_at) "
                  + "VALUES (?,?,?, CURRENT_TIMESTAMP(6)) "
                  + "ON DUPLICATE KEY UPDATE "
                  + "current_points  = current_points + VALUES(current_points), "
                  + "lifetime_points = lifetime_points + VALUES(lifetime_points), "
                  + "updated_at      = CURRENT_TIMESTAMP(6)")) {
                long lifetimeDelta = delta > 0 ? delta : 0;
                ps.setLong(1, userId);
                ps.setLong(2, delta);
                ps.setLong(3, lifetimeDelta);
                ps.executeUpdate();
            }

            conn.commit();
            invalidateCache(userId);
            return true;
        } catch (Exception e) {
            logger.error("VipPointsService.addPoints failed user=" + userId
                    + " delta=" + delta + " reason=" + reason + " tx=" + txId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Current redeemable VIP points for {@code userId}. Cache-first.
     *
     * @return current_points, or 0 if no row / read error
     */
    public static long getCurrent(long userId) {
        VipPointsSnapshot snap = getSnapshot(userId);
        return snap == null ? 0L : snap.currentPoints;
    }

    /**
     * Lifetime (cumulative earned) VIP points for {@code userId}. Cache-first.
     *
     * @return lifetime_points, or 0 if no row / read error
     */
    public static long getLifetime(long userId) {
        VipPointsSnapshot snap = getSnapshot(userId);
        return snap == null ? 0L : snap.lifetimePoints;
    }

    /** Cache-aware fetch.  Public so admin tools / debug can use it. */
    public static VipPointsSnapshot getSnapshot(long userId) {
        if (userId <= 0) return null;
        IMap<Long, VipPointsSnapshot> cache = cacheMap();
        if (cache != null) {
            try {
                VipPointsSnapshot cached = cache.get(userId);
                if (cached != null) return cached;
            } catch (Exception e) {
                // Cache failure → fall through to DB; never block reads.
                logger.warn("VipPointsService cache read failed user=" + userId + ": " + e.getMessage());
            }
        }
        VipPointsSnapshot loaded = loadFromDb(userId);
        if (loaded != null && cache != null) {
            try { cache.put(userId, loaded); } catch (Exception ignored) {}
        }
        return loaded;
    }

    /**
     * Force-refresh a user's snapshot from MySQL, evicting any cached
     * copy.  Useful after admin-side writes that bypass {@link #addPoints}
     * (the audit-only legacy path).
     */
    public static void invalidateCache(long userId) {
        IMap<Long, VipPointsSnapshot> cache = cacheMap();
        if (cache != null) {
            try { cache.remove(userId); } catch (Exception ignored) {}
        }
    }

    // -----------------------------------------------------------------
    // internal helpers
    // -----------------------------------------------------------------

    private static VipPointsSnapshot loadFromDb(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT current_points, lifetime_points, "
                   + "UNIX_TIMESTAMP(updated_at)*1000 AS updated_ms "
                   + "FROM vip_points WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new VipPointsSnapshot(
                            userId,
                            rs.getLong("current_points"),
                            rs.getLong("lifetime_points"),
                            rs.getLong("updated_ms"));
                }
            }
        } catch (Exception e) {
            logger.error("VipPointsService.loadFromDb error user=" + userId, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static IMap<Long, VipPointsSnapshot> cacheMap() {
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) return null;
            return (IMap<Long, VipPointsSnapshot>) (IMap<?, ?>) hz.getMap(CACHE_MAP_NAME);
        } catch (Exception e) {
            logger.warn("VipPointsService Hazelcast unavailable: " + e.getMessage());
            return null;
        }
    }
}

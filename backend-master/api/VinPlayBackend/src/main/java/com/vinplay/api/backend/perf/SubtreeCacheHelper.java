package com.vinplay.api.backend.perf;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SUN-1108 Tier 2 — Hazelcast cache for agency subtree resolution.
 *
 * <p>Wraps the (agentNick + playerFilter + maxPlayers) → resolved player
 * nicknames computation that's invoked on every c=9541 / c=9843 request.
 * The resolution is expensive (4 SQL hops) but the agency tree changes
 * rarely (minutes-to-hours), so a short-TTL Hazelcast IMap eliminates
 * 95%+ of the SQL load on this code path under normal usage.
 *
 * <p>Operational toggles (env vars, no redeploy needed):
 * <ul>
 *   <li>{@code SUBTREE_CACHE_ENABLED=false} → disable cache, always go to DB</li>
 *   <li>{@code SUBTREE_CACHE_TTL_SECONDS=600} → override default 10-minute TTL</li>
 * </ul>
 *
 * <p>Safety: a Hazelcast outage / serialization error never fails the
 * request. The {@link #get(String)} call returns null on any exception,
 * which the caller treats as a cache miss → falls through to direct
 * computation. Same for {@link #put(String, CachedScope)}.
 *
 * <p>Invalidation: relies on TTL only. Rare agent/player creation events
 * become visible after at most {@code SUBTREE_CACHE_TTL_SECONDS}. If
 * faster invalidation is needed, call {@link #invalidate(String)} from
 * the agent/player creation processors.
 */
public final class SubtreeCacheHelper {

    private static final Logger logger = Logger.getLogger("backend");
    private static final String MAP_NAME = "agent_subtree_cache_v1";

    private SubtreeCacheHelper() {}

    /** Default 10-minute TTL — agency tree rarely changes faster than this. */
    private static final int DEFAULT_TTL_SECONDS = 600;

    private static boolean isEnabled() {
        String v = System.getenv("SUBTREE_CACHE_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    private static int ttlSeconds() {
        String v = System.getenv("SUBTREE_CACHE_TTL_SECONDS");
        if (v == null) return DEFAULT_TTL_SECONDS;
        try { return Integer.parseInt(v); } catch (NumberFormatException ignored) { return DEFAULT_TTL_SECONDS; }
    }

    /**
     * Build cache key from the request shape. Filter + maxPlayers are
     * part of the key because the SQL applies a LIKE filter early —
     * different filters return different sets.
     */
    public static String key(String agentNick, String playerFilter, int maxPlayers) {
        return key(agentNick, playerFilter, maxPlayers, true);
    }

    /**
     * Overload bao gồm hideBot trong cache key.
     * Bot-filtered và non-filtered scopes được cache tách biệt để không
     * bao giờ trả nhầm kết quả bot/no-bot cho nhau.
     */
    public static String key(String agentNick, String playerFilter, int maxPlayers, boolean hideBot) {
        StringBuilder sb = new StringBuilder("subtree:v1:");
        sb.append(agentNick == null ? "" : agentNick);
        sb.append(':');
        sb.append(playerFilter == null ? "" : playerFilter);
        sb.append(':');
        sb.append(maxPlayers);
        sb.append(':');
        sb.append(hideBot ? "1" : "0");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static CachedScope get(String key) {
        if (!isEnabled() || key == null) return null;
        try {
            IMap<String, CachedScope> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            return m.get(key);
        } catch (Exception e) {
            logger.warn("SubtreeCacheHelper.get failed (cache miss fallback): " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static void put(String key, CachedScope value) {
        if (!isEnabled() || key == null || value == null) return;
        try {
            IMap<String, CachedScope> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            m.put(key, value, ttlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("SubtreeCacheHelper.put failed (skip cache): " + e.getMessage());
        }
    }

    /** Manual invalidation hook — usable from agent/player creation processors. */
    @SuppressWarnings("unchecked")
    public static void invalidate(String key) {
        if (key == null) return;
        try {
            IMap<String, CachedScope> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            m.delete(key);
        } catch (Exception ignored) {}
    }

    /**
     * Serializable wrapper around the resolved player scope. Mirrors the
     * private PlayerScope inner class in GetBettingHistoryByAgentProcessor
     * but lives in DAL so multiple processors can share the same cache.
     */
    public static final class CachedScope implements Serializable {
        private static final long serialVersionUID = 1L;
        private final ArrayList<String> nicknames;
        private final boolean truncated;

        public CachedScope(List<String> nicknames, boolean truncated) {
            this.nicknames = nicknames == null ? new ArrayList<>() : new ArrayList<>(nicknames);
            this.truncated = truncated;
        }

        public List<String> getNicknames() { return nicknames; }
        public boolean isTruncated() { return truncated; }
    }
}

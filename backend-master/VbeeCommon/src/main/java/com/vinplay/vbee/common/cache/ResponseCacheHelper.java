package com.vinplay.vbee.common.cache;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * SUN-1108 Tier 4 — Hazelcast response cache for agency report endpoints.
 *
 * <p>Wraps the entire JSON response of c=9541 / c=9843 / c=9750 keyed by
 * request shape (cmd + agent + date range + paging + sort). Same agent
 * refreshing the same dashboard pulls the cached response instead of
 * re-running the underlying scan + summary aggregation.
 *
 * <p>SUN-1155: moved from VinPlayBackend to VbeeCommon so cross-server
 * write paths (e.g. c=3083 ClaimCashbackProcessor in VinPlayPortal) can
 * call {@link #invalidateForAgent(int)} after a state change. A second
 * IMap, {@link #INDEX_MAP_NAME}, holds a reverse lookup from agentId to
 * the set of cache keys belonging to that agent — written by
 * {@link #putWithAgent(String, String, boolean, Integer)} and consumed
 * by {@link #invalidateForAgent(int)}.
 *
 * <p>TTL strategy:
 * <ul>
 *   <li>30 s for "current period" (date range includes today) — short
 *       enough to feel real-time, long enough to absorb the rapid-fire
 *       refresh pattern when QA / agents tab between sub-pages</li>
 *   <li>300 s for "historical only" (et &lt; today) — data is immutable</li>
 * </ul>
 *
 * <p>Operational toggles:
 * <ul>
 *   <li>{@code RESPONSE_CACHE_ENABLED=false} → disable, always recompute</li>
 *   <li>{@code RESPONSE_CACHE_LIVE_TTL=30} → override 30s current TTL</li>
 *   <li>{@code RESPONSE_CACHE_HIST_TTL=300} → override 300s historical TTL</li>
 * </ul>
 *
 * <p>Safety: any Hazelcast failure → return null from {@link #get(String)}
 * (cache miss) and silently skip {@link #put(String, String, boolean)}.
 * Cache faults never fail the request.
 */
public final class ResponseCacheHelper {

    private static final Logger logger = Logger.getLogger("backend");

    /** Forward map: hashed cache key → JSON response. */
    public static final String MAP_NAME = "agency_response_cache_v1";

    /**
     * Reverse index: {@code "agent:{id}"} → newline-separated forward
     * cache keys. Lets a write path invalidate every cached response
     * for one agent without scanning the whole forward map.
     */
    public static final String INDEX_MAP_NAME = "agency_response_cache_index_v1";

    private ResponseCacheHelper() {}

    private static final int DEFAULT_LIVE_TTL_SECONDS = 30;
    private static final int DEFAULT_HIST_TTL_SECONDS = 300;
    /** Index entries outlive the cache by this slack so reads of the
     *  index never "see" a longer set than the forward map actually
     *  contains. Stale-but-deleted forward entries are no-ops on
     *  invalidate, so the slack is safe. */
    private static final int INDEX_TTL_SLACK_SECONDS = 60;

    private static boolean isEnabled() {
        String v = System.getenv("RESPONSE_CACHE_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    private static int liveTtlSeconds() {
        String v = System.getenv("RESPONSE_CACHE_LIVE_TTL");
        if (v == null) return DEFAULT_LIVE_TTL_SECONDS;
        try { return Integer.parseInt(v); } catch (NumberFormatException ignored) { return DEFAULT_LIVE_TTL_SECONDS; }
    }

    private static int histTtlSeconds() {
        String v = System.getenv("RESPONSE_CACHE_HIST_TTL");
        if (v == null) return DEFAULT_HIST_TTL_SECONDS;
        try { return Integer.parseInt(v); } catch (NumberFormatException ignored) { return DEFAULT_HIST_TTL_SECONDS; }
    }

    /**
     * Stable cache key — SHA-256 over a deterministic concatenation of
     * the request fields that affect the response. Length-bounded to
     * keep Hazelcast keys compact.
     */
    public static String key(String cmd, Object... fields) {
        StringBuilder sb = new StringBuilder("resp:v1:").append(cmd);
        for (Object f : fields) {
            sb.append('|').append(f == null ? "" : f.toString());
        }
        return sha256Hex(sb.toString());
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(64);
            for (byte b : h) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            return s;
        }
    }

    public static String get(String key) {
        if (!isEnabled() || key == null) return null;
        try {
            IMap<String, String> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            return m.get(key);
        } catch (Exception e) {
            logger.warn("ResponseCacheHelper.get failed (cache miss fallback): " + e.getMessage());
            return null;
        }
    }

    public static void put(String key, String json, boolean isHistorical) {
        if (!isEnabled() || key == null || json == null) return;
        try {
            IMap<String, String> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            int ttl = isHistorical ? histTtlSeconds() : liveTtlSeconds();
            m.put(key, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("ResponseCacheHelper.put failed (skip cache): " + e.getMessage());
        }
    }

    /**
     * Like {@link #put(String, String, boolean)} but also records the
     * cache key under the agent's reverse-index entry so the write
     * path (c=3083 claim, future approve/reject flows) can call
     * {@link #invalidateForAgent(int)} and drop only the affected
     * agent's cached responses.
     *
     * <p>Failures in the index update are logged and swallowed —
     * forward cache write still happens. Worst case is a stale entry
     * left until TTL, which matches the pre-SUN-1155 behavior.
     */
    public static void putWithAgent(String key, String json, boolean isHistorical, Integer agentId) {
        put(key, json, isHistorical);
        if (!isEnabled() || key == null || json == null) return;
        if (agentId == null || agentId <= 0) return;
        try {
            IMap<String, String> idx = HazelcastClientFactory.getInstance().getMap(INDEX_MAP_NAME);
            String idxKey = "agent:" + agentId;
            int ttl = (isHistorical ? histTtlSeconds() : liveTtlSeconds()) + INDEX_TTL_SLACK_SECONDS;
            idx.lock(idxKey);
            try {
                Set<String> keys = parseKeys(idx.get(idxKey));
                if (keys.add(key)) {
                    idx.put(idxKey, joinKeys(keys), ttl, TimeUnit.SECONDS);
                }
            } finally {
                idx.unlock(idxKey);
            }
        } catch (Exception e) {
            logger.warn("ResponseCacheHelper.putWithAgent index update failed (forward cache still wrote): "
                    + e.getMessage());
        }
    }

    /**
     * Drop every cached response associated with this agent. Called
     * from write paths that change rebate state visibly to the agent
     * (e.g. c=3083 claim, future approve/reject endpoints).
     *
     * <p>Idempotent. Safe to call when no entries exist (no-op).
     * Returns the number of forward-map entries deleted.
     */
    public static int invalidateForAgent(int agentId) {
        if (agentId <= 0) return 0;
        int deleted = 0;
        try {
            IMap<String, String> idx = HazelcastClientFactory.getInstance().getMap(INDEX_MAP_NAME);
            IMap<String, String> fwd = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            String idxKey = "agent:" + agentId;
            idx.lock(idxKey);
            try {
                String existing = idx.get(idxKey);
                if (existing == null || existing.isEmpty()) return 0;
                for (String k : existing.split("\n")) {
                    if (!k.isEmpty()) {
                        fwd.delete(k);
                        deleted++;
                    }
                }
                idx.delete(idxKey);
            } finally {
                idx.unlock(idxKey);
            }
        } catch (Exception e) {
            logger.warn("ResponseCacheHelper.invalidateForAgent failed agent=" + agentId
                    + " err=" + e.getMessage());
        }
        return deleted;
    }

    /**
     * Decide whether the date range is purely historical (allows longer
     * TTL). "et" must be strictly before today.
     */
    public static boolean isHistoricalOnly(String dateTo) {
        if (dateTo == null || dateTo.isEmpty()) return false;
        try {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(new java.util.Date());
            String et = dateTo.contains(" ") ? dateTo.substring(0, 10) : dateTo;
            return et.compareTo(today) < 0;
        } catch (Exception ignored) { return false; }
    }

    public static void invalidate(String key) {
        if (key == null) return;
        try {
            IMap<String, String> m = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            m.delete(key);
        } catch (Exception ignored) {}
    }

    private static Set<String> parseKeys(String s) {
        Set<String> out = new HashSet<>();
        if (s == null || s.isEmpty()) return out;
        for (String k : s.split("\n")) if (!k.isEmpty()) out.add(k);
        return out;
    }

    private static String joinKeys(Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String k : keys) {
            if (!first) sb.append('\n');
            sb.append(k);
            first = false;
        }
        return sb.toString();
    }
}

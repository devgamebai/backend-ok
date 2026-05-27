package com.vinplay.vbee.common.rtp;

import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import org.apache.log4j.Logger;

/**
 * Resolves the effective win-rate percentage for a user in a given game.
 *
 * Precedence (first hit wins):
 *   1. user + game override         — cacheUserRtpOverride["<user_id>:<game_code>"]
 *   2. user + 'ALL' override        — cacheUserRtpOverride["<user_id>:ALL"]
 *   3. game default                 — cacheGameRtp[game_code]
 *   4. hardcoded fallback           — DEFAULT_FALLBACK_PCT
 *
 * Game engines call {@link #effectivePct(long, String)} on the hot path.
 * Caches are populated by the admin config processors (c=9770-9775) and never
 * written to by game code, so reads are lock-free.
 *
 * Kill switch: env var RTP_ENGINE_DISABLED=1 → always returns DEFAULT_FALLBACK_PCT.
 */
public final class RtpResolver {
    private static final Logger logger = Logger.getLogger(RtpResolver.class);

    /** Hardcoded fallback if no config is present. Matches current SlotHouseEdge baseline. */
    public static final double DEFAULT_FALLBACK_PCT = 92.0d;

    /** Sentinel game code meaning "apply to every game this user plays". */
    public static final String GAME_CODE_ALL = "ALL";

    /** Minimum allowed win-rate — hard floor to prevent player-hostile settings. */
    public static final double HARD_FLOOR_PCT = 40.0d;

    /** Maximum allowed win-rate — hard ceiling to prevent fund drain. */
    public static final double HARD_CEIL_PCT = 99.0d;

    private static final String CACHE_GAME = "cacheGameRtp";
    private static final String CACHE_USER = "cacheUserRtpOverride";

    private RtpResolver() {}

    /**
     * Returns the effective win-rate pct for a user+game combination.
     * Never throws — on any error, returns {@link #DEFAULT_FALLBACK_PCT}.
     */
    public static double effectivePct(long userId, String gameCode) {
        if (isKilled()) return DEFAULT_FALLBACK_PCT;
        if (gameCode == null || gameCode.isEmpty()) return DEFAULT_FALLBACK_PCT;

        try {
            DistCache<String, Object> userMap = CacheFactory.get(CACHE_USER, Object.class);

            // 1. user + game override
            Object v = userMap.get(userId + ":" + gameCode);
            if (v != null) return clamp(toDouble(v));

            // 2. user + ALL override
            v = userMap.get(userId + ":" + GAME_CODE_ALL);
            if (v != null) return clamp(toDouble(v));

            // 3. game default
            DistCache<String, Object> gameMap = CacheFactory.get(CACHE_GAME, Object.class);
            v = gameMap.get(gameCode);
            if (v != null) return clamp(toDouble(v));
        } catch (Exception e) {
            logger.warn("RtpResolver: failed to resolve user=" + userId + " game=" + gameCode
                    + " — falling back to " + DEFAULT_FALLBACK_PCT, e);
        }

        // 4. hardcoded fallback
        return DEFAULT_FALLBACK_PCT;
    }

    /** Backward-compatible overload for game-level resolution without user override. */
    public static double effectivePct(String gameCode) {
        return effectivePct(0L, gameCode);
    }

    /** Clamp to [HARD_FLOOR_PCT, HARD_CEIL_PCT]. */
    public static double clamp(double pct) {
        if (pct < HARD_FLOOR_PCT) return HARD_FLOOR_PCT;
        if (pct > HARD_CEIL_PCT) return HARD_CEIL_PCT;
        return pct;
    }

    private static double toDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); }
            catch (NumberFormatException e) { return DEFAULT_FALLBACK_PCT; }
        }
        return DEFAULT_FALLBACK_PCT;
    }

    private static boolean isKilled() {
        String v = System.getenv("RTP_ENGINE_DISABLED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}

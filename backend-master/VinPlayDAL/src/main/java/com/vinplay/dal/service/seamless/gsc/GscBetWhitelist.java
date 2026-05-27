package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bet-time table gate for GSC seamless wallet callbacks.
 *
 * <p>Source of truth is the {@code gsc_game_catalog} table — every
 * {@code (product_code, game_code)} pair has an {@code active} flag that
 * already determines whether the game shows in our lobby. This class
 * reuses that flag at bet time so a player who reaches a deactivated
 * table (via deep link, browser back-button, stale cache, or directly
 * inside the provider's own lobby — e.g. Evo) is refused with
 * {@code code=2000} ({@code PRODUCT_UNDER_MAINTENANCE}) instead of
 * having the bet quietly accepted.
 *
 * <p>Env-var overrides take precedence (deny wins, then env-allow when
 * set, then catalog active flag):
 * <ul>
 *   <li>{@code GSC_BET_WHITELIST_ENFORCE} — master toggle. Default
 *       {@code false} → {@link #isAllowed(int, String)} returns true for
 *       every input. Production posture is unchanged until ops sets it
 *       explicitly.</li>
 *   <li>{@code GSC_BLOCKED_PRODUCTS} / {@code GSC_BLOCKED_GAMES} —
 *       explicit deny lists. Win over the catalog and over the env-allow
 *       list. Useful for a fast surgical block of one bad table without
 *       running an UPDATE on the catalog.</li>
 *   <li>{@code GSC_BET_ALLOWED_GAMES} — explicit allow list (separate
 *       from {@code GSC_WHITELIST_GAMES} which is read by the lobby
 *       filter only — coupling them would force ops to maintain the same
 *       full enumeration in two places). When non-empty, only these
 *       game_codes pass; the catalog flag is ignored. When empty (the
 *       common case), the catalog flag decides.</li>
 *   <li>{@code GSC_BET_UNKNOWN_GAME_POLICY} — {@code allow} (default) or
 *       {@code deny}. Decides what happens for a bet on a
 *       {@code (product, game_code)} pair that has no row in the catalog
 *       (e.g. a brand-new provider game we have not synced yet).</li>
 * </ul>
 *
 * <p><b>Caching.</b> Catalog lookups are cached in-process for 60 seconds
 * to keep withdraw latency unaffected during peak bursts (~30 TPS). A
 * sustained drift between the env vars / catalog and runtime behaviour
 * is therefore at most {@link #CACHE_TTL_MS}; for instant flips, recreate
 * the container after editing the catalog.
 */
public final class GscBetWhitelist {

    private static final Logger logger = Logger.getLogger("backend");

    /** How long a {@code (product, game) → active} answer is cached. */
    private static final long CACHE_TTL_MS = 60_000L;

    /** {@code product_code + ":" + game_code} → {expiresAt, active}. */
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private GscBetWhitelist() {}

    /** @return {@code true} if the (product, game) pair may be bet on. */
    public static boolean isAllowed(int productCode, String gameCode) {
        if (!enforceEnabled()) return true;

        // 1. Explicit env-var deny lists win first.
        if (productCode > 0 && blockedProducts().contains(productCode)) {
            return false;
        }
        if (gameCode != null && !gameCode.isEmpty() && blockedGames().contains(gameCode)) {
            return false;
        }

        // 2. Explicit env-var allow list, when populated, overrides the
        //    catalog. Lets ops force-allow a game that hasn't been synced
        //    into the catalog yet without touching SQL.
        Set<String> allowedGames = allowedGames();
        if (!allowedGames.isEmpty()
                && gameCode != null && !gameCode.isEmpty()) {
            return allowedGames.contains(gameCode);
        }

        // 3. Default path — fall back to the catalog's `active` flag.
        if (productCode <= 0 || gameCode == null || gameCode.isEmpty()) {
            // Insufficient identification — let it pass; the rest of the
            // dispatch pipeline will reject on its own constraints.
            return true;
        }
        Boolean active = lookupActiveCached(productCode, gameCode);
        if (active == null) {
            return unknownGameAllowed();
        }
        return active;
    }

    public static boolean enforceEnabled() {
        return parseBool(System.getenv("GSC_BET_WHITELIST_ENFORCE"), false);
    }

    private static boolean unknownGameAllowed() {
        String v = System.getenv("GSC_BET_UNKNOWN_GAME_POLICY");
        if (v == null || v.isEmpty()) return true;
        return !v.equalsIgnoreCase("deny");
    }

    private static Set<Integer> blockedProducts() {
        return parseInts(System.getenv("GSC_BLOCKED_PRODUCTS"));
    }

    private static Set<String> blockedGames() {
        return parseStrings(System.getenv("GSC_BLOCKED_GAMES"));
    }

    private static Set<String> allowedGames() {
        return parseStrings(System.getenv("GSC_BET_ALLOWED_GAMES"));
    }

    /** Returns {@code true}/{@code false} from catalog, or {@code null} if no row. */
    private static Boolean lookupActiveCached(int productCode, String gameCode) {
        String key = productCode + ":" + gameCode;
        long now = System.currentTimeMillis();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && cached.expiresAt > now) {
            return cached.active;
        }
        Boolean fresh = lookupActive(productCode, gameCode);
        CACHE.put(key, new CacheEntry(now + CACHE_TTL_MS, fresh));
        return fresh;
    }

    private static Boolean lookupActive(int productCode, String gameCode) {
        String sql = "SELECT active FROM gsc_game_catalog WHERE product_code = ? AND game_code = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productCode);
            ps.setString(2, gameCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 1;
                }
                return null;
            }
        } catch (Exception e) {
            // Fail open on transient DB errors — better to let a bet through
            // than to wrongly reject a legitimate one because MySQL hiccupped.
            // The wallet primitive's atomic checks still apply downstream.
            logger.warn("GscBetWhitelist.lookupActive failed (fail-open) "
                    + "product=" + productCode + " game=" + gameCode + ": " + e.getMessage());
            return null;
        }
    }

    /** Test-only — drop the cache between unit tests. */
    static void resetCacheForTests() { CACHE.clear(); }

    private static boolean parseBool(String raw, boolean fallback) {
        if (raw == null || raw.isEmpty()) return fallback;
        return raw.equalsIgnoreCase("true") || raw.equals("1");
    }

    private static Set<Integer> parseInts(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Set<Integer> out = new HashSet<>();
        for (String p : raw.split(",")) {
            p = p.trim();
            if (p.isEmpty()) continue;
            try { out.add(Integer.parseInt(p)); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    private static Set<String> parseStrings(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (String s : raw.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static final class CacheEntry {
        final long expiresAt;
        final Boolean active; // null = no row in catalog
        CacheEntry(long expiresAt, Boolean active) {
            this.expiresAt = expiresAt;
            this.active = active;
        }
    }
}

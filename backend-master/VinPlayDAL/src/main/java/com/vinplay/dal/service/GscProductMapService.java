package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SUN-850: resolve a GSC+ {@code product_code} to a
 * {@code vinplay.game_commission_rate} game_key.
 *
 * <p>Seamless-wallet callbacks ({@code WithdrawProcess}, {@code DepositProcess})
 * receive a numeric {@code product_code} from GSC (e.g. 1012 SBO, 1046 Saba,
 * 1002 Evolution). That code isn't what {@code game_commission_rate} keys on —
 * agents configure rates per {@code game_key} string ({@code "sbo"}, {@code "ag"},
 * {@code "wm"}, …). The mapping is admin-editable in {@code vinplay.gsc_product_map}
 * so production can add new providers without a redeploy.
 *
 * <p>Contract: {@link #resolveGameKey(int)} returns {@code null} when no active
 * row matches. Callers that hit null should fall back to the agent's global
 * {@code useragent.commission_rate} (matches the existing
 * {@code LogMoneyUserExtraProcessor.effectiveRateFor} null-gameKey branch).
 *
 * <p>Cache: in-memory with a 5-minute TTL. CMS CRUD endpoints must call
 * {@link #invalidate()} after each mutation — the cache is per-JVM, but in
 * staging/prod we have one portal-api, one vbee-consumer, one game-thirdparty;
 * an admin edit triggers invalidate across all three via the RMQ broadcast
 * shipped by the CRUD endpoint (future subtask F).
 */
public class GscProductMapService {

    private static final Logger logger = Logger.getLogger("service");
    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;

    /** Wildcard game_type used as the per-provider fallback row. */
    private static final String WILDCARD = "*";

    /** Cache keyed by (product_code, game_type). */
    private static volatile Map<String, String> cache = null;
    private static final AtomicLong cacheLoadedAt = new AtomicLong(0L);


    /**
     * Return the game_key for a given GSC product_code, or {@code null} when
     * no active mapping exists. Callers treat {@code null} as "use global rate".
     *
     * <p>Equivalent to {@link #resolveGameKey(int, String)} with a null
     * {@code gameType} — resolves to the provider-level wildcard row only.
     */
    public static String resolveGameKey(int productCode) {
        return resolveGameKey(productCode, null);
    }

    /**
     * SUN-857: resolve (product_code, game_type) → game_key. Looks up the
     * exact (pc, gt) row first; falls back to the wildcard (pc, '*') row
     * when no per-game_type override exists. Returns {@code null} when
     * neither matches so callers apply the agent's global rate.
     *
     * <p>Example: Evolution (pc=1002) is the provider-level fallback with
     * game_key='ag'. If ops adds (1002, 'Sicbo', game_key='sicbo') then bets
     * where GSC sends {@code game_type="Sicbo"} resolve to our {@code sicbo}
     * rate while everything else Evolution still lands on {@code ag}.
     */
    public static String resolveGameKey(int productCode, String gameType) {
        // SUN-857: per-(product_code, game_type) override in gsc_product_map,
        // with wildcard provider-level fallback. SUN-865 doesn't go through
        // this path anymore — live-game commission uses the synthetic key
        // `gsc_<pc>_<game_code>` resolved directly in vbee against
        // game_commission_rate. This lookup stays for pre-SUN-865 clients
        // still passing game_type.
        Map<String, String> map = getCache();
        if (gameType != null && !gameType.isEmpty()) {
            String hit = map.get(productCode + "|" + gameType);
            if (hit != null) return hit;
        }
        return map.get(productCode + "|" + WILDCARD);
    }

    /**
     * Clear the in-process cache so the next lookup reloads from DB.
     * Call this from the admin CMS CRUD endpoints after insert/update/delete.
     */
    public static void invalidate() {
        cache = null;
        cacheLoadedAt.set(0L);
    }

    private static Map<String, String> getCache() {
        Map<String, String> existing = cache;
        long loadedAt = cacheLoadedAt.get();
        if (existing != null && (System.currentTimeMillis() - loadedAt) < CACHE_TTL_MS) {
            return existing;
        }
        synchronized (GscProductMapService.class) {
            // Double-check after acquiring the lock
            existing = cache;
            loadedAt = cacheLoadedAt.get();
            if (existing != null && (System.currentTimeMillis() - loadedAt) < CACHE_TTL_MS) {
                return existing;
            }
            existing = loadFromDb();
            cache = existing;
            cacheLoadedAt.set(System.currentTimeMillis());
            return existing;
        }
    }

    private static Map<String, String> loadFromDb() {
        // Keyed as "{product_code}|{game_type}" — wildcard rows use game_type='*'
        // and act as the provider-level fallback.
        Map<String, String> out = new HashMap<>();
        // Try the SUN-857 schema first (game_type column). If the migration
        // hasn't been applied, fall back to the pre-857 schema (product_code
        // only) and synthesize wildcard keys — keeps rolling deploys working.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT product_code, game_type, game_key FROM vinplay.gsc_product_map WHERE is_active = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pc = rs.getInt("product_code");
                    String gt = rs.getString("game_type");
                    String gk = rs.getString("game_key");
                    if (gt == null || gt.trim().isEmpty()) gt = WILDCARD;
                    if (gk != null && !gk.trim().isEmpty()) {
                        out.put(pc + "|" + gt.trim(), gk.trim());
                    }
                }
            }
            return out;
        } catch (Exception e) {
            logger.warn("GscProductMapService: SUN-857 schema load failed, retrying legacy — " + e.getMessage());
        }
        // Legacy fallback: pre-857 schema with no game_type column. Every row
        // becomes a wildcard (pc|'*') entry.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT product_code, game_key FROM vinplay.gsc_product_map WHERE is_active = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pc = rs.getInt("product_code");
                    String gk = rs.getString("game_key");
                    if (gk != null && !gk.trim().isEmpty()) {
                        out.put(pc + "|" + WILDCARD, gk.trim());
                    }
                }
            }
        } catch (Exception e) {
            // Table may not exist yet in environments that haven't run any
            // migration — don't blow up, return empty so callers fall back
            // to global rate.
            logger.warn("GscProductMapService: legacy load failed, empty map — " + e.getMessage());
        }
        return out;
    }
}

package com.vinplay.dal.service;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Layered lookup over {@code vinplay.game_commission_rate} for a given
 * {@code (agent_nickname, game_key)} pair. Extracted from
 * {@code LogMoneyUserExtraProcessor.effectiveRateFor} so other callers
 * (RealTimeCommission, CommissionCronJob, future seamless hooks) share
 * the same resolution rules.
 *
 * <p>Resolution chain — first match wins:
 * <ol>
 *   <li><b>EXACT</b> — agent has a row with the exact game_key
 *       (e.g. {@code "gsc_1052_20101"}). Ops can pin a per-table rate.</li>
 *   <li><b>CATEGORY</b> — the synthetic key resolves to a category via
 *       {@code gsc_game_catalog} / {@code awc_game_catalog} / offline
 *       category map → {@code live_cat_<Category>} /
 *       {@code offline_cat_<Category>}. Inherits from the team's per-
 *       category configuration (the common case).</li>
 *   <li><b>PROVIDER</b> — fall back to {@code live_provider_<pc>}
 *       (NEW). Lets ops set a flat "all Dream Gaming = X%" rate for
 *       providers whose categories are still NULL or whose wager
 *       pushes lack a game_code. Skips when the gameKey isn't a GSC
 *       synthetic.</li>
 *   <li><b>NONE</b> — no row matched. Caller decides whether to use a
 *       global default or zero. Matches the prior behavior of returning
 *       0.0 for unknown game_keys.</li>
 * </ol>
 *
 * <p>The resolver returns a {@link Result} carrying the rate plus the
 * layer that produced it, so audit trails (rebate_logs.note) can record
 * which fallback ran.
 */
public final class CommissionRateResolver {

    private static final Logger logger = Logger.getLogger("dal");

    public enum Layer { EXACT, CATEGORY, PROVIDER, NONE }

    public static final class Result {
        public final double rate;
        public final String matchedKey;   // null when NONE
        public final Layer layer;

        Result(double rate, String matchedKey, Layer layer) {
            this.rate = rate;
            this.matchedKey = matchedKey;
            this.layer = layer;
        }

        public static Result none() { return new Result(0.0, null, Layer.NONE); }
    }

    private CommissionRateResolver() {}

    /**
     * Walk the resolution chain. {@code conn} is reused for the
     * exact / category / provider lookups so callers that already hold a
     * {@code mysqlpoolname} connection avoid pool churn. Pass
     * {@code null} to let the resolver borrow a connection per query
     * (slower but safe).
     */
    public static Result resolve(Connection conn, String agentNickname, String gameKey) {
        if (agentNickname == null || agentNickname.isEmpty()) return Result.none();
        if (gameKey == null || gameKey.isEmpty()) return Result.none();

        // Layer 1 — exact match on game_key (per-table or per-game).
        Double r = lookupRate(conn, agentNickname, gameKey);
        if (r != null) return new Result(r, gameKey, Layer.EXACT);

        // Layer 2A — FK-based category lookup (SUN-GAME-FK Phase 3).
        // Resolve game → games.category_id → game_commission_rate by
        // category_id. Faster + typo-proof vs the legacy live_cat_<name>
        // string match. Falls through to legacy if either side is
        // un-backfilled (NULL category_id on rate row, or no games row
        // for this gameKey).
        GameLookup.Result fk = GameLookup.resolve(gameKey);
        if (fk.hasCategoryId()) {
            Double catFk = lookupRateByCategoryId(conn, agentNickname, fk.categoryId);
            if (catFk != null) {
                return new Result(catFk, "category_id=" + fk.categoryId, Layer.CATEGORY);
            }
        }

        // Layer 2B — legacy category lookup via string keys
        // ("live_cat_<X>" / "offline_cat_<X>"). Stays for backward-
        // compat with rate rows whose category_id wasn't backfilled
        // and for the offline path which doesn't sit in `games` yet.
        String catKey = categoryKeyFor(gameKey);
        if (catKey != null) {
            r = lookupRate(conn, agentNickname, catKey);
            if (r != null) return new Result(r, catKey, Layer.CATEGORY);
        }

        // Layer 3 — provider-level fallback (SUN-1201 follow-up).
        // Only meaningful for GSC-synthetic keys, where the product_code
        // is embedded. Lets ops configure "live_provider_1052 = 1.25"
        // once and have it apply to every Dream bet whose category
        // didn't resolve.
        String provKey = providerKeyFor(gameKey);
        if (provKey != null) {
            r = lookupRate(conn, agentNickname, provKey);
            if (r != null) return new Result(r, provKey, Layer.PROVIDER);
        }

        return Result.none();
    }

    /**
     * SUN-GAME-FK Phase 3: lookup rate via the typed category_id FK.
     * Reads rows where {@code game_commission_rate.category_id} matches —
     * those rows were backfilled in Phase 1 from the legacy
     * {@code live_cat_<name>} string keys, so most agents already have
     * coverage. Returns null when no row matches.
     */
    public static Double lookupRateByCategoryId(Connection conn, String agentNickname, int categoryId) {
        if (agentNickname == null || agentNickname.isEmpty() || categoryId <= 0) return null;
        boolean borrowed = false;
        Connection c = conn;
        try {
            if (c == null) {
                c = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                        .getConnection("mysqlpoolname");
                borrowed = true;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT rate FROM vinplay.game_commission_rate "
                  + "WHERE agent_nickname = ? AND category_id = ? LIMIT 1")) {
                ps.setString(1, agentNickname);
                ps.setInt(2, categoryId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("rate");
                }
            }
        } catch (Exception e) {
            logger.warn("CommissionRateResolver.lookupRateByCategoryId failed nick="
                    + agentNickname + " catId=" + categoryId + " err=" + e.getMessage());
        } finally {
            if (borrowed && c != null) {
                try { c.close(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Direct rate lookup — used by both this resolver and any caller
     *  that already knows the exact key it wants (no chain). */
    public static Double lookupRate(Connection conn, String agentNickname, String gameKey) {
        boolean borrowed = false;
        Connection c = conn;
        try {
            if (c == null) {
                c = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                        .getConnection("mysqlpoolname");
                borrowed = true;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT rate FROM vinplay.game_commission_rate " +
                    "WHERE agent_nickname = ? AND game_key = ? LIMIT 1")) {
                ps.setString(1, agentNickname);
                ps.setString(2, gameKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("rate");
                }
            }
        } catch (Exception e) {
            logger.warn("CommissionRateResolver.lookupRate failed nick="
                    + agentNickname + " gameKey=" + gameKey + " err=" + e.getMessage());
        } finally {
            if (borrowed && c != null) {
                try { c.close(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Map a per-game key to its category lookup key.
     * <ul>
     *   <li>{@code gsc_<pc>_<gc>}  → {@code live_cat_<Category>} via
     *       {@code gsc_game_catalog.category}</li>
     *   <li>{@code awc_<platform>_<gc>} → {@code live_cat_<Category>}
     *       via {@code awc_game_catalog} / {@code awc_platform_map}</li>
     *   <li>Offline game_key in OfflineGameCategoryMap →
     *       {@code offline_cat_<Category>}</li>
     * </ul>
     */
    public static String categoryKeyFor(String gameKey) {
        if (gameKey == null || gameKey.isEmpty()) return null;
        if (gameKey.startsWith("gsc_")) {
            int p1 = gameKey.indexOf('_', 4);
            if (p1 < 0) return null;
            String pcStr = gameKey.substring(4, p1);
            String gc = gameKey.substring(p1 + 1);
            int pc;
            try { pc = Integer.parseInt(pcStr); } catch (NumberFormatException e) { return null; }
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                    .getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT category FROM vinplay.gsc_game_catalog "
                       + "WHERE product_code = ? AND game_code = ? LIMIT 1")) {
                ps.setInt(1, pc);
                ps.setString(2, gc);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String cat = rs.getString("category");
                        if (cat != null && !cat.isEmpty()) return "live_cat_" + cat;
                    }
                }
            } catch (Exception ignore) { /* catalog table may be absent — silent fallback */ }
            return null;
        }
        if (gameKey.startsWith("awc_")) {
            // SUN-GAME-FK Phase 4 cleanup: AWC category resolution moved
            // entirely to Layer 2A (GameLookup → games.category_id JOIN).
            // The legacy awc_game_catalog + awc_platform_map reads — and
            // the hardcoded normalizeAwcCategory translations
            // (CASINO→Baccarat, FISH→Fishing, …) — are gone. Returning
            // null here forces the resolver to fall through to
            // PROVIDER / NONE when Layer 2A misses, which is correct:
            // a new AWC platform not yet seeded in `games` should
            // resolve to rate=0 until ops onboards it.
            //
            // Onboarding flow for a new AWC platform:
            //   INSERT INTO games (provider, vendor_platform, game_code,
            //                      game_name, category_id) VALUES
            //   ('AWC', '<NEW_PROVIDER>', '*', '<Display>', <cat_id>);
            // Resolver picks it up on the next bet, no code change.
            return null;
        }
        // Offline (in-house) games — static mapping.
        String cat = OfflineGameCategoryMap.categoryOf(gameKey);
        if (cat != null) return "offline_cat_" + cat;
        return null;
    }

    /**
     * SUN-1201 follow-up: provider-level fallback key. Only emitted for
     * GSC synthetic keys; for AWC and offline games the per-category
     * rate is the right fallback already (no per-platform tier).
     *
     * @return {@code "live_provider_<pc>"} for GSC keys, otherwise null
     */
    public static String providerKeyFor(String gameKey) {
        if (gameKey == null || !gameKey.startsWith("gsc_")) return null;
        int p1 = gameKey.indexOf('_', 4);
        if (p1 < 0) return null;
        String pcStr = gameKey.substring(4, p1);
        try {
            int pc = Integer.parseInt(pcStr);
            return "live_provider_" + pc;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

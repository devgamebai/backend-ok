package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve human-readable names for GSC live-game bets.
 *
 * <p>Three cache layers (hot → cold):
 * <ol>
 *   <li>JVM in-memory {@code gameNameCache} — instant hit on repeated bets.
 *   <li>{@code vinplay.gsc_game_catalog} MySQL table — persists across restarts.
 *   <li>GSC API {@code /api/operators/provider-games?product_code=…} — authoritative,
 *       hit on first bet of any unseen game. Response is upserted into the catalog
 *       table so layer 2 covers every subsequent bet.
 * </ol>
 *
 * <p>GSC fetch is rate-limited per product_code (one attempt per
 * {@link #GSC_REFETCH_COOLDOWN_MS} window) so a storm of bets on a missing game
 * doesn't hammer the upstream API. After cooldown expires and GSC still doesn't
 * list that (pc, gc) combination, bets fall back to {@code "<provider> - <game_code>"}.
 */
public final class GscGameNameResolver {

    private static final Logger logger = Logger.getLogger("GscGameNameResolver");
    private static final long GSC_REFETCH_COOLDOWN_MS = 3_600_000L; // 1 h per product_code

    private static final Map<Integer, String> providerCache = new ConcurrentHashMap<>();
    private static final Map<String, String> gameNameCache = new ConcurrentHashMap<>();
    // Tracks the last time we successfully pulled (or attempted) a given
    // product_code's catalog from GSC. Prevents repeat HTTP calls when a
    // product's catalog truly doesn't contain the bet's game_code.
    private static final Map<Integer, Long> lastGscFetchMs = new ConcurrentHashMap<>();

    // SUN-980: cache of (product_code|game_code) → commission_eligible flag.
    // Hot path (called per /seamless/withdraw webhook) — DB lookup cached
    // for a rolling 5-min window, matching GSCGameListProcessor's cache TTL.
    private static final Map<String, Boolean> commissionEligibleCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> commissionEligibleCacheAt = new ConcurrentHashMap<>();
    private static final long COMMISSION_CACHE_TTL_MS = 5L * 60L * 1000L;

    // SUN-1373: cache of (product_code|game_code) → game active flag.
    // Same TTL window as commission_eligible — both are read on every bet webhook.
    private static final Map<String, Boolean> gameActiveCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> gameActiveCacheAt = new ConcurrentHashMap<>();

    // SUN-1175: cache of (product_code|game_code) → category string. Same
    // hot-path window as commission_eligible since both are read by
    // WithdrawProcess on every seamless webhook. Empty string sentinel
    // distinguishes "looked up, not found" from "never looked up".
    private static final Map<String, String> categoryCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> categoryCacheAt = new ConcurrentHashMap<>();

    // SUN-1207/1210 — Dream Gaming (1052) sends empty game_code in the
    // seamless BET push, so per-bet table attribution depends on the
    // player's last c=3090 launch session. That session becomes stale
    // when (a) the player navigates to another table inside Dream's UI
    // without coming back to our FE for a re-launch, or (b) Dream's
    // backend auto-switches the player (dealer rotation, table
    // closure). Showing the launch-time table label as if it were the
    // actual at-bet table leads to QC reports like "Rồng Hổ hiển thị
    // khi user đang chơi Bac". We can't recover the truth from Dream
    // (their wager-detail PULL also returns empty game_code), so degrade
    // to category-level for display — truthful for in-category nav,
    // bounded-wrong for cross-category nav. Other providers (Evolution,
    // PG Soft, JILI, etc.) include the game_code in their push and stay
    // on the precise-table label.
    private static final int DREAM_GAMING_PRODUCT_CODE = 1052;
    private static final String DREAM_GAMING_LABEL = "Dream Gaming";

    private GscGameNameResolver() {}

    public static String providerName(int productCode) {
        String cached = providerCache.get(productCode);
        if (cached != null) return cached;
        String resolved = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT provider_name FROM vinplay.gsc_product_map WHERE product_code = ? LIMIT 1")) {
            ps.setInt(1, productCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) resolved = rs.getString("provider_name");
            }
        } catch (Exception e) {
            logger.warn("providerName lookup failed pc=" + productCode + " err=" + e.getMessage());
        }
        if (resolved == null || resolved.isEmpty()) resolved = "LIVE #" + productCode;
        providerCache.put(productCode, resolved);
        return resolved;
    }

    public static String displayName(int productCode, String gameCode) {
        if (gameCode == null) gameCode = "";
        String key = productCode + "|" + gameCode;
        String cached = gameNameCache.get(key);
        if (cached != null) return cached;

        // SUN-1207/1210 — see class comment block. For Dream Gaming
        // we degrade to category-level display because the per-bet
        // table attribution can't be trusted (vendor sends empty
        // game_code; SESSION-based fallback drifts on in-app nav).
        if (productCode == DREAM_GAMING_PRODUCT_CODE) {
            String dreamLabel;
            String category = null;
            if (gameCode != null && !gameCode.isEmpty()) {
                try { category = categoryOf(productCode, gameCode); }
                catch (Throwable ignored) { /* treat as no category */ }
            }
            if (category != null && !category.isEmpty()) {
                dreamLabel = DREAM_GAMING_LABEL + " - " + category;
            } else {
                dreamLabel = DREAM_GAMING_LABEL;
            }
            gameNameCache.put(key, dreamLabel);
            return dreamLabel;
        }

        String resolved = lookupCatalog(productCode, gameCode);

        // Layer 3: on-demand GSC fetch if catalog miss + we haven't tried
        // recently for this product_code. Fetches EVERY game under the
        // product, upserts all into the catalog, so other concurrent
        // bets for sibling games benefit too.
        if (resolved == null || resolved.isEmpty()) {
            if (shouldAttemptGscFetch(productCode)) {
                fetchAndUpsertProductCatalog(productCode);
                resolved = lookupCatalog(productCode, gameCode);
            }
        }

        if (resolved == null || resolved.isEmpty()) {
            resolved = providerName(productCode) + " - " + gameCode;
        }
        gameNameCache.put(key, resolved);
        return resolved;
    }

    private static String lookupCatalog(int productCode, String gameCode) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(game_name_vi, game_name) AS display_name FROM vinplay.gsc_game_catalog " +
                     "WHERE product_code = ? AND game_code = ? LIMIT 1")) {
            ps.setInt(1, productCode);
            ps.setString(2, gameCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("display_name");
            }
        } catch (Exception ignore) {
            // table may not exist yet — silent fallback
        }
        return null;
    }

    private static boolean shouldAttemptGscFetch(int productCode) {
        Long last = lastGscFetchMs.get(productCode);
        if (last == null) return true;
        return (System.currentTimeMillis() - last) > GSC_REFETCH_COOLDOWN_MS;
    }

    /**
     * Hit GSC {@code /api/operators/provider-games} for {@code productCode},
     * upsert every returned game into {@code gsc_game_catalog}. Non-fatal on
     * any failure — resolver falls back to the provider-level name.
     */
    static void fetchAndUpsertProductCatalog(int productCode) {
        lastGscFetchMs.put(productCode, System.currentTimeMillis());
        try {
            String operatorUrl = envOrDefault("GSC_OPERATOR_URL", "https://staging.gsimw.com");
            String operatorCode = envOrDefault("GSC_OPERATOR_CODE", "G7A1");
            String secretKey = envOrDefault("GSC_SECRET_KEY", "abYVbCrLT2VwpASotZGmCT");
            long requestTime = System.currentTimeMillis() / 1000;
            String sign = md5(requestTime + secretKey + "gamelist" + operatorCode);
            String url = operatorUrl + "/api/operators/provider-games"
                    + "?operator_code=" + operatorCode
                    + "&product_code=" + productCode
                    + "&sign=" + sign
                    + "&request_time=" + requestTime;

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            int status = conn.getResponseCode();
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) body.append(line);
            }
            JSONObject resp = new JSONObject(body.toString());
            if (resp.optInt("code", -1) != 0) {
                logger.warn("GSC catalog fetch pc=" + productCode + " returned code=" + resp.optInt("code") + " msg=" + resp.optString("message"));
                return;
            }
            JSONArray games = resp.optJSONArray("provider_games");
            if (games == null || games.length() == 0) return;

            String upsertSql = "INSERT INTO vinplay.gsc_game_catalog " +
                    "(product_code, game_code, game_name, game_type, category, image_url) VALUES " +
                    "(?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "game_name = VALUES(game_name), game_type = VALUES(game_type), " +
                    "category = VALUES(category), image_url = VALUES(image_url)";
            int inserted = 0;
            try (Connection dbConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = dbConn.prepareStatement(upsertSql)) {
                for (int i = 0; i < games.length(); i++) {
                    JSONObject g = games.getJSONObject(i);
                    if (!"ACTIVATED".equalsIgnoreCase(g.optString("status", "ACTIVATED"))) continue;
                    String gname = g.optString("game_name", "");
                    String gtype = g.optString("game_type", "");
                    String cat = g.optString("category", "");
                    // GSC upstream doesn't return `category` — infer from name/type
                    // so the rebate pipeline's live_cat_<X> lookup works.
                    if (cat.isEmpty()) cat = inferCategory(gname, gtype);
                    ps.setInt(1, productCode);
                    ps.setString(2, g.optString("game_code", ""));
                    ps.setString(3, gname);
                    ps.setString(4, gtype);
                    ps.setString(5, cat);
                    ps.setString(6, g.optString("image_url", ""));
                    ps.addBatch();
                    inserted++;
                }
                ps.executeBatch();
            }
            logger.info("GSC catalog on-demand fetch pc=" + productCode + " upserted=" + inserted);
        } catch (Throwable t) {
            logger.warn("GSC catalog fetch pc=" + productCode + " failed: " + t.getMessage());
        }
    }

    private static String envOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Heuristic category inference — in sync with api_backend
     * ListGSCGamesProcessor.inferCategory() + api_portal
     * GSCGameListProcessor.inferCategory() + scripts/seed-gsc-catalog.sh.
     * Keep all 4 sources aligned when keywords change.
     */
    private static String inferCategory(String name, String gameType) {
        String lc = (name == null ? "" : name).toLowerCase();
        if (lc.contains("baccarat") || lc.contains("baccarrat")) return "Baccarat";
        if (lc.contains("sic bo") || lc.contains("sicbo")) return "SicBoDice";
        if (lc.contains("dragon tiger") || lc.contains("dragontiger") || lc.contains("rồng hổ")) return "DragonTiger";
        if (lc.contains("roulette")) return "Roulette";
        if (lc.contains("blackjack")) return "Blackjack";
        if (lc.contains("poker") || lc.contains("hold'em") || lc.contains("holdem")) return "Poker";
        if (lc.contains("crazy time") || lc.contains("crazytime")) return "GameShow";
        if (lc.contains("mega ball") || lc.contains("megaball")) return "GameShow";
        if (lc.contains("monopoly")) return "GameShow";
        if (lc.contains("deal or no deal")) return "GameShow";
        if (lc.contains("dream catcher")) return "GameShow";
        if (lc.contains("bac bo")) return "GameShow";
        if (lc.contains("lightning")) return "GameShow";
        if (lc.contains("fantan") || lc.contains("fan tan")) return "GameShow";
        if (lc.contains("keno")) return "GameShow";
        if (lc.contains("teen patti")) return "GameShow";
        if (lc.contains("andar bahar")) return "GameShow";
        if (lc.contains("hi lo") || lc.contains("hilo")) return "GameShow";
        if (lc.contains("slot")) return "Slot";
        if ("SLOT".equals(gameType)) return "Slot";
        if ("FISHING".equals(gameType)) return "Fishing";
        if ("SPORT_BOOK".equals(gameType)) return "Sports";
        return "Other";
    }

    /** Manual cache invalidation hook — call when catalog is refreshed. */
    public static void clearCache() {
        providerCache.clear();
        gameNameCache.clear();
        lastGscFetchMs.clear();
        commissionEligibleCache.clear();
        commissionEligibleCacheAt.clear();
        gameActiveCache.clear();
        gameActiveCacheAt.clear();
    }

    /**
     * SUN-980 — look up commission_eligible on gsc_game_catalog.
     *
     * <p>Returns {@code true} if the (product_code, game_code) pair is marked
     * eligible for per-bet commission (default for all games). Returns
     * {@code false} only when DB says so — e.g. seamless-transfer fish
     * providers where amount = full balance, not a real bet.
     *
     * <p>On catalog miss OR any DB error, returns {@code true} (fail-open —
     * a missing catalog row is treated as a normal bet rather than silently
     * excluding the game from commission).
     *
     * <p>5-minute in-memory cache per (product, game) tuple. Call
     * {@link #clearCache()} to force a fresh DB hit.
     */
    public static boolean isCommissionEligible(int productCode, String gameCode) {
        Boolean v = lookupCatalogColumn(productCode, gameCode, "commission_eligible",
                commissionEligibleCache, commissionEligibleCacheAt, Boolean.TRUE,
                rs -> rs.getInt(1) == 1);
        return v != null && v;
    }

    /**
     * SUN-1373 — check whether a (product_code, game_code) pair is currently
     * active ({@code gsc_game_catalog.active = 1}).
     *
     * <p>Returns {@code true} (allowed) in all of these cases so the gate
     * only fires when the DB <em>explicitly</em> says disabled:
     * <ul>
     *   <li>No row in {@code gsc_game_catalog} — catalog not yet seeded,
     *       treat as active (fail-open).</li>
     *   <li>Any DB / pool error — fail-open so a transient outage never
     *       blocks all bets.</li>
     * </ul>
     *
     * <p>Returns {@code false} only when the DB returns {@code active=0}.
     *
     * <p>5-minute in-memory cache per (product, game) tuple — same TTL as
     * commission_eligible so admin c=9982 toggle takes effect within 5 min
     * without a restart. Call {@link #clearCache()} to force immediate effect.
     *
     * <p>Gating is only enforced when {@code GSC_BET_WHITELIST_ENFORCE=true}
     * is set in the environment — the caller is responsible for that check so
     * this method itself is always side-effect-free.
     */
    public static boolean isGameActive(int productCode, String gameCode) {
        Boolean v = lookupCatalogColumn(productCode, gameCode, "active",
                gameActiveCache, gameActiveCacheAt, Boolean.TRUE,
                rs -> rs.getInt(1) == 1);
        return v == null || v; // null → fail-open (treat as active)
    }

    /**
     * Catalog category for a (product, game) pair: "Fishing", "Baccarat",
     * "Roulette", "GameShow", etc. {@code null} when the catalog has no
     * row or the column is empty.
     */
    public static String categoryOf(int productCode, String gameCode) {
        String v = lookupCatalogColumn(productCode, gameCode, "category",
                categoryCache, categoryCacheAt, "",
                rs -> {
                    String c = rs.getString(1);
                    return c == null ? "" : c.trim();
                });
        return v == null || v.isEmpty() ? null : v;
    }

    /** Maps a single SELECT row to a typed value. */
    @FunctionalInterface
    private interface RowParser<T> {
        T parse(ResultSet rs) throws Exception;
    }

    /**
     * Cached lookup of one column from {@code gsc_game_catalog} keyed by
     * (product, game). Backs {@link #isCommissionEligible} and
     * {@link #categoryOf}; new column lookups should delegate here too
     * rather than duplicate the cache + SQL boilerplate.
     *
     * <p>Column name is treated as a Java-controlled identifier (never
     * user input), so it's spliced directly into the prepared statement.
     */
    private static <T> T lookupCatalogColumn(int productCode, String gameCode, String column,
                                             Map<String, T> cache, Map<String, Long> cacheAt,
                                             T defaultValue, RowParser<T> parser) {
        if (gameCode == null) gameCode = "";
        String key = productCode + "|" + gameCode;
        Long cachedAt = cacheAt.get(key);
        if (cachedAt != null
                && (System.currentTimeMillis() - cachedAt) < COMMISSION_CACHE_TTL_MS) {
            T cached = cache.get(key);
            if (cached != null) return cached;
        }

        T value = defaultValue;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + column + " FROM vinplay.gsc_game_catalog "
                             + "WHERE product_code = ? AND game_code = ? LIMIT 1")) {
            ps.setInt(1, productCode);
            ps.setString(2, gameCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) value = parser.parse(rs);
            }
        } catch (Exception e) {
            logger.warn("gsc_game_catalog column=" + column + " lookup failed pc=" + productCode
                    + " gc=" + gameCode + " err=" + e.getMessage());
        }
        cache.put(key, value);
        cacheAt.put(key, System.currentTimeMillis());
        return value;
    }
}

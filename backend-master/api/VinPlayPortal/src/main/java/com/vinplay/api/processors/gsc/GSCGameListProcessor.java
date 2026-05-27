package com.vinplay.api.processors.gsc;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

/**
 * c=3091 — Get list of available GSC+ games.
 * Player calls: /api?c=3091&at=TOKEN&product_code=1002
 * Returns: {"success":true,"data":{"games":[{game_code, game_name, game_type, image_url},...]}
 *
 * Config is env-driven (see GscEnv). The game list can be filtered via
 * GSC_WHITELIST_PRODUCTS and GSC_WHITELIST_GAMES:
 *   - GSC_WHITELIST_MODE=strict   → only whitelisted games are returned
 *   - GSC_WHITELIST_MODE=feature  → all activated games returned, whitelisted
 *                                   games get a "featured":true flag
 *   - both lists empty             → no filtering (returns all activated)
 */
public class GSCGameListProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = LoggerFactory.getLogger("portal");
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    // Cache key format: "<productCode>:<aggregator>[:<provider>]". All keys for
    // the same productCode share that prefix so admin toggles can invalidate
    // every aggregator-variant of a single product in one pass.
    private static final java.util.concurrent.ConcurrentHashMap<String, CacheEntry> GAME_LIST_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    // SUN-1xxx (2026-05-12): cross-JVM cache invalidation via a Hazelcast topic.
    // Without this, admin toggles to gsc_game_catalog took up to 5 minutes to
    // show in the player lobby (the CACHE_TTL_MS). With this, c=9982 publishes
    // the toggled product_code on this topic; every portal-api JVM listening
    // here clears its in-process entries for that product immediately.
    private static final String CACHE_INVALIDATE_TOPIC = "gsc_game_list_invalidate";
    private static volatile boolean CACHE_LISTENER_REGISTERED = false;
    private static final Object CACHE_LISTENER_LOCK = new Object();

    /**
     * Clear the in-process game-list cache. If {@code productCode > 0}, only
     * the entries for that product are removed; otherwise the whole cache is
     * flushed. Returns the number of entries removed for logging.
     */
    public static int clearCache(int productCode) {
        if (productCode <= 0) {
            int n = GAME_LIST_CACHE.size();
            GAME_LIST_CACHE.clear();
            return n;
        }
        String prefix = productCode + ":";
        String exact  = String.valueOf(productCode);
        int n = 0;
        java.util.Iterator<String> it = GAME_LIST_CACHE.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equals(exact) || k.startsWith(prefix)) { it.remove(); n++; }
        }
        return n;
    }

    /**
     * Idempotently subscribe to the cache-invalidate topic. Called from the
     * request path so it doesn't run at class-load time (when Hazelcast is
     * not guaranteed to be ready) but is set up before the first cache miss.
     */
    private static void ensureCacheInvalidationListener() {
        if (CACHE_LISTENER_REGISTERED) return;
        synchronized (CACHE_LISTENER_LOCK) {
            if (CACHE_LISTENER_REGISTERED) return;
            try {
                com.hazelcast.core.ITopic<Integer> topic =
                        com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance()
                                .getTopic(CACHE_INVALIDATE_TOPIC);
                topic.addMessageListener(msg -> {
                    Integer pc = msg.getMessageObject();
                    int product = pc == null ? 0 : pc.intValue();
                    int cleared = clearCache(product);
                    logger.info("GSC game-list cache invalidated by topic: product={} cleared_entries={}",
                            product, cleared);
                });
                CACHE_LISTENER_REGISTERED = true;
                logger.info("GSC game-list cache-invalidate listener registered on topic {}",
                        CACHE_INVALIDATE_TOPIC);
            } catch (Throwable t) {
                // Fire-and-keep-trying: next request will retry. Until then,
                // the 5-min TTL eventually expires the cache anyway.
                logger.warn("GSC game-list cache-invalidate listener registration failed (will retry): {}",
                        t.getMessage());
            }
        }
    }

    /**
     * SUN-1071/1072/1073 — provider → aggregator-specific id mapping.
     * Key is the uppercase game-maker name (JILI, CQ9, PP, PG, ...).
     * Value arrays: {gsc_product_code} for GSC side (first is primary); null → not on that aggregator.
     * AWC side: the platform string (same as game-maker name for most).
     */
    private static final java.util.HashMap<String, int[]> PROVIDER_TO_GSC_PRODUCT = new java.util.HashMap<>();
    private static final java.util.HashMap<String, String> PROVIDER_TO_AWC_PLATFORM = new java.util.HashMap<>();
    static {
        PROVIDER_TO_GSC_PRODUCT.put("EVOLUTION",   new int[]{1002});
        PROVIDER_TO_GSC_PRODUCT.put("PRAGMATIC",   new int[]{1006});
        PROVIDER_TO_GSC_PRODUCT.put("PP",          new int[]{1006});
        PROVIDER_TO_GSC_PRODUCT.put("PG",          new int[]{1007});
        PROVIDER_TO_GSC_PRODUCT.put("CQ9",         new int[]{1009});
        PROVIDER_TO_GSC_PRODUCT.put("SBO",         new int[]{1012});
        PROVIDER_TO_GSC_PRODUCT.put("SABA",        new int[]{1046});
        PROVIDER_TO_GSC_PRODUCT.put("DG",          new int[]{1052});
        PROVIDER_TO_GSC_PRODUCT.put("DREAM",       new int[]{1052});
        PROVIDER_TO_GSC_PRODUCT.put("JDB",         new int[]{1085});
        PROVIDER_TO_GSC_PRODUCT.put("JILI",        new int[]{1091});
        PROVIDER_TO_GSC_PRODUCT.put("HASH",        new int[]{1149});
        PROVIDER_TO_GSC_PRODUCT.put("SA",          new int[]{1185});
        PROVIDER_TO_GSC_PRODUCT.put("WM",          new int[]{1194});
        PROVIDER_TO_GSC_PRODUCT.put("MX",          new int[]{1291});

        PROVIDER_TO_AWC_PLATFORM.put("EVOLUTION",   "EVOLUTION");
        PROVIDER_TO_AWC_PLATFORM.put("SEXYBCRT",    "SEXYBCRT");
        PROVIDER_TO_AWC_PLATFORM.put("SEXY",        "SEXYBCRT");
        PROVIDER_TO_AWC_PLATFORM.put("HOTROAD",     "HOTROAD");
        PROVIDER_TO_AWC_PLATFORM.put("PRAGMATIC",   "PP");
        PROVIDER_TO_AWC_PLATFORM.put("PP",          "PP");
        PROVIDER_TO_AWC_PLATFORM.put("PG",          "PG");
        PROVIDER_TO_AWC_PLATFORM.put("JDB",         "JDB");
        PROVIDER_TO_AWC_PLATFORM.put("JDBFISH",     "JDBFISH");
        PROVIDER_TO_AWC_PLATFORM.put("FACHAI",      "FC");
        PROVIDER_TO_AWC_PLATFORM.put("FC",          "FC");
        PROVIDER_TO_AWC_PLATFORM.put("JILI",        "JILI");
        PROVIDER_TO_AWC_PLATFORM.put("NETENT",      "NETENT");
        PROVIDER_TO_AWC_PLATFORM.put("NLC",         "NLC");
        PROVIDER_TO_AWC_PLATFORM.put("BTG",         "BTG");
        PROVIDER_TO_AWC_PLATFORM.put("FASTSPIN",    "FASTSPIN");
        PROVIDER_TO_AWC_PLATFORM.put("RT",          "RT");
        PROVIDER_TO_AWC_PLATFORM.put("REDTIGER",    "RT");
        PROVIDER_TO_AWC_PLATFORM.put("DRAGOONSOFT", "DRAGOONSOFT");
        PROVIDER_TO_AWC_PLATFORM.put("PLAY8",       "PLAY8");
        PROVIDER_TO_AWC_PLATFORM.put("SPADE",       "SPADE");
        PROVIDER_TO_AWC_PLATFORM.put("PT",          "PT");
        PROVIDER_TO_AWC_PLATFORM.put("PLAYTECH",    "PT");
        PROVIDER_TO_AWC_PLATFORM.put("KINGMAKER",   "KINGMAKER");
        PROVIDER_TO_AWC_PLATFORM.put("KINGMAKERMINI","KINGMAKERMINI");
        PROVIDER_TO_AWC_PLATFORM.put("YESBINGO",    "YESBINGO");
        PROVIDER_TO_AWC_PLATFORM.put("LUDO",        "LUDO");
        PROVIDER_TO_AWC_PLATFORM.put("SABA",        "SABA");
        PROVIDER_TO_AWC_PLATFORM.put("E1SPORT",     "E1SPORT");
        PROVIDER_TO_AWC_PLATFORM.put("SV388",       "SV388");
        PROVIDER_TO_AWC_PLATFORM.put("HORSEBOOK",   "HORSEBOOK");
    }

    private static class CacheEntry {
        final String json;
        final long expiresAt;
        CacheEntry(String json, long expiresAt) { this.json = json; this.expiresAt = expiresAt; }
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        // Lazy-init the cross-JVM cache-invalidate listener on the first
        // request — Hazelcast is reliably ready by then, unlike class-load.
        ensureCacheInvalidationListener();
        try {
            HttpServletRequest request = param.get();

            // Auth
            String token = request.getParameter("at");
            if (token == null || token.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }
            com.hazelcast.core.IMap<String, String> tokenMap =
                    com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("cacheToken");
            String nickname = tokenMap.get(token);
            if (nickname == null || nickname.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }

            // Params
            String productCodeStr = request.getParameter("product_code");
            int productCode = 1002;
            if (productCodeStr != null && !productCodeStr.isEmpty()) {
                try { productCode = Integer.parseInt(productCodeStr); } catch (NumberFormatException e) { /* default */ }
            }

            // `aggregator` param (SUN-1071..1073):
            //   "gsc"        → GSC only            (legacy default)
            //   "awc"        → AWC only
            //   "all"        → both
            String aggregator = request.getParameter("aggregator");
            if (aggregator != null) aggregator = aggregator.toLowerCase();
            if (aggregator == null || aggregator.isEmpty()
                    || (!aggregator.equals("gsc") && !aggregator.equals("awc") && !aggregator.equals("all"))) {
                aggregator = "gsc";
            }
            boolean wantGsc = aggregator.equals("gsc") || aggregator.equals("all");
            boolean wantAwc = aggregator.equals("awc") || aggregator.equals("all");

            // `provider` param — filters by game-maker across both aggregators.
            // Value matches the uppercase key in PROVIDER_MAP (JILI, CQ9, PP, PG, ...).
            String provider = request.getParameter("provider");
            if (provider != null) provider = provider.trim().toUpperCase();
            Integer providerGscProduct = null;
            String providerAwcPlatform = null;
            if (provider != null && !provider.isEmpty()) {
                int[] gsc = PROVIDER_TO_GSC_PRODUCT.get(provider);
                if (gsc != null && gsc.length > 0) providerGscProduct = gsc[0];
                providerAwcPlatform = PROVIDER_TO_AWC_PLATFORM.get(provider);
                // When provider filter narrows GSC side, override productCode to match.
                if (providerGscProduct != null) productCode = providerGscProduct;
            }

            String cacheKey = productCode + ":" + aggregator
                    + (provider != null ? ":" + provider : "");

            // Check cache first — avoid hitting external GSC API on every request
            CacheEntry cached = GAME_LIST_CACHE.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.json;
            }

            // GitLab #35: provider-level gate. If admin disabled the whole
            // provider via gsc_product_map.is_active=0, short-circuit and
            // return an empty list — saves a 10s upstream GSC roundtrip and
            // matches the AWC path that already filters via m.is_active.
            // MAX(is_active) collapses the per-(product_code, game_type) rows:
            // any row with is_active=1 allows; only ALL rows=0 disables.
            // wasNull = product_code not yet seeded in gsc_product_map →
            // default-allow for backwards compat (only gate on explicit 0).
            if (wantGsc) {
                try (java.sql.Connection gateConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement gatePs = gateConn.prepareStatement(
                             "SELECT MAX(is_active) FROM gsc_product_map WHERE product_code = ?")) {
                    gatePs.setInt(1, productCode);
                    try (java.sql.ResultSet grs = gatePs.executeQuery()) {
                        if (grs.next()) {
                            int providerActive = grs.getInt(1);
                            boolean wasNull = grs.wasNull();
                            if (!wasNull && providerActive == 0) {
                                JSONObject gateData = new JSONObject();
                                gateData.put("games", new JSONArray());
                                gateData.put("categories", new JSONObject());
                                gateData.put("total", 0);
                                gateData.put("product_code", productCode);
                                gateData.put("provider_disabled", true);
                                response.put("success", true);
                                response.put("errorCode", "0");
                                response.put("data", gateData);
                                String gateResult = response.toString();
                                GAME_LIST_CACHE.put(cacheKey, new CacheEntry(gateResult, System.currentTimeMillis() + CACHE_TTL_MS));
                                logger.info("GSCGameListProcessor: provider " + productCode
                                        + " is disabled (gsc_product_map.is_active=0) — returning empty list");
                                return gateResult;
                            }
                        }
                    }
                } catch (Exception gateErr) {
                    // Best-effort — on DB error, fall through to upstream GSC call.
                    logger.warn("GitLab #35 provider gate failed for product_code=" + productCode
                            + ": " + gateErr.getMessage());
                }
            }

            // GSC config from env
            String operatorUrl = GscEnv.operatorUrl();
            String operatorCode = GscEnv.operatorCode();
            String secretKey = GscEnv.secretKey();
            long requestTime = System.currentTimeMillis() / 1000;

            // Sign: md5(request_time + secret_key + "gamelist" + operator_code)
            String sign = md5(requestTime + secretKey + "gamelist" + operatorCode);

            String urlStr = operatorUrl + "/api/operators/provider-games"
                    + "?operator_code=" + operatorCode
                    + "&product_code=" + productCode
                    + "&sign=" + sign
                    + "&request_time=" + requestTime;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            JSONObject gscResp = new JSONObject(sb.toString());
            int code = gscResp.optInt("code", -1);

            if (!wantGsc) {
                // aggregator=awc — skip GSC, go straight to AWC append below.
                // If provider set but not on AWC (e.g. CQ9), return empty list.
                JSONArray games = new JSONArray();
                JSONObject categories = new JSONObject();
                boolean skipAwcDueToProvider = (provider != null && !provider.isEmpty()
                                                && providerAwcPlatform == null);
                if (!skipAwcDueToProvider) {
                    appendAwcGames(games, categories, providerAwcPlatform);
                }
                // active=true rows first, then active=false. Within each group:
                // sort_order descending, then game_name ascending. _active_sortkey
                // is stripped after sort so the FE response has no `active` field.
                java.util.Comparator<JSONObject> gameOrderAwc = (a, b) -> {
                    int act = Boolean.compare(b.optBoolean("_active_sortkey", true), a.optBoolean("_active_sortkey", true));
                    if (act != 0) return act;
                    int cmp = Integer.compare(b.optInt("sort_order", 0), a.optInt("sort_order", 0));
                    return cmp != 0 ? cmp : a.optString("game_name").compareTo(b.optString("game_name"));
                };
                // SUN-1xxx (2026-05-11): apply the same ordering to category
                // buckets, not just the flat array. See the GSC branch below
                // for full rationale — FEs that render by category were
                // seeing per-bucket API-arrival order instead of active-first.
                java.util.Iterator<String> awcCatKeys = categories.keys();
                java.util.List<String> awcCatKeyList = new java.util.ArrayList<>();
                while (awcCatKeys.hasNext()) awcCatKeyList.add(awcCatKeys.next());
                for (String catKey : awcCatKeyList) {
                    JSONArray bucket = categories.optJSONArray(catKey);
                    if (bucket == null) continue;
                    java.util.List<JSONObject> bucketList = new java.util.ArrayList<>();
                    for (int i = 0; i < bucket.length(); i++) bucketList.add(bucket.getJSONObject(i));
                    bucketList.sort(gameOrderAwc);
                    JSONArray bucketSorted = new JSONArray();
                    for (JSONObject g : bucketList) bucketSorted.put(g);
                    categories.put(catKey, bucketSorted);
                }
                JSONArray sorted = new JSONArray();
                java.util.List<JSONObject> sortList = new java.util.ArrayList<>();
                for (int i = 0; i < games.length(); i++) sortList.add(games.getJSONObject(i));
                sortList.sort(gameOrderAwc);
                for (JSONObject g : sortList) { g.remove("_active_sortkey"); sorted.put(g); }
                JSONObject data = new JSONObject();
                data.put("games", sorted);
                data.put("categories", categories);
                data.put("total", sorted.length());
                data.put("aggregator", aggregator);
                if (provider != null) data.put("provider", provider);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
                String result = response.toString();
                GAME_LIST_CACHE.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS));
                return result;
            }

            if (code == 0) {
                Set<Integer> wlProducts = GscEnv.whitelistProducts();
                Set<String> wlGames = GscEnv.whitelistGames();
                boolean strict = "strict".equalsIgnoreCase(GscEnv.whitelistMode());
                boolean filtering = !wlProducts.isEmpty() || !wlGames.isEmpty();

                // 2026-04-25: load DB metadata BEFORE iterating upstream so the
                // category bucket key uses gsc_game_catalog.category (the
                // mapping table maintained by ops/QC) instead of the
                // hardcoded keyword-match inferCategory(). This lets QC move
                // BacBo into Baccarat or Ice Fishing into GameShow without a
                // Java code change. inferCategory remains the fallback for
                // upstream games that have no row in gsc_game_catalog yet.
                java.util.Map<String, int[]> dbFields = new java.util.HashMap<>();
                java.util.Map<String, String> viNames = new java.util.HashMap<>();
                java.util.Map<String, String> dbCategories = new java.util.HashMap<>();
                try (java.sql.Connection dbConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement dbPs = dbConn.prepareStatement(
                             // SUN-1xxx (2026-05-12): single source of truth for
                             // "game is admin-active in player lobby" =
                             // gsc_game_catalog.active. Dropped the join with
                             // vinplay.games.is_active that the legacy SQL ANDed
                             // in — that AND was causing silent admin drift:
                             // toggle via the new UI updated games.is_active but
                             // not gsc_game_catalog.active (pre-dual-write era),
                             // so admin thought a game was on but the lobby +
                             // bet-time gate both still rejected it. The bet-
                             // time gate (GscBetWhitelist.lookupActive) has
                             // always read only c.active. By aligning the
                             // lobby with that single field, the admin toggle
                             // (c=9982, which dual-writes to both tables) takes
                             // effect through a single read path.
                             //
                             // One-time repair UPDATE was run alongside this
                             // deploy to sync any historical split rows
                             // (c.active=0, g.is_active=1 → set c.active=1).
                             // games.is_active stays in the schema and gets
                             // dual-written by c=9982 — but no longer gates
                             // visibility. Migrating the rest of the system
                             // off vinplay.games entirely is Phase A of the
                             // unified-catalog plan; this is the read-side
                             // half.
                             "SELECT c.game_code, c.active, "
                                     + "c.sort_order, c.game_name_vi, c.category "
                                     + "FROM gsc_game_catalog c "
                                     + "WHERE c.product_code = ?")) {
                    dbPs.setInt(1, productCode);
                    try (java.sql.ResultSet rs = dbPs.executeQuery()) {
                        while (rs.next()) {
                            String gc = rs.getString("game_code");
                            dbFields.put(gc, new int[]{rs.getInt("active"), rs.getInt("sort_order")});
                            String vi = rs.getString("game_name_vi");
                            if (vi != null && !vi.isEmpty()) viNames.put(gc, vi);
                            String cat = rs.getString("category");
                            if (cat != null && !cat.isEmpty()) dbCategories.put(gc, cat);
                        }
                    }
                } catch (Exception dbErr) {
                    // DB overlay is best-effort — fall back to inferCategory
                    // when DB unavailable so the player lobby still renders.
                    logger.warn("SUN-1002: DB overlay pre-fetch failed: " + dbErr.getMessage());
                }

                JSONArray providerGames = gscResp.optJSONArray("provider_games");
                JSONArray games = new JSONArray();
                JSONObject categories = new JSONObject();
                // SUN-1xxx (2026-05-11): player lobby returns ONLY admin-active
                // games. Inactive games (catalog c.active=0 OR games.is_active=0
                // OR no catalog row at all) are excluded entirely from the
                // response — not just sorted to the bottom. Operator intent:
                // "for 3091 those inactive should not show on endpoint, which
                // means only show actives games."
                //
                // Admin/CMS that need the full upstream list use a different
                // endpoint (c=9982 family). c=3091 is player-facing only.
                if (providerGames != null) {
                    for (int i = 0; i < providerGames.length(); i++) {
                        JSONObject g = providerGames.getJSONObject(i);
                        if (!"ACTIVATED".equals(g.optString("status"))) continue;

                        boolean inWhitelist = !filtering
                                || wlProducts.contains(g.optInt("product_code"))
                                || wlGames.contains(g.optString("game_code"));

                        if (strict && filtering && !inWhitelist) continue;

                        String gc = g.optString("game_code");
                        // SUN-1xxx (2026-05-11): admin-active filter. Pull the
                        // catalog flag first; if not admin-active (catalog miss
                        // or active=0), skip — game does not appear in the
                        // player lobby. Done before we build the item/category
                        // bucket so inactive games consume no further work and
                        // don't leak into the categories map.
                        int[] activeFields = dbFields.get(gc);
                        boolean adminActive = activeFields != null && activeFields[0] == 1;
                        if (!adminActive) continue;
                        // DB-driven category if mapped; fall back to keyword
                        // inference for games not in gsc_game_catalog.
                        String dbCat = dbCategories.get(gc);
                        String category = (dbCat != null && !dbCat.isEmpty())
                                ? dbCat
                                : inferCategory(g.optString("game_name"));

                        JSONObject item = new JSONObject();
                        // provider field lets FE dispatch to the right launch
                        // endpoint (c=3090 for gsc, c=3095 for awc) from a
                        // single unified list — see AWC_FE_API_HANDOVER.md §2.
                        item.put("aggregator", "gsc");
                        item.put("game_code", gc);
                        item.put("game_name", g.optString("game_name"));
                        item.put("game_type", normalizeGameType(g.optString("game_type")));
                        item.put("image_url", g.optString("image_url"));
                        item.put("product_code", g.optInt("product_code"));
                        item.put("support_currency", g.optString("support_currency"));
                        item.put("category", category);
                        // Apply pre-fetched DB metadata in the same pass so a
                        // single rs.iter() suffices and the categories map is
                        // built using the final category value.
                        int[] fields = dbFields.get(gc);
                        // Keep _active as a transient sort key — server still
                        // sorts active games first — but it gets stripped before
                        // the response is returned (operator wants no `active`
                        // in the FE response; bet-time refusal is the only
                        // public gate).
                        //
                        // SUN-1xxx (2026-05-11): default to FALSE when the game
                        // is unknown to gsc_game_catalog. Admin is the source
                        // of truth for active visibility — a game that has
                        // never been touched by admin (no catalog row) is
                        // treated as inactive in the player lobby, same as
                        // a game admin explicitly toggled off. This keeps
                        // upstream additions from sneaking to the top of the
                        // player list before ops curates them. Operator
                        // intent: "admin api returns all games from provider,
                        // when admin marks active it adds to the db, and
                        // 3091 refers to those."
                        item.put("_active_sortkey", fields != null && fields[0] == 1);
                        item.put("sort_order", fields != null ? fields[1] : 0);
                        // SUN-1201 follow-up: always emit game_name_vi so FE
                        // can render localized labels uniformly. Falls back
                        // to the English game_name when no VN label is set
                        // in gsc_game_catalog. Without this fallback FE
                        // skips cards whose label map doesn't recognize
                        // the game_code (Dreaming AG: 4 of 11 tables had
                        // no FE-side label, so only 7 rendered).
                        String vi = viNames.get(gc);
                        item.put("game_name_vi",
                                (vi != null && !vi.isEmpty()) ? vi : g.optString("game_name"));
                        if (filtering) item.put("featured", inWhitelist);
                        games.put(item);

                        // Group by category for FE rendering. DB-mapped where
                        // available, keyword-inferred otherwise.
                        JSONArray bucket = categories.optJSONArray(category);
                        if (bucket == null) { bucket = new JSONArray(); categories.put(category, bucket); }
                        bucket.put(item);
                    }
                }

                // GitLab #40: server-side filter removed per product decision.
                // Response now includes every upstream ACTIVATED game with an
                // `active` flag overlaid from gsc_game_catalog.active.
                // FE is responsible for rendering only active=true to players;
                // admin/CMS sees the full list and can toggle per-game.
                // (show_disabled param is accepted for backwards compat but
                // no longer changes behavior.)

                // Optionally merge AWC catalog into the same flat list +
                // categories map. FE opts in via ?aggregator=all so existing
                // clients that only want GSC get identical output.
                // If provider set but not on AWC (e.g. CQ9), skip AWC entirely.
                // Passing null through would return ALL AWC rows (unfiltered).
                boolean skipAwcDueToProvider = (provider != null && !provider.isEmpty()
                                                && providerAwcPlatform == null);
                if (wantAwc && !skipAwcDueToProvider) {
                    appendAwcGames(games, categories, providerAwcPlatform);
                }

                // active first, then sort_order desc, then game_name asc.
                // _active_sortkey stripped after sort so it's hidden from the
                // FE response.
                java.util.Comparator<JSONObject> gameOrder = (a, b) -> {
                    int act = Boolean.compare(b.optBoolean("_active_sortkey", true), a.optBoolean("_active_sortkey", true));
                    if (act != 0) return act;
                    int cmp = Integer.compare(b.optInt("sort_order", 0), a.optInt("sort_order", 0));
                    return cmp != 0 ? cmp : a.optString("game_name").compareTo(b.optString("game_name"));
                };

                // SUN-1xxx (2026-05-11): sort the per-category buckets too.
                // The flat 'games' array gets sorted below but the 'categories'
                // map was populated in API-arrival (alphabetical) order during
                // the providerGames loop and would otherwise render unsorted
                // to FEs that group by category — active games would not
                // appear at the top of their category bucket. Done BEFORE the
                // flat-array strip so the same _active_sortkey is still
                // available on each item. Buckets hold the same item refs as
                // the flat array, so the strip below also strips them here.
                java.util.Iterator<String> catKeys = categories.keys();
                java.util.List<String> catKeyList = new java.util.ArrayList<>();
                while (catKeys.hasNext()) catKeyList.add(catKeys.next());
                for (String catKey : catKeyList) {
                    JSONArray bucket = categories.optJSONArray(catKey);
                    if (bucket == null) continue;
                    java.util.List<JSONObject> bucketList = new java.util.ArrayList<>();
                    for (int i = 0; i < bucket.length(); i++) bucketList.add(bucket.getJSONObject(i));
                    bucketList.sort(gameOrder);
                    JSONArray bucketSorted = new JSONArray();
                    for (JSONObject g : bucketList) bucketSorted.put(g);
                    categories.put(catKey, bucketSorted);
                }

                JSONArray sorted = new JSONArray();
                java.util.List<JSONObject> sortList = new java.util.ArrayList<>();
                for (int i = 0; i < games.length(); i++) sortList.add(games.getJSONObject(i));
                sortList.sort(gameOrder);
                for (JSONObject g : sortList) { g.remove("_active_sortkey"); sorted.put(g); }

                JSONObject data = new JSONObject();
                data.put("games", sorted);
                data.put("categories", categories);
                data.put("total", sorted.length());
                data.put("product_code", productCode);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);

                // Cache the successful response for 5 minutes
                String result = response.toString();
                GAME_LIST_CACHE.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS));
                return result;
            } else {
                return err(response, "5001", "GSC error: " + gscResp.optString("message", "Unknown"));
            }
        } catch (Exception e) {
            logger.error("GSCGameListProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    private static String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Cheap category inference from free-text {@code game_name}. Kept in sync
     * with {@code api_backend ListGSCGamesProcessor.inferCategory()} — if the
     * logic drifts, add the new keyword to BOTH classes so admin CMS and
     * player client agree on category labels.
     */
    private static String inferCategory(String n) {
        if (n == null) return "Other";
        String lc = n.toLowerCase();
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
        return "Other";
    }

    /**
     * Normalize GSC/AWC game_type taxonomies to a single vocabulary that FE
     * can filter by without per-aggregator branching.
     *   LIVE_CASINO → LIVE     (GSC)
     *   FISHING     → FISH     (GSC)
     *   FH          → FISH     (AWC)
     *   SPORT_BOOK  → SPORT    (GSC)
     *   ESPORTS     → SPORT    (AWC)
     *   LIVE, SLOT, EGAME      (already canonical)
     */
    /**
     * Map games.category_id (1..11) → legacy bucket name the FE renders.
     * Empty string when unknown — caller falls through to inferCategory.
     */
    private static String mapCategoryIdToName(int categoryId) {
        switch (categoryId) {
            case 1:  return "Baccarat";
            case 2:  return "DragonTiger";
            case 3:  return "Roulette";
            case 4:  return "Sicbo";
            case 5:  return "Blackjack";
            case 6:  return "GameShow";
            case 7:  return "Poker";
            case 8:  return "Slot";
            case 9:  return "Fish";
            case 10: return "Sport";
            case 11: return "Other";
            default: return "";
        }
    }

    /**
     * Map category_id → normalized game_type bucket the FE filter uses.
     * Live-casino style categories collapse to LIVE; everything else uses
     * its specific bucket.
     */
    private static String categoryIdToGameType(int categoryId) {
        switch (categoryId) {
            case 1: case 2: case 3: case 4: case 5: case 6: case 11:
                return "LIVE";
            case 7:  return "POKER";
            case 8:  return "SLOT";
            case 9:  return "FISH";
            case 10: return "SPORT";
            default: return "OTHER";
        }
    }

    private static String normalizeGameType(String t) {
        if (t == null) return "OTHER";
        switch (t.toUpperCase()) {
            case "LIVE_CASINO": return "LIVE";
            case "FISHING":
            case "FH":          return "FISH";
            case "SPORT_BOOK":
            case "ESPORTS":     return "SPORT";
            default:            return t.toUpperCase();
        }
    }

    /**
     * Append AWC catalog rows into the unified games array + categories map.
     * Mirrors the shape produced by the GSC branch (provider, game_code,
     * game_name, game_type, image_url, category, sort_order, active)
     * so FE can render both aggregators through one list and dispatch
     * launch via the `provider` field.
     *
     * Best-effort: on any DB error, logs a warning and leaves `games`
     * untouched — GSC rows still render.
     */
    private void appendAwcGames(JSONArray games, JSONObject categories, String platformFilter) {
        // SUN-GAME-FK Phase 4: read from the unified vinplay.games catalog
        // so admin toggles via c=9982 propagate. The legacy awc_game_catalog
        // / awc_platform_map remain as compatibility tables but are no longer
        // the source of truth.
        // Returns ALL AWC games (active + inactive). FE renders maintenance
        // badge from the `active` field; bet-time enforcement (UserGameBlock
        // + the seamless writer's catalog gate) refuses inactive bets.
        String sql = "SELECT vendor_platform AS platform, game_code, table_tag, "
                   + "       game_name, category_id, is_active "
                   + "  FROM vinplay.games "
                   + " WHERE provider = 'AWC' "
                   + "   AND game_code <> '*'";
        if (platformFilter != null && !platformFilter.isEmpty()) {
            sql += " AND vendor_platform COLLATE utf8mb4_unicode_ci = ?";
        }
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            if (platformFilter != null && !platformFilter.isEmpty()) ps.setString(1, platformFilter);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
            int appended = 0;
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("aggregator", "awc");
                item.put("platform", rs.getString("platform"));
                item.put("game_code", rs.getString("game_code"));
                String tag = rs.getString("table_tag");
                if (tag != null && !tag.isEmpty()) item.put("table_tag", tag);
                String name = rs.getString("game_name");
                String displayName = name != null ? name : rs.getString("game_code");
                item.put("game_name", displayName);
                // game_name_vi: catalog now stores the curated display name
                // directly in game_name (incl. table tag for SEXYBCRT — vd
                // "Sexy Baccarat M01"), so VN label = display name.
                item.put("game_name_vi", displayName);
                // category_id from games table (1=Baccarat, 2=DragonTiger,
                // 3=Roulette, 4=Sicbo, 5=Blackjack, 6=GameShows, 7=Poker,
                // 8=Slot, 9=Fish, 10=Sport, 11=Other). Map to legacy string
                // bucket so FE renderer that groups by `category` keeps working.
                int catId = rs.getInt("category_id");
                String cat = mapCategoryIdToName(catId);
                if (cat == null || cat.isEmpty()) cat = inferCategory(item.optString("game_name"));
                item.put("category", cat);
                item.put("category_id", catId);
                // game_type heuristic from category (legacy field; FE uses
                // it to filter LIVE / SLOT / etc).
                item.put("game_type", categoryIdToGameType(catId));
                // Same as GSC item path: keep `_active_sortkey` for sort,
                // strip before response. Field doesn't reach FE.
                item.put("_active_sortkey", rs.getInt("is_active") == 1);
                item.put("sort_order", 0);

                games.put(item);
                JSONArray bucket = categories.optJSONArray(cat);
                if (bucket == null) { bucket = new JSONArray(); categories.put(cat, bucket); }
                bucket.put(item);
                appended++;
            }
                if (appended > 0) logger.info("GSCGameListProcessor: appended " + appended + " AWC rows");
            }
        } catch (Exception e) {
            logger.warn("appendAwcGames failed (GSC rows still returned): " + e.getMessage());
        }
    }
}

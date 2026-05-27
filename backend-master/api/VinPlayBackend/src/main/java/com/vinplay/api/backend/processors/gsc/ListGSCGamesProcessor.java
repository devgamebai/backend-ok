package com.vinplay.api.backend.processors.gsc;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * c=9894 — SUN-886.
 *
 * List all games for a single provider (product_code) directly from the
 * GSC+ {@code /api/operators/provider-games} endpoint. Returns only
 * ACTIVATED games, grouped by inferred category (Baccarat, Blackjack,
 * Dragon Tiger, Roulette, Sicbo, Slot, Sport, Other).
 *
 * <p>Used by the admin CMS after the user picks a provider in the
 * parent dropdown (SUN-885). Each game here is a candidate for the
 * SUN-887 mapping step — admin picks which offline {@code game_key}
 * this GSC game should inherit commission rate from.
 *
 * <p>Params:
 * <ul>
 *   <li>{@code product_code} (required) — numeric GSC provider id (e.g. 1002 = Evolution)</li>
 * </ul>
 *
 * <p>Response shape:
 * <pre>
 * {
 *   "success": true,
 *   "errorCode": "0",
 *   "product_code": 1002,
 *   "total": 17,
 *   "categories": {
 *     "Baccarat":    [{game_code, game_name, game_type, image_url}, ...],
 *     "Blackjack":   [...],
 *     "DragonTiger": [...],
 *     "Roulette":    [...],
 *     "Other":       [...]
 *   },
 *   "data": [ // flat list for convenience
 *     { "game_code": "InsuranceBac0001", "game_name": "Insurance Baccarat",
 *       "game_type": "LIVE_CASINO", "image_url": "https://...", "category": "Baccarat" },
 *     ...
 *   ]
 * }
 * </pre>
 *
 * <p>GSC endpoint, operator code and secret key come from env vars via
 * {@code GscEnv} mirror on the portal side; duplicated here for backend
 * to avoid a cross-module dependency. Falls back to the staging G7A1
 * credentials if env is unset (same defaults portal uses).
 */
public class ListGSCGamesProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String typeParam = request.getParameter("type");
            String pcStr = request.getParameter("product_code");

            // Category-bucket mode: ?type=OFFLINE | LIVE → group games by
            // inferred category instead of listing one provider's games.
            // Used by the admin CMS commission-config page (SUN-category).
            if (typeParam != null && !typeParam.isEmpty()) {
                String t = typeParam.trim().toUpperCase();
                if ("OFFLINE".equals(t)) return buildOfflineResponse(response);
                if ("LIVE".equals(t)) return buildLiveResponse(response);
                return err(response, "4001", "type must be OFFLINE or LIVE");
            }

            if (pcStr == null || pcStr.isEmpty()) {
                return err(response, "4001", "product_code or type required");
            }
            int productCode;
            try { productCode = Integer.parseInt(pcStr); }
            catch (NumberFormatException e) { return err(response, "4001", "product_code must be a number"); }

            String operatorUrl = getenv("GSC_OPERATOR_URL", "https://staging.gsimw.com");
            String operatorCode = getenv("GSC_OPERATOR_CODE", "G7A1");
            String secretKey    = getenv("GSC_SECRET_KEY",    "abYVbCrLT2VwpASotZGmCT");
            long requestTime = System.currentTimeMillis() / 1000;
            String sign = md5(requestTime + secretKey + "gamelist" + operatorCode);

            String urlStr = operatorUrl + "/api/operators/provider-games"
                          + "?operator_code=" + operatorCode
                          + "&product_code=" + productCode
                          + "&sign=" + sign
                          + "&request_time=" + requestTime;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            JSONObject gscResp = new JSONObject(sb.toString());
            if (gscResp.optInt("code", -1) != 0) {
                return err(response, "5001", "GSC error: " + gscResp.optString("message", "Unknown"));
            }

            JSONArray providerGames = gscResp.optJSONArray("provider_games");
            if (providerGames == null) providerGames = new JSONArray();

            // 2026-04-25: prefer gsc_game_catalog.category mapping over the
            // hardcoded keyword-match inferCategory(). Lets ops/QC reorganize
            // categories via DB without code change. inferCategory remains
            // fallback for upstream rows not yet in our catalog.
            java.util.Map<String, String> dbCategories = new java.util.HashMap<>();
            try (java.sql.Connection dbConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 java.sql.PreparedStatement dbPs = dbConn.prepareStatement(
                         "SELECT game_code, category FROM gsc_game_catalog WHERE product_code = ?")) {
                dbPs.setInt(1, productCode);
                try (java.sql.ResultSet rs = dbPs.executeQuery()) {
                    while (rs.next()) {
                        String c = rs.getString("category");
                        if (c != null && !c.isEmpty()) {
                            dbCategories.put(rs.getString("game_code"), c);
                        }
                    }
                }
            } catch (Exception dbErr) {
                logger.warn("ListGSCGamesProcessor: DB category prefetch failed: " + dbErr.getMessage());
            }

            JSONArray flat = new JSONArray();
            JSONObject grouped = new JSONObject();

            for (int i = 0; i < providerGames.length(); i++) {
                JSONObject g = providerGames.getJSONObject(i);
                String gameStatus = g.optString("status", "");
                if (!"ACTIVATED".equals(gameStatus)) continue;

                String name = g.optString("game_name", "");
                String code = g.optString("game_code", "");
                String type = g.optString("game_type", "");
                String img  = g.optString("image_url", "");
                String dbCat = dbCategories.get(code);
                String category = (dbCat != null && !dbCat.isEmpty())
                        ? dbCat
                        : inferCategory(name);

                JSONObject item = new JSONObject();
                item.put("game_code", code);
                item.put("game_name", name);
                item.put("game_type", type);
                item.put("image_url", img);
                item.put("category", category);
                flat.put(item);

                JSONArray bucket = grouped.optJSONArray(category);
                if (bucket == null) { bucket = new JSONArray(); grouped.put(category, bucket); }
                bucket.put(item);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("product_code", productCode);
            response.put("total", flat.length());
            response.put("categories", grouped);
            response.put("data", flat);
        } catch (Exception e) {
            logger.error("ListGSCGamesProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Build {@code type=OFFLINE} response — 8 hardcoded offline categories
     * from {@link com.vinplay.dal.service.OfflineGameCategoryMap}. Each
     * category's {@code games[]} has the internal game_key that
     * {@code SetGameCommissionRateProcessor} already validates.
     */
    private String buildOfflineResponse(JSONObject response) {
        JSONArray flat = new JSONArray();
        JSONObject grouped = new JSONObject();
        for (java.util.Map.Entry<String, String[]> e
                : com.vinplay.dal.service.OfflineGameCategoryMap.allCategories().entrySet()) {
            String cat = e.getKey();
            JSONArray bucket = new JSONArray();
            for (String gkey : e.getValue()) {
                JSONObject g = new JSONObject();
                g.put("game_code", gkey);
                g.put("game_key", gkey);
                g.put("game_name", gkey);
                g.put("game_type", "OFFLINE");
                g.put("category", cat);
                flat.put(g);
                bucket.put(g);
            }
            grouped.put(cat, bucket);
        }
        response.put("success", true);
        response.put("errorCode", "0");
        response.put("type", "OFFLINE");
        response.put("total", flat.length());
        response.put("categories", grouped);
        response.put("data", flat);
        return response.toString();
    }

    /**
     * Build {@code type=LIVE} response — aggregate every row in
     * {@code vinplay.gsc_game_catalog} across ALL GSC providers,
     * grouped by {@code category}. Catalog is populated by the seed
     * script and by on-demand fetches in {@code GscGameNameResolver}.
     */
    private String buildLiveResponse(JSONObject response) {
        JSONArray flat = new JSONArray();
        JSONObject grouped = new JSONObject();
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                .getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT product_code, game_code, game_name, game_type, category, image_url "
                   + "FROM vinplay.gsc_game_catalog "
                   + "ORDER BY category, product_code, game_code")) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("game_name");
                    String cat = rs.getString("category");
                    if (cat == null || cat.isEmpty()) cat = inferCategory(name);
                    int pc = rs.getInt("product_code");
                    String code = rs.getString("game_code");

                    JSONObject g = new JSONObject();
                    g.put("product_code", pc);
                    g.put("game_code", code);
                    g.put("game_key", "gsc_" + pc + "_" + code);
                    g.put("game_name", name);
                    g.put("game_type", rs.getString("game_type") != null ? rs.getString("game_type") : "");
                    g.put("image_url", rs.getString("image_url") != null ? rs.getString("image_url") : "");
                    g.put("category", cat);
                    flat.put(g);

                    JSONArray bucket = grouped.optJSONArray(cat);
                    if (bucket == null) { bucket = new JSONArray(); grouped.put(cat, bucket); }
                    bucket.put(g);
                }
            }
        } catch (Exception e) {
            logger.error("buildLiveResponse error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        response.put("success", true);
        response.put("errorCode", "0");
        response.put("type", "LIVE");
        response.put("total", flat.length());
        response.put("categories", grouped);
        response.put("data", flat);
        return response.toString();
    }

    /** Cheap category inference from the free-text game_name. Keeps this */
    /* processor dependency-free; SUN-887 will store an authoritative */
    /* category → game_key mapping per row in the DB. */
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

    private static String getenv(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}

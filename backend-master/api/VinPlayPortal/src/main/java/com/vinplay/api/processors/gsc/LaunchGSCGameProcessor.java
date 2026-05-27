package com.vinplay.api.processors.gsc;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * c=3090 — Launch a GSC+ game (table, slot, or sport-book entry).
 *
 * Returns: {"success":true,"data":{"url":..., "url_android":..., "url_ios":..., "expires_at":..., "ttl_seconds":3600}}
 *
 * <p>Dual-URL response (SUN-XXXX): mobile clients pick {@code url_ios} on
 * iOS and {@code url_android} elsewhere. For the configured products
 * (default: PG Soft 1007), {@code url_ios} uses the masked iOS domain
 * (default: pgsoft-sunkr.live) instead of the upstream provider host.
 * {@code url} is kept for backward compatibility — equal to
 * {@code url_android}.
 *
 * <p>2026-04-28: launch-URL caching is disabled. Every c=3090 call
 * issues a fresh GSC session — required so each "come and play" gets
 * a clean state on the GSC side. The earlier 1-hour Hazelcast cache
 * (IMap {@code gsc_launch_urls}, keyed by nickname/product/game/
 * platform) reused launch URLs across re-opens, which kept players
 * on stale GSC sessions the provider may have already invalidated.
 * The inline-HTML token cache for PG Soft (c=3092, IMap
 * {@code launch_html_tokens}) is a separate mechanism and stays.
 *
 * <p>All GSC endpoints, credentials, lobby URL and currency come from
 * env vars (GSC_*). iOS rewrite is configured via:
 * <ul>
 *   <li>{@code IOS_REWRITE_PRODUCTS} — comma-separated product codes
 *       (default {@code 1007})</li>
 *   <li>{@code PGSOFT_IOS_HOST} — replacement host for product 1007
 *       (default {@code pgsoft-sunkr.live})</li>
 *   <li>{@code LAUNCH_URL_TTL_SECONDS} — cache TTL (default {@code 3600})</li>
 *   <li>{@code LAUNCH_DUAL_URL} — set {@code false} to disable the
 *       new behavior entirely and revert to single-URL response</li>
 * </ul>
 */
public class LaunchGSCGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = LoggerFactory.getLogger("portal");

    /** Inline HTML token store — see {@code LaunchHtmlTokenProcessor} (c=3092). */
    static final String HTML_TOKEN_MAP_NAME = "launch_html_tokens";

    private static final boolean DUAL_URL_ENABLED;
    private static final int LAUNCH_TTL_SECONDS;
    private static final java.util.Set<Integer> IOS_REWRITE_PRODUCTS;
    private static final String PGSOFT_IOS_HOST;

    static {
        String dualFlag = System.getenv("LAUNCH_DUAL_URL");
        DUAL_URL_ENABLED = dualFlag == null || !"false".equalsIgnoreCase(dualFlag.trim());

        int ttl = 3600;
        try {
            String t = System.getenv("LAUNCH_URL_TTL_SECONDS");
            if (t != null && !t.isEmpty()) ttl = Integer.parseInt(t.trim());
        } catch (NumberFormatException ignored) {}
        LAUNCH_TTL_SECONDS = Math.max(60, ttl);

        java.util.Set<Integer> products = new java.util.HashSet<>();
        String csv = System.getenv("IOS_REWRITE_PRODUCTS");
        if (csv == null || csv.isEmpty()) csv = "1007";
        for (String s : csv.split(",")) {
            try { products.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        IOS_REWRITE_PRODUCTS = java.util.Collections.unmodifiableSet(products);

        String h = System.getenv("PGSOFT_IOS_HOST");
        PGSOFT_IOS_HOST = (h == null || h.isEmpty()) ? "pgsoft-sunkr.live" : h.trim();
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // Auth: get player nickname from token
            String token = request.getParameter("at");
            if (token == null || token.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }
            com.hazelcast.core.IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String nickname = tokenMap.get(token);
            if (nickname == null || nickname.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }

            // Params
            String gameCode = request.getParameter("game_code");
            String productCodeStr = request.getParameter("product_code");
            String platform = request.getParameter("platform");
            String gameType = request.getParameter("game_type");

            int productCode = 1002; // default
            if (productCodeStr != null && !productCodeStr.isEmpty()) {
                try { productCode = Integer.parseInt(productCodeStr); } catch (NumberFormatException e) { /* use default */ }
            }
            if (platform == null || platform.isEmpty()) platform = "WEB";
            if (gameType == null || gameType.isEmpty()) gameType = "LIVE_CASINO";
            // c=3091 normalizes LIVE_CASINO→LIVE etc. for FE; launch-game must
            // send GSC's original taxonomy. De-normalize anything the FE hands
            // back so `game_type` always reaches GSC in its expected form.
            gameType = denormalizeGameType(gameType);

            // Sport-book tiles (SBO=1012, Saba=1046, etc.) have a single entry point
            // per provider — clicking the tile means "open the sports app". FE doesn't
            // know (and shouldn't need to know) a specific game_code for these.
            // Default to "1" which is the main sport-book entry on all enabled products.
            if (gameCode == null || gameCode.isEmpty()) {
                gameCode = "1";
            }

            // No launch-URL cache lookup — see class javadoc (every
            // c=3090 must mint a fresh GSC session).

            // GSC config from env — operator account determines the currency,
            // so always use the configured GSC_CURRENCY and ignore any value
            // the client sends (old client builds still send IDR2 by default).
            String operatorUrl = GscEnv.operatorUrl();
            String operatorCode = GscEnv.operatorCode();
            String secretKey = GscEnv.secretKey();
            String lobbyUrl = GscEnv.lobbyUrl();
            // Per-product override wins (e.g. PG Soft 1007 needs KRW2, not KRW).
            String currency = GscEnv.currencyFor(productCode);
            long requestTime = System.currentTimeMillis() / 1000;

            // Sign: md5(request_time + secret_key + "launchgame" + operator_code)
            String sign = md5(requestTime + secretKey + "launchgame" + operatorCode);

            // Get player IP
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();

            JSONObject body = new JSONObject();
            body.put("operator_code", operatorCode);
            body.put("member_account", nickname);
            body.put("password", "e10adc3949ba59abbe56e057f20f883e"); // dummy, GSC doesn't validate
            body.put("nickname", nickname);
            body.put("currency", currency);
            body.put("game_code", gameCode);
            body.put("product_code", productCode);
            body.put("game_type", gameType);
            // GSC language_code per provider doc: 0=English, 1=Traditional Chinese,
            // 2=Simplified Chinese, 3=Thai, 4=Indonesian, 5=Japanese, 6=Korean,
            // 7=Vietnamese, 8=Deutsch. Default to Vietnamese for the VN audience.
            body.put("language_code", 7);
            body.put("ip", ip);
            body.put("platform", platform);
            body.put("sign", sign);
            body.put("request_time", requestTime);
            body.put("operator_lobby_url", lobbyUrl);

            // Call GSC
            URL url = new URL(operatorUrl + "/api/operators/launch-game");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int status = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            JSONObject gscResp = new JSONObject(sb.toString());
            int code = gscResp.optInt("code", -1);

            if (code == 200 || code == 0) {
                // Most providers return a `url` the client navigates to.
                // Some (PG Soft, etc.) return inline HTML in `content` instead.
                // Forward both — client uses whichever is populated.
                JSONObject data = new JSONObject();
                String gameUrl = gscResp.optString("url", "");
                String gameContent = gscResp.optString("content", "");

                // SUN-XXXX dual-URL: derive iOS-masked URL for configured
                // products (PG Soft etc.) so iOS clients can avoid the
                // upstream provider domain. Android keeps the original.
                String urlAndroid = gameUrl;
                String urlIos = rewriteHostForIos(gameUrl, productCode);

                // Inline HTML providers (PG Soft) — GSC returns content
                // instead of a URL. Mobile clients open the launch in a
                // new browser tab, which can't render inline HTML, so we
                // host the HTML ourselves under a one-shot token URL on
                // pgsoft-sunkr.live and return that URL to mobile. Token
                // lives in Hazelcast for LAUNCH_TTL_SECONDS; c=3092
                // serves the HTML by token.
                if (DUAL_URL_ENABLED && (gameUrl == null || gameUrl.isEmpty())
                        && gameContent != null && !gameContent.isEmpty()
                        && IOS_REWRITE_PRODUCTS.contains(productCode)) {
                    try {
                        String htmlToken = java.util.UUID.randomUUID().toString().replace("-", "");
                        com.hazelcast.core.IMap<String, String> htmlMap =
                                HazelcastClientFactory.getInstance().getMap(HTML_TOKEN_MAP_NAME);
                        htmlMap.put(htmlToken, gameContent, LAUNCH_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                        String hostedUrl = "https://" + PGSOFT_IOS_HOST + "/p/" + htmlToken;
                        urlAndroid = hostedUrl; // mobile (both platforms) needs a URL
                        urlIos = hostedUrl;
                        gameUrl = hostedUrl;    // back-compat field
                    } catch (Throwable hostErr) {
                        logger.warn("LaunchGSCGameProcessor inline-HTML hosting failed (non-fatal): {}", hostErr.getMessage());
                    }
                }

                data.put("url", urlAndroid); // back-compat — matches old single-URL field
                if (DUAL_URL_ENABLED) {
                    data.put("url_android", urlAndroid);
                    data.put("url_ios", urlIos);
                    long expiresAt = (System.currentTimeMillis() / 1000L) + LAUNCH_TTL_SECONDS;
                    data.put("expires_at", expiresAt);
                    data.put("ttl_seconds", LAUNCH_TTL_SECONDS);
                }
                if (!gameContent.isEmpty()) data.put("content", gameContent);
                data.put("game_code", gameCode);
                data.put("product_code", productCode);

                // SUN-1201 Phase 1: tag the player's current live-table so
                // wager-push handlers (WithdrawProcess / DepositProcess) can
                // attribute bets to a specific game_code when the vendor's
                // payload omits it (Dream Gaming sends game_code="").
                // Hazelcast-backed, cluster-shared, 2h TTL.
                com.vinplay.dal.service.PlayerGameSessionService.record(
                        nickname, productCode, gameCode);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            } else {
                return err(response, "5001", "GSC error: " + gscResp.optString("message", "Unknown"));
            }
        } catch (Exception e) {
            logger.error("LaunchGSCGameProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /**
     * Reverse of GSCGameListProcessor.normalizeGameType. c=3091 emits the
     * short form for FE convenience (LIVE, FISH, SPORT); GSC's launch-game
     * endpoint only accepts the full GSC vocabulary. Accept both so callers
     * that already send the canonical form keep working.
     */
    private static String denormalizeGameType(String t) {
        if (t == null) return "LIVE_CASINO";
        switch (t.toUpperCase()) {
            case "LIVE":       return "LIVE_CASINO";
            case "FISH":       return "FISHING";
            case "SPORT":      return "SPORT_BOOK";
            default:           return t.toUpperCase();
        }
    }

    /**
     * Rewrite the URL host for iOS clients on the configured products.
     * Returns the original URL when no rewrite applies — that way the
     * iOS field is always populated and clients can use it
     * unconditionally without an extra null-check.
     */
    private static String rewriteHostForIos(String url, int productCode) {
        if (url == null || url.isEmpty()) return url;
        if (!IOS_REWRITE_PRODUCTS.contains(productCode)) return url;
        // Replace scheme://host (no path) with https://<masked-host>.
        // Keeping original scheme ensures we don't downgrade https→http.
        return url.replaceFirst("^https?://[^/]+", "https://" + PGSOFT_IOS_HOST);
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
}

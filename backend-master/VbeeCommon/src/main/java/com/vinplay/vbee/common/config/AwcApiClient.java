package com.vinplay.vbee.common.config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Outbound HTTP client for AWC (Asia Win Connect) API.
 *
 * All calls are POST with JSON body containing agentId + cert.
 * No HMAC — plain cert in body. Responses: status "0000" = success.
 */
public final class AwcApiClient {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("awc");
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;

    private AwcApiClient() {}

    /**
     * Default per-platform betLimit for LIVE games. AWC recommended
     * these values via direct support instruction (2026-04-22). Must
     * be supplied on createMember for SEXYBCRT / HOTROAD / PP LIVE
     * tables to avoid the downstream 1026 "Bet setting is not
     * properly set up" error when the player enters a table.
     *
     * Extend this string when a new LIVE platform is enabled on
     * agent lime01 (or production equivalent).
     */
    private static final String DEFAULT_BET_LIMIT_JSON =
            // STAGING-ONLY DEFAULTS. These limitIds were provisioned for AWC
            // staging agent `lime01` (cert PigVq2D07hNL); production agent
            // `sunkr` rejects them — createMember returns 400 Bad Request and
            // updateBetLimit returns 1026 "has not correct betLimit of KRW".
            // Production deployments MUST override via AWC_BET_LIMIT_JSON env
            // var with prod-agent PTV-bound IDs (request from AWC if unsure).
            "{\"SEXYBCRT\":{\"LIVE\":{\"limitId\":[281401,281402,281403,281404,281405]}},"
          + "\"HOTROAD\":{\"LIVE\":{\"limitId\":[1100006]}},"
          + "\"PP\":{\"LIVE\":{\"limitId\":[\"G1\"]}}}";

    /**
     * Resolve betLimit JSON sent on createMember. Reads env override
     * {@code AWC_BET_LIMIT_JSON} first; set the env to the literal string
     * {@code "off"} (or leave empty after un-baking the default) to skip
     * the field entirely. Useful while AWC support is still whitelisting
     * the limitId values on the agent side — under that condition any
     * createMember/login call carrying betLimit returns "Invalid bet
     * limit setting" and the player cannot enter SEXYBCRT.
     */
    private static String resolveBetLimitJson() {
        String env = System.getenv("AWC_BET_LIMIT_JSON");
        if (env != null) {
            String trimmed = env.trim();
            if (trimmed.isEmpty() || "off".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                return null;
            }
            return trimmed;
        }
        return DEFAULT_BET_LIMIT_JSON;
    }

    /**
     * POST /wallet/createMember
     * Creates a player on AWC. Idempotent — duplicate userId returns 0000.
     *
     * AWC requires form-encoded (application/x-www-form-urlencoded) for
     * this endpoint and wants betLimit supplied at creation so new
     * accounts don't hit 1026 on SEXYBCRT/HOTROAD/PP LIVE entry.
     * Existing accounts (AWC's idempotent "Account existed" response)
     * keep whatever betLimit they already had — those must be reset
     * via /wallet/updateBetLimit or provider-side admin.
     */
    public static JSONObject createMember(String username, String displayName) {
        java.util.LinkedHashMap<String, String> form = new java.util.LinkedHashMap<>();
        form.put("cert", AwcConfig.cert());
        form.put("agentId", AwcConfig.agentId());
        form.put("userId", AwcConfig.toAwcUserId(username));
        form.put("currency", AwcConfig.currency());
        form.put("language", AwcConfig.language());
        if (displayName != null && !displayName.isEmpty()) {
            String clean = displayName.replaceAll("[^0-9a-zA-Z]", "");
            if (clean.length() > 20) clean = clean.substring(0, 20);
            if (!clean.isEmpty()) form.put("userName", clean);
        }
        String bl = resolveBetLimitJson();
        if (bl != null && !bl.isEmpty()) form.put("betLimit", bl);
        return postForm("/wallet/createMember", form);
    }

    /**
     * POST /wallet/login — opens provider lobby.
     * Returns single-use URL (expires 10 min).
     */
    public static JSONObject login(String username, String platform, String gameType,
                                   boolean isMobile, String betLimit, String externalURL) {
        JSONObject body = baseBody();
        body.put("userId", AwcConfig.toAwcUserId(username));
        body.put("platform", platform);
        body.put("gameType", gameType != null ? gameType : "LIVE");
        body.put("isMobileLogin", isMobile);
        body.put("language", AwcConfig.language());
        if (betLimit != null && !betLimit.isEmpty()) body.put("betLimit", betLimit);
        if (externalURL != null && !externalURL.isEmpty()) body.put("externalURL", externalURL);
        return post("/wallet/login", body);
    }

    /**
     * POST /wallet/doLoginAndLaunchGame — opens specific game directly.
     * Returns single-use URL (expires 10 min).
     * Legacy 6-arg form. Delegates to the full overload below.
     */
    public static JSONObject doLoginAndLaunchGame(String username, String platform,
                                                   String gameCode, String gameType,
                                                   boolean isMobile, String betLimit) {
        return doLoginAndLaunchGame(username, platform, gameCode, gameType,
                isMobile, betLimit, null, false, null);
    }

    /**
     * POST /wallet/doLoginAndLaunchGame — full overload with direct-to-table params.
     *
     * @param gameTableId    SEXYBCRT table id; pass null to land on lobby
     * @param launchGameTable true to enter the specified table (requires gameTableId)
     * @param hall           SEXYBCRT-only alternative to tableId (e.g. "SEXY")
     */
    public static JSONObject doLoginAndLaunchGame(String username, String platform,
                                                   String gameCode, String gameType,
                                                   boolean isMobile, String betLimit,
                                                   String gameTableId, boolean launchGameTable,
                                                   String hall) {
        JSONObject body = baseBody();
        body.put("userId", AwcConfig.toAwcUserId(username));
        body.put("platform", platform);
        body.put("gameCode", gameCode);
        body.put("gameType", gameType != null ? gameType : "SLOT");
        body.put("isMobileLogin", isMobile);
        body.put("language", AwcConfig.language());
        if (betLimit != null && !betLimit.isEmpty()) body.put("betLimit", betLimit);
        if (gameTableId != null && !gameTableId.isEmpty()) body.put("gameTableId", gameTableId);
        if (launchGameTable) body.put("isLaunchGameTable", true);
        if (hall != null && !hall.isEmpty()) body.put("hall", hall);
        return post("/wallet/doLoginAndLaunchGame", body);
    }

    /**
     * POST /wallet/logout — force-logout players by userId list.
     */
    public static JSONObject logout(List<String> usernames) {
        JSONObject body = baseBody();
        JSONArray ids = new JSONArray();
        for (String u : usernames) ids.put(AwcConfig.toAwcUserId(u));
        body.put("userIds", ids.toString());
        return post("/wallet/logout", body);
    }

    /**
     * POST /fetch/getPlatformListByAgent — list all available platforms.
     */
    public static JSONObject getPlatformListByAgent() {
        return post("/fetch/getPlatformListByAgent", baseBody());
    }


    /**
     * POST /wallet/getTransactionStatus — query txn status (7-day window).
     */
    public static JSONObject getTransactionStatus(String platformTxId) {
        JSONObject body = baseBody();
        body.put("platformTxId", platformTxId);
        return post("/wallet/getTransactionStatus", body);
    }

    /**
     * POST /wallet/checkStatus — check if platform/gameCode is available.
     */
    public static JSONObject checkStatus(String platform) {
        JSONObject body = baseBody();
        body.put("platform", platform);
        return post("/wallet/checkStatus", body);
    }

    private static JSONObject baseBody() {
        JSONObject body = new JSONObject();
        body.put("agentId", AwcConfig.agentId());
        body.put("cert", AwcConfig.cert());
        return body;
    }

    private static JSONObject post(String path, JSONObject body) {
        String url = AwcConfig.apiUrl() + path;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "SunKR-AWC/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            String reqJson = body.toString();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(reqJson.getBytes("UTF-8"));
            }

            int httpStatus = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            java.io.InputStream stream = httpStatus >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }

            String raw = sb.toString().trim();
            logger.info("AWC " + path + " HTTP " + httpStatus
                    + " req=" + (reqJson.length() > 300 ? reqJson.substring(0, 300) : reqJson)
                    + " raw=" + (raw.length() > 200 ? raw.substring(0, 200) : raw));

            if (raw.isEmpty() || !raw.startsWith("{")) {
                JSONObject err = new JSONObject();
                err.put("status", "9999");
                err.put("desc", "Non-JSON response (HTTP " + httpStatus + "): " + (raw.length() > 100 ? raw.substring(0, 100) : raw));
                return err;
            }

            JSONObject resp = new JSONObject(raw);
            // AWC error responses use "statusCode" instead of "status"
            if (!resp.has("status") && resp.has("statusCode")) {
                resp.put("status", String.valueOf(resp.get("statusCode")));
            }
            return resp;
        } catch (Exception e) {
            logger.error("AWC POST " + path + " failed", e);
            JSONObject err = new JSONObject();
            err.put("status", "9999");
            err.put("desc", "HTTP error: " + e.getMessage());
            return err;
        }
    }

    /**
     * Form-encoded POST — mirrors AWC's documented curl format:
     *   curl --data-urlencode 'cert=...' --data-urlencode 'betLimit={...}'
     * AWC requires this for /wallet/createMember when betLimit is attached.
     * Response parsing matches post(): JSON body, statusCode → status
     * fallback, non-JSON wrapped in 9999 error.
     */
    private static JSONObject postForm(String path, java.util.Map<String, String> form) {
        String url = AwcConfig.apiUrl() + path;
        try {
            StringBuilder body = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : form.entrySet()) {
                if (body.length() > 0) body.append('&');
                body.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"));
                body.append('=');
                body.append(java.net.URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
            }
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "SunKR-AWC/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }
            int httpStatus = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            java.io.InputStream stream = httpStatus >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            String raw = sb.toString().trim();
            logger.info("AWC " + path + " [form] HTTP " + httpStatus + " raw=" + (raw.length() > 200 ? raw.substring(0, 200) : raw));
            if (raw.isEmpty() || !raw.startsWith("{")) {
                JSONObject err = new JSONObject();
                err.put("status", "9999");
                err.put("desc", "Non-JSON response (HTTP " + httpStatus + "): " + (raw.length() > 100 ? raw.substring(0, 100) : raw));
                return err;
            }
            JSONObject resp = new JSONObject(raw);
            if (!resp.has("status") && resp.has("statusCode")) {
                resp.put("status", String.valueOf(resp.get("statusCode")));
            }
            return resp;
        } catch (Exception e) {
            logger.error("AWC POST-FORM " + path + " failed", e);
            JSONObject err = new JSONObject();
            err.put("status", "9999");
            err.put("desc", "HTTP error: " + e.getMessage());
            return err;
        }
    }
}

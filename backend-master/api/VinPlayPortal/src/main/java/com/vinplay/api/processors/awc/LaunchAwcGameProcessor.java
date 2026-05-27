package com.vinplay.api.processors.awc;

import com.vinplay.vbee.common.config.AwcApiClient;
import com.vinplay.vbee.common.config.AwcConfig;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * c=3095 — Launch an AWC game for the player.
 *
 * Two modes:
 * 1. Specific game: game_code + platform → doLoginAndLaunchGame
 * 2. Provider lobby: platform only → login (opens provider lobby)
 *
 * Auto-creates AWC member on first launch (idempotent).
 * Returns: {"success":true,"data":{"url":"https://..."}}
 */
public class LaunchAwcGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = LoggerFactory.getLogger("portal");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            if (!AwcConfig.isEnabled()) {
                return err(response, "5002", "AWC is disabled");
            }

            HttpServletRequest request = param.get();

            String token = request.getParameter("at");
            if (token == null || token.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }
            com.hazelcast.core.IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String nickname = tokenMap.get(token);
            if (nickname == null || nickname.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }

            String platform = request.getParameter("platform");
            String gameCode = request.getParameter("game_code");
            if (gameCode == null || gameCode.isEmpty()) gameCode = request.getParameter("gameCode");
            // Accept both snake_case (legacy FE) and camelCase (sunkr-admin-next, AWC docs) keys.
            String gameType = request.getParameter("game_type");
            if (gameType == null || gameType.isEmpty()) gameType = request.getParameter("gameType");
            String betLimit = request.getParameter("bet_limit");
            if (betLimit == null || betLimit.isEmpty()) betLimit = request.getParameter("betLimit");
            String device = request.getParameter("device");
            if (device == null || device.isEmpty()) device = request.getParameter("isMobile");
            // SEXYBCRT direct-to-table params (AWC doLoginAndLaunchGame):
            //   game_table_id          — table id (e.g. "101")
            //   is_launch_game_table   — "1"/"true" to enter that specific table
            //   hall                   — SEXYBCRT-only alternative to table id (e.g. "SEXY")
            String gameTableId = request.getParameter("game_table_id");
            if (gameTableId == null || gameTableId.isEmpty()) gameTableId = request.getParameter("gameTableId");
            String hall = request.getParameter("hall");
            String launchTableStr = request.getParameter("is_launch_game_table");
            boolean launchGameTable = "1".equals(launchTableStr) || "true".equalsIgnoreCase(launchTableStr);
            if (device == null || device.isEmpty()) {
                // `pf` is the GSC-style device param — accept as alias so FE
                // can use identical param names across c=3090 (GSC) and c=3095
                // (AWC) without a per-aggregator branch.
                device = request.getParameter("pf");
            }

            // GSC → AWC param compat (2026-04-22):
            //   FE may send `platform=WEB|MOBILE|DESKTOP` (that's a GSC param
            //   meaning device form-factor, not a provider code). If we see
            //   one of those values in `platform`, promote it to `device` and
            //   blank out platform so the real AWC platform can come from
            //   awc_game_catalog lookup below.
            if (platform != null) {
                String p = platform.trim().toUpperCase();
                if ("WEB".equals(p) || "MOBILE".equals(p) || "DESKTOP".equals(p)) {
                    if (device == null || device.isEmpty()) device = p;
                    platform = null;
                }
            }

            // GSC → AWC param compat — normalize game_type vocabulary:
            //   LIVE_CASINO → LIVE, FISHING → FH, SPORT_BOOK → ESPORTS.
            //   Pass-through anything already canonical (LIVE/SLOT/FH/EGAME/ESPORTS).
            if (gameType != null && !gameType.isEmpty()) {
                String g = gameType.trim().toUpperCase();
                if ("LIVE_CASINO".equals(g))       gameType = "LIVE";
                else if ("FISHING".equals(g))      gameType = "FH";
                else if ("SPORT_BOOK".equals(g))   gameType = "ESPORTS";
                else                               gameType = g;
            }
            if (gameType == null || gameType.isEmpty()) gameType = "SLOT";

            // If platform wasn't supplied (or was a device value we moved out),
            // look it up from the catalog by game_code. Lets FE send only the
            // game_code on a unified lobby tile without knowing which
            // aggregator the game belongs to.
            if ((platform == null || platform.isEmpty()) && gameCode != null && !gameCode.isEmpty()) {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                             "SELECT platform FROM awc_game_catalog WHERE game_code = ? AND is_active = 1 LIMIT 1")) {
                    ps.setString(1, gameCode);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) platform = rs.getString(1);
                    }
                } catch (Exception lookupErr) {
                    logger.warn("LaunchAwcGameProcessor: catalog platform lookup failed for {}: {}", gameCode, lookupErr.getMessage());
                }
            }

            if (platform == null || platform.isEmpty()) {
                return err(response, "4001", "platform required (AWC platform code like SEXYBCRT/JILI, or send game_code so backend can look it up)");
            }

            boolean isMobile = "MOBILE".equalsIgnoreCase(device) || "1".equals(device);

            // Auto-create member (idempotent — AWC returns 0000 or 1001 "Account existed").
            // Hard-fail on any other status: if createMember silently fails (e.g.
            // betLimit currency mismatch — "has not correct betLimit of KRW"),
            // the subsequent doLoginAndLaunchGame returns "Account is not
            // exists [1002]" which is misleading. Surface the real cause to
            // the caller instead of falling through.
            JSONObject createResp = AwcApiClient.createMember(nickname, nickname);
            String createStatus = createResp.optString("status", "");
            if (!"0000".equals(createStatus) && !"1001".equals(createStatus)) {
                String desc = createResp.optString("desc", createResp.optString("message", ""));
                logger.warn("AWC createMember failed for {}: status={} desc={}", nickname, createStatus, desc);
                return err(response, "5003", "AWC createMember failed: " + desc);
            }

            // Launch game or lobby. SUN-1326 follow-up 2026-05-14: AWC
            // sometimes returns 500 / "Platform login failed [1033]: Invalid
            // Game" immediately after a fresh createMember sets the
            // betLimit — their backend hasn't propagated the limit to the
            // platform routing table yet. Retry once with a 1.5s delay so
            // the first-time launch succeeds without forcing the FE to
            // re-issue the call.
            String externalURL = AwcConfig.callbackUrl().replace("/awc/callback", "");
            JSONObject awcResp = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                if (gameCode != null && !gameCode.isEmpty()) {
                    awcResp = AwcApiClient.doLoginAndLaunchGame(nickname, platform, gameCode, gameType, isMobile, betLimit,
                            gameTableId, launchGameTable, hall);
                } else {
                    awcResp = AwcApiClient.login(nickname, platform, gameType, isMobile, betLimit, externalURL);
                }
                String url0 = awcResp == null ? null : awcResp.optString("url", "");
                if (url0 != null && !url0.isEmpty()) break;
                String err0 = awcResp == null ? "" : awcResp.optString("message", awcResp.optString("desc", ""));
                if (attempt == 0 && err0.contains("[1033]")) {
                    try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                break;
            }

            // AWC success: {url, extension} (no status field)
            // AWC error: {status:"XXXX", desc:"..."} or {statusCode:400, message:"..."}
            String gameUrl = awcResp.optString("url", "");
            if (!gameUrl.isEmpty()) {
                JSONObject data = new JSONObject();
                data.put("url", gameUrl);
                data.put("platform", platform);
                if (gameCode != null) data.put("game_code", gameCode);
                data.put("game_type", gameType);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            } else {
                String errMsg = awcResp.optString("desc", awcResp.optString("message", awcResp.optString("error", "Unknown")));
                return err(response, "5001", "AWC error: " + errMsg);
            }
        } catch (Exception e) {
            logger.error("LaunchAwcGameProcessor error", e);
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
}

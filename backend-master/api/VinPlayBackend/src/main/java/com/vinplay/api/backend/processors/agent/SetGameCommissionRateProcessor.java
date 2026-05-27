package com.vinplay.api.backend.processors.agent;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9849 — Set per-game commission rates for an agent.
 *
 * Admin sets individual game rates for each agent.
 * Each game rate must be <= parent agent's rate for that game.
 * Total doesn't need to equal global rate — each game is independent.
 *
 * Params:
 *   nn    = agent nickname (required)
 *   games = JSON array: [{"game_key":"taixiu","rate":0.5},{"game_key":"xocdia","rate":0.3}] (required)
 *           Use rate=0 or omit a game to use the global default.
 *           Use rate=-1 to DELETE a per-game override (falls back to global).
 */
public class SetGameCommissionRateProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String agentNick = request.getParameter("nn");
            String gamesJson = request.getParameter("games");
            int force = 0;
            String forceStr = request.getParameter("force");
            if (forceStr != null && !forceStr.isEmpty()) {
                try { force = Integer.parseInt(forceStr); } catch (Exception ignored) {}
            }

            // SUN-667: Also accept payload from JSON request body when not in query/form params
            // (FE may send POST application/json: {"nn":"...","games":[...]})
            if ((agentNick == null || agentNick.isEmpty()) || (gamesJson == null || gamesJson.isEmpty())) {
                try {
                    StringBuilder sb = new StringBuilder();
                    java.io.BufferedReader br = request.getReader();
                    if (br != null) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    String body = sb.toString().trim();
                    if (!body.isEmpty() && body.startsWith("{")) {
                        JSONObject bodyJson = new JSONObject(body);
                        if ((agentNick == null || agentNick.isEmpty()) && bodyJson.has("nn")) {
                            agentNick = bodyJson.getString("nn");
                        }
                        if ((gamesJson == null || gamesJson.isEmpty()) && bodyJson.has("games")) {
                            // games can be array or string
                            Object g = bodyJson.get("games");
                            gamesJson = (g instanceof JSONArray) ? g.toString() : g.toString();
                        }
                        if (bodyJson.has("force")) {
                            force = bodyJson.optInt("force", 0);
                        }
                    }
                } catch (Exception bodyErr) {
                    logger.warn("SUN-667 body parse skipped: " + bodyErr.getMessage());
                }
            }

            if (agentNick == null || agentNick.isEmpty()) {
                return err(response, "4001", "nn (agent nickname) required");
            }
            if (gamesJson == null || gamesJson.isEmpty()) {
                return err(response, "4001", "games JSON array required");
            }

            JSONArray gamesArr;
            try {
                gamesArr = new JSONArray(gamesJson);
            } catch (Exception e) {
                return err(response, "4002", "Invalid games JSON format");
            }

            // Resolve agent
            int agentId = 0;
            int parentId = -1;
            try (Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id, parentid FROM useragent WHERE nickname = ?")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            agentId = rs.getInt("id");
                            parentId = rs.getInt("parentid");
                        }
                    }
                }
            }
            if (agentId == 0) return err(response, "1002", "Agent not found: " + agentNick);

            // Get parent's per-game rates for validation
            String parentNick = null;
            double parentGlobalRate = 100; // top-level has no limit
            int parentLevel = -1;
            java.util.Map<String, Double> parentGameRates = new java.util.HashMap<>();

            if (parentId > 0) {
                try (Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT nickname, commission_rate, level FROM useragent WHERE id = ?")) {
                        ps.setInt(1, parentId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                parentNick = rs.getString("nickname");
                                parentGlobalRate = rs.getDouble("commission_rate");
                                parentLevel = rs.getInt("level");
                            }
                        }
                    }
                }

                // System-root (level=0, e.g. SpecialAccount) has no real commission
                // ceiling — its global rate is irrelevant. Per-game rates still apply
                // if configured, but the fallback is unlimited (100%).
                if (parentLevel == 0) {
                    parentGlobalRate = 100;
                }

                if (parentNick != null) {
                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT game_key, rate FROM game_commission_rate WHERE agent_nickname = ?")) {
                            ps.setString(1, parentNick);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    parentGameRates.put(rs.getString("game_key"), rs.getDouble("rate"));
                                }
                            }
                        }
                    }
                }
            }

            // Validate all games exist in our map
            java.util.Map<String, String> validGames = ListGameCommissionConfigProcessor.getGameMap();

            // PASS 1: Check for any downline exceeds BEFORE applying any changes!
            // This ensures we do not partially save changes before showing the 4015 confirmation popup.
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                for (int i = 0; i < gamesArr.length(); i++) {
                    JSONObject gameObj = gamesArr.getJSONObject(i);
                    String gameKey = gameObj.optString("game_key", "");
                    if (gameKey.isEmpty()) continue;

                    double rate;
                    try {
                        rate = gameObj.getDouble("rate");
                    } catch (Exception e) { continue; }

                    if (rate < 0 || rate > 100) continue;

                    if (adminConn != null) {
                        boolean exceeds = checkDownlineExceeds(adminConn, conn, agentId, gameKey, rate);
                        if (exceeds && force != 1) {
                            response.put("success", false);
                            response.put("errorCode", "4015");
                            response.put("message", "Hoa hồng game này ở tuyến dưới sẽ reset về 0%, bạn có muốn tiếp tục?");
                            return response.toString();
                        }
                    }
                }
            }

            // PASS 2: Process each game rate
            JSONArray results = new JSONArray();
            int updated = 0, deleted = 0, errors = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                for (int i = 0; i < gamesArr.length(); i++) {
                    JSONObject gameObj = gamesArr.getJSONObject(i);
                    String gameKey = gameObj.optString("game_key", "");
                    
                    if (gameKey.isEmpty()) {
                        JSONObject result = new JSONObject();
                        result.put("game_key", "UNKNOWN");
                        result.put("status", "error");
                        result.put("message", "Missing game_key");
                        errors++;
                        results.put(result);
                        continue;
                    }

                    double rate;
                    try {
                        rate = gameObj.getDouble("rate");
                    } catch (Exception e) {
                        JSONObject result = new JSONObject();
                        result.put("game_key", gameKey);
                        result.put("status", "error");
                        result.put("message", "Invalid rate format");
                        errors++;
                        results.put(result);
                        continue;
                    }

                    JSONObject result = new JSONObject();
                    result.put("game_key", gameKey);

                    // Accepted key patterns:
                    //   - Offline per-game:      one of the hardcoded GAME_MAP
                    //                            entries (taixiu, slot_*, ...).
                    //   - Live per-game:         gsc_<product_code>_<game_code>
                    //                            from SUN-865 (c=9893/9894 lookup).
                    //   - Offline per-category:  offline_cat_<CategoryName>
                    //                            (TaiXiu, Slot, CardGame, BauCua,
                    //                            XocDia, CaoThap, MiniPoker, Fish).
                    //   - Live per-category:     live_cat_<CategoryName>
                    //                            (Baccarat, Roulette, Sicbo,
                    //                            DragonTiger, Blackjack, MegaBall,
                    //                            Slot, Other, …) — aggregates all
                    //                            GSC providers under that category.
                    boolean isLiveGsc      = gameKey.matches("^gsc_\\d+_[a-zA-Z0-9_\\-]+$");
                    boolean isOfflineCat   = gameKey.matches("^offline_cat_[a-zA-Z0-9_]+$")
                            && com.vinplay.dal.service.OfflineGameCategoryMap.allCategories()
                                .containsKey(gameKey.substring("offline_cat_".length()));
                    boolean isLiveCat      = gameKey.matches("^live_cat_[a-zA-Z0-9_]+$");
                    // SUN-1060: legacy mojibake keys like
                    //   live_cat_Sic Bo & Dice (TÃ i xá»‰u, xÃ³c Ä‘Ä©a)
                    // exist in game_commission_rate from an older FE that stored
                    // Vietnamese display names as keys. When CMS posts back the
                    // full set, those rows shouldn't fail the whole request —
                    // ListGameCommissionConfigProcessor already filters them out
                    // of the UI, so SET should silently skip them too. Detect by
                    // dynamic prefix + fails-strict-regex → skip, don't error.
                    boolean hasDynamicPrefix = gameKey.startsWith("live_cat_")
                            || gameKey.startsWith("offline_cat_")
                            || gameKey.startsWith("gsc_");
                    boolean isLegacyGarbage = hasDynamicPrefix && !isLiveGsc && !isOfflineCat && !isLiveCat;
                    if (isLegacyGarbage) {
                        // silent skip — treat as no-op, keep the HTTP response clean
                        continue;
                    }
                    if (!isLiveGsc && !isOfflineCat && !isLiveCat
                            && !validGames.containsKey(gameKey)) {
                        result.put("status", "error");
                        result.put("message", "Unknown game: " + gameKey);
                        errors++;
                        results.put(result);
                        continue;
                    }

                    // Delete override (rate = -1)
                    if (rate < 0) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM game_commission_rate WHERE agent_nickname = ? AND game_key = ?")) {
                            ps.setString(1, agentNick);
                            ps.setString(2, gameKey);
                            ps.executeUpdate();
                        }
                        result.put("status", "deleted");
                        deleted++;
                        results.put(result);
                        continue;
                    }

                    // Validate rate range
                    if (rate > 100) {
                        result.put("status", "error");
                        result.put("message", "Rate cannot exceed 100%");
                        errors++;
                        results.put(result);
                        continue;
                    }

                    // Validate against parent's rate for this game
                    if (parentId > 0) {
                        double parentRate = parentGameRates.containsKey(gameKey)
                                ? parentGameRates.get(gameKey)
                                : parentGlobalRate;
                        if (rate > parentRate) {
                            result.put("status", "error");
                            result.put("message", "Tỷ lệ vượt quá giới hạn cho phép (<=" + parentRate + "%)");
                            errors++;
                            results.put(result);
                            continue;
                        }
                    }

                    // Downline check and confirmation flow (Pass 2: Force is implicitly 1 if we reach here and exceeds=true)
                    if (adminConn != null) {
                        boolean exceeds = checkDownlineExceeds(adminConn, conn, agentId, gameKey, rate);
                        if (exceeds) {
                            cascadeGameCommissionResetToZero(adminConn, conn, agentId, gameKey);
                        }
                    }

                    // Upsert
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO game_commission_rate (agent_nickname, agent_user_id, game_key, rate) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE rate = VALUES(rate)")) {
                        ps.setString(1, agentNick);
                        ps.setInt(2, agentId);
                        ps.setString(3, gameKey);
                        ps.setDouble(4, rate);
                        ps.executeUpdate();
                    }
                    // SUN-1255 Phase 1: cascade the new rate to descendants
                    // (rate = max(floor, parent_rate - step) per
                    // commission_rate_policy). Top-down ordering keeps the
                    // pool-aware guard happy. Failure here should NOT roll
                    // back the parent write — the upsert itself already
                    // satisfies the per-row invariant; descendants stay at
                    // their previous rate until ops fixes them.
                    try {
                        int casc = com.vinplay.dal.service.CommissionPropagator
                                .cascadeRateChange(conn, agentId, gameKey);
                        if (casc > 0) {
                            result.put("descendants_updated", casc);
                        }
                    } catch (Exception cascErr) {
                        logger.warn("cascadeRateChange failed for agent_id=" + agentId
                                + " gameKey=" + gameKey + ": " + cascErr.getMessage());
                    }
                    result.put("status", "ok");
                    result.put("rate", rate);
                    updated++;
                    results.put(result);
                }
            }

            JSONObject data = new JSONObject();
            data.put("agent_nickname", agentNick);
            data.put("results", results);
            data.put("updated", updated);
            data.put("deleted", deleted);
            data.put("errors", errors);

            response.put("success", errors == 0);
            response.put("errorCode", errors == 0 ? "0" : "4005");
            response.put("data", data);

        } catch (Exception e) {
            logger.error("SetGameCommissionRateProcessor error", e);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    private boolean checkDownlineExceeds(Connection adminConn, Connection gameConn, int parentId, String gameKey, double newRate) throws Exception {
        try (PreparedStatement ps = adminConn.prepareStatement("SELECT id, nickname, commission_rate FROM useragent WHERE parentid = ?")) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int childId = rs.getInt("id");
                    String childNick = rs.getString("nickname");
                    double childGlobalRate = rs.getDouble("commission_rate");

                    double childEffectiveRate = childGlobalRate;
                    try (PreparedStatement gps = gameConn.prepareStatement("SELECT rate FROM game_commission_rate WHERE agent_nickname = ? AND game_key = ?")) {
                        gps.setString(1, childNick);
                        gps.setString(2, gameKey);
                        try (ResultSet grs = gps.executeQuery()) {
                            if (grs.next()) {
                                childEffectiveRate = grs.getDouble("rate");
                            }
                        }
                    }

                    if (childEffectiveRate > newRate) {
                        return true;
                    }

                    // Recurse
                    if (checkDownlineExceeds(adminConn, gameConn, childId, gameKey, newRate)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void cascadeGameCommissionResetToZero(Connection adminConn, Connection gameConn, int parentId, String gameKey) throws Exception {
        try (PreparedStatement ps = adminConn.prepareStatement("SELECT id, nickname FROM useragent WHERE parentid = ?")) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int childId = rs.getInt("id");
                    String childNick = rs.getString("nickname");

                    try (PreparedStatement gps = gameConn.prepareStatement(
                            "INSERT INTO game_commission_rate (agent_nickname, game_key, rate) VALUES (?, ?, 0) " +
                            "ON DUPLICATE KEY UPDATE rate = 0")) {
                        gps.setString(1, childNick);
                        gps.setString(2, gameKey);
                        gps.executeUpdate();
                    }

                    // Recurse
                    cascadeGameCommissionResetToZero(adminConn, gameConn, childId, gameKey);
                }
            }
        }
    }
}

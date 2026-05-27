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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * c=9848 — List all configurable games + per-agent commission rates.
 *
 * Without agent param: returns game list with default names.
 * With nn (agent nickname): returns game list + that agent's per-game rates.
 *
 * Params: nn (optional, agent nickname)
 */
public class ListGameCommissionConfigProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    // Game key → Display name mapping (matches log_report_user columns)
    private static final LinkedHashMap<String, String> GAME_MAP = new LinkedHashMap<>();
    static {
        // Minigames
        GAME_MAP.put("taixiu", "Tài Xỉu");
        GAME_MAP.put("taixiu_st", "Sicbo");
        GAME_MAP.put("caothap", "Cao Thấp");
        GAME_MAP.put("minipoker", "Mini Poker");
        GAME_MAP.put("baucua", "Bầu Cua");
        GAME_MAP.put("xocdia", "Xóc Đĩa");
        // Card games
        GAME_MAP.put("tlmn", "Tiến Lên Miền Nam");
        GAME_MAP.put("bacay", "Ba Cây");
        // Slots
        GAME_MAP.put("slot_pokemon", "Slot Pokemon");
        GAME_MAP.put("slot_chiemtinh", "Slot Chiêm Tinh");
        GAME_MAP.put("slot_bikini", "Slot Bikini");
        GAME_MAP.put("slot_galaxy", "Slot Galaxy");
        GAME_MAP.put("slot_thanbai", "Slot Thần Bài");
        GAME_MAP.put("slot_bitcoin", "Slot Bitcoin");
        GAME_MAP.put("slot_taydu", "Slot Tây Du");
        GAME_MAP.put("slot_angrybird", "Slot Angry Bird");
        GAME_MAP.put("slot_thantai", "Slot Thần Tài");
        GAME_MAP.put("slot_thethao", "Slot Thể Thao");
        // Fish
        GAME_MAP.put("fish", "Bắn Cá");
        // 3rd party
        GAME_MAP.put("wm", "WM Casino");
        GAME_MAP.put("ibc", "IBC Sports");
        GAME_MAP.put("ag", "AG Casino");
        GAME_MAP.put("cmd", "CMD Sports");
        GAME_MAP.put("ebet", "eBet");
        GAME_MAP.put("sbo", "SBO Sports");
    }

    public static Map<String, String> getGameMap() {
        return GAME_MAP;
    }

    public static int compareDynamicKeys(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        boolean aLive = a.startsWith("live_cat_");
        boolean bLive = b.startsWith("live_cat_");
        boolean aOff  = a.startsWith("offline_cat_");
        boolean bOff  = b.startsWith("offline_cat_");
        if (aLive && bLive) {
            java.util.List<String> liveCatOrder = new java.util.ArrayList<>(LIVE_CAT_LABEL.keySet());
            int ai = liveCatOrder.indexOf(a.substring("live_cat_".length()));
            int bi = liveCatOrder.indexOf(b.substring("live_cat_".length()));
            if (ai == -1) ai = Integer.MAX_VALUE;
            if (bi == -1) bi = Integer.MAX_VALUE;
            if (ai == bi) return a.compareTo(b); // FALLBACK for stability
            return Integer.compare(ai, bi);
        }
        if (aLive)  return -1;
        if (bLive)  return  1;
        if (aOff && bOff) return a.compareTo(b);
        if (aOff)   return -1;
        if (bOff)   return  1;
        return a.compareTo(b);
    }

    // SUN-29: Vietnamese labels for live / offline category buckets.
    // Source: docs/ref/Game Evolution - Game Evolution.csv (product-authoritative).
    // Placeholders Fishing / Slot / Sports / Other: pending product confirmation.
    private static final LinkedHashMap<String, String> LIVE_CAT_LABEL = new LinkedHashMap<>();
    static {
        LIVE_CAT_LABEL.put("Baccarat",    "Baccarat");
        LIVE_CAT_LABEL.put("SicBoDice",   "Sic Bo & Dice (Tài xỉu, xóc đĩa)");
        LIVE_CAT_LABEL.put("DragonTiger", "Rồng hổ (Dragon Tiger)");
        LIVE_CAT_LABEL.put("Roulette",    "Roulette");
        LIVE_CAT_LABEL.put("Blackjack",   "Blackjack");
        LIVE_CAT_LABEL.put("Poker",       "Poker");
        LIVE_CAT_LABEL.put("GameShow",    "Game Show");
        LIVE_CAT_LABEL.put("Fishing",     "Bắn Cá Live");
        LIVE_CAT_LABEL.put("Slot",        "Slot");
        LIVE_CAT_LABEL.put("Sports",      "Thể Thao");
        LIVE_CAT_LABEL.put("Other",       "Khác");
    }

    private static String resolveDynamicDisplayName(String key) {
        if (key.startsWith("live_cat_")) {
            String cat = key.substring("live_cat_".length());
            return LIVE_CAT_LABEL.getOrDefault(cat, cat);
        }
        if (key.startsWith("offline_cat_")) {
            String cat = key.substring("offline_cat_".length());
            return LIVE_CAT_LABEL.getOrDefault(cat, cat);
        }
        if (key.startsWith("gsc_")) {
            // Format: gsc_<productCode>_<gameCode>
            String[] parts = key.split("_", 3);
            if (parts.length == 3) {
                try {
                    int pc = Integer.parseInt(parts[1]);
                    return com.vinplay.dal.service.GscGameNameResolver.displayName(pc, parts[2]);
                } catch (NumberFormatException ignored) {
                    // fall through
                } catch (Throwable t) {
                    // GscGameNameResolver may not be initialised in every admin-api
                    // deployment — return raw key rather than crash.
                }
            }
        }
        return key;
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String agentNick = request.getParameter("nn");

            // Load per-game rates for agent if specified
            Map<String, Double> agentRates = new LinkedHashMap<>();
            double globalRate = 0;

            if (agentNick != null && !agentNick.isEmpty()) {
                // Get global rate
                try (Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT commission_rate FROM useragent WHERE nickname = ?")) {
                        ps.setString(1, agentNick);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) globalRate = rs.getDouble("commission_rate");
                        }
                    }
                }

                // Get per-game rates
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT game_key, rate FROM game_commission_rate WHERE agent_nickname = ?")) {
                        ps.setString(1, agentNick);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                agentRates.put(rs.getString("game_key"), rs.getDouble("rate"));
                            }
                        }
                    }
                }
            }

            JSONArray games = new JSONArray();
            for (Map.Entry<String, String> entry : GAME_MAP.entrySet()) {
                JSONObject game = new JSONObject();
                game.put("game_key", entry.getKey());
                game.put("game_name", entry.getValue());
                if (agentNick != null && !agentNick.isEmpty()) {
                    double rate = agentRates.containsKey(entry.getKey())
                            ? agentRates.get(entry.getKey())
                            : 0.0;
                    // SUN-1098: emit as 2-decimal string so FE doesn't see "1.2" / "0".
                    game.put("rate", com.vinplay.dal.utils.PctFormatter.format(rate));
                    game.put("is_custom", agentRates.containsKey(entry.getKey()));
                }
                games.put(game);
            }

            // SUN-865 + SUN-category: surface dynamic-key rates too. Any
            // row in game_commission_rate whose game_key is NOT in the
            // hardcoded offline GAME_MAP falls into one of these buckets:
            //   - gsc_<pc>_<gc>         — live per-game (SUN-865)
            //   - offline_cat_<Cat>     — offline per-category (new)
            //   - live_cat_<Cat>        — live per-category (new)
            // FE enriches the display name client-side via c=9894.
            //
            // Sort dynamic keys by canonical LIVE_CAT_LABEL order so the list
            // is stable across saves. Without this the DB scan order changes
            // every time ON DUPLICATE KEY UPDATE is executed, causing the UI
            // to reorder games randomly after each save.
            int dynamicCount = 0;
            if (agentNick != null && !agentNick.isEmpty()) {
                // Collect valid dynamic keys first, then sort before emitting
                java.util.List<String> dynamicKeys = new java.util.ArrayList<>();
                for (Map.Entry<String, Double> r : agentRates.entrySet()) {
                    String key = r.getKey();
                    if (GAME_MAP.containsKey(key)) continue;
                    // Skip legacy/garbage keys with spaces, parentheses, etc.
                    // For example: "live_cat_Rồng hổ (Dragon Tiger)", "live_cat_Game Show" (SUN-865)
                    if (!key.matches("^[a-zA-Z0-9_\\-]+$")) continue;
                    dynamicKeys.add(key);
                }

                // Canonical order: live_cat_* by LIVE_CAT_LABEL position,
                // offline_cat_* alphabetically after, gsc_* last alphabetically.
                dynamicKeys.sort(ListGameCommissionConfigProcessor::compareDynamicKeys);

                for (String key : dynamicKeys) {
                    double rateVal = agentRates.get(key);
                    JSONObject game = new JSONObject();
                    game.put("game_key", key);
                    game.put("game_name", resolveDynamicDisplayName(key));
                    // SUN-1098: 2-decimal string for dynamic rate buckets.
                    game.put("rate", com.vinplay.dal.utils.PctFormatter.format(rateVal));
                    game.put("is_custom", true);
                    game.put("is_live", key.startsWith("gsc_") || key.startsWith("live_cat_"));
                    game.put("is_category", key.startsWith("offline_cat_") || key.startsWith("live_cat_"));
                    if (key.startsWith("offline_cat_")) {
                        game.put("category", key.substring("offline_cat_".length()));
                        game.put("category_scope", "OFFLINE");
                    } else if (key.startsWith("live_cat_")) {
                        game.put("category", key.substring("live_cat_".length()));
                        game.put("category_scope", "LIVE");
                    }
                    games.put(game);
                    dynamicCount++;
                }
            }
            int liveCount = dynamicCount;

            JSONObject data = new JSONObject();
            data.put("games", games);
            data.put("total", GAME_MAP.size() + liveCount);
            if (agentNick != null && !agentNick.isEmpty()) {
                data.put("agent_nickname", agentNick);
                data.put("global_rate", globalRate);
                data.put("custom_count", agentRates.size());
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

        } catch (Exception e) {
            logger.error("ListGameCommissionConfigProcessor error", e);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}

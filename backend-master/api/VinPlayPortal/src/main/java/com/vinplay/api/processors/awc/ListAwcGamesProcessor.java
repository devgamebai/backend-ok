package com.vinplay.api.processors.awc;

import com.vinplay.vbee.common.config.AwcConfig;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3096 — List available AWC games/providers for FE.
 *
 * <p>Reads {@code vinplay.games} (the unified catalog, SUN-GAME-FK
 * Phase 4). Same row drives the admin Game Catalog (c=9980-9983) and
 * the LS Cược / LS Rolling renderers, so admin toggles propagate to
 * FE without a separate sync.
 *
 * <p>Modes:
 * <ul>
 *   <li>No params → list distinct {@code vendor_platform} values for
 *       provider=AWC where any game in that platform is active.</li>
 *   <li>{@code ?platform=SEXYBCRT} → list games for that platform.
 *       Per-table rows ({@code table_tag != ''}) come back as separate
 *       items (Sexy Live Baccarat M01..M151 / C01..C18) so FE can
 *       render the per-table thumbnails.</li>
 * </ul>
 */
public class ListAwcGamesProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            String platformFilter = request.getParameter("platform");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                if (platformFilter != null && !platformFilter.isEmpty()) {
                    // Game list for one platform — one row per game_code +
                    // table_tag combination so the FE can show per-table
                    // entries when SEXYBCRT MX-LIVE-001 is split across
                    // M01..M151. Skip the placeholder row (game_code='*')
                    // — that's the platform stub for the resolver
                    // fallback chain, not a player-facing entry.
                    //
                    // Returns BOTH active AND inactive games. Caller's lobby
                    // can render an "under maintenance" badge using the
                    // surfaced `is_active` field. Bet-time enforcement
                    // (UserGameBlock + GscBetWhitelist) refuses the wager
                    // server-side if the player tries to play a disabled
                    // game, so the FE doesn't have to gatekeep.
                    JSONArray games = new JSONArray();
                    String sql = "SELECT id, game_code, table_tag, game_name, category_id, is_active "
                               + "FROM games WHERE provider='AWC' "
                               + "AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                               + "AND game_code <> '*' "
                               + "ORDER BY game_code, table_tag";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, platformFilter);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                JSONObject g = new JSONObject();
                                g.put("id", rs.getLong("id"));
                                g.put("game_code", rs.getString("game_code"));
                                String tag = rs.getString("table_tag");
                                g.put("table_tag", tag == null ? "" : tag);
                                g.put("game_name", rs.getString("game_name"));
                                g.put("category_id", rs.getInt("category_id"));
                                g.put("is_active", rs.getInt("is_active") == 1);
                                games.put(g);
                            }
                        }
                    }
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("platform", platformFilter);
                    response.put("total", games.length());
                    response.put("data", games);
                } else {
                    // Platform list — distinct vendor_platform from games.
                    // Stub row (game_code='*') gives us the human-friendly
                    // platform-level name; fall back to the vendor_platform
                    // code itself when no stub exists. Includes platforms
                    // even if every game inside is inactive — server-side
                    // enforcement handles the "no bets" path.
                    JSONArray platforms = new JSONArray();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT vendor_platform, "
                                    + "MAX(CASE WHEN game_code='*' THEN game_name END) AS stub_name, "
                                    + "MIN(category_id) AS category_id, "
                                    + "COUNT(*) AS game_count, "
                                    + "SUM(is_active) AS active_count "
                                    + "FROM games WHERE provider='AWC' "
                                    + "GROUP BY vendor_platform ORDER BY vendor_platform")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                JSONObject p = new JSONObject();
                                String vp = rs.getString("vendor_platform");
                                p.put("platform", vp);
                                String stub = rs.getString("stub_name");
                                p.put("provider_name", stub != null ? stub : vp);
                                p.put("category_id", rs.getInt("category_id"));
                                p.put("game_count", rs.getInt("game_count"));
                                p.put("active_count", rs.getInt("active_count"));
                                platforms.put(p);
                            }
                        }
                    }
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("total", platforms.length());
                    response.put("data", platforms);
                }
            }
        } catch (Exception e) {
            logger.error("ListAwcGamesProcessor error", e);
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

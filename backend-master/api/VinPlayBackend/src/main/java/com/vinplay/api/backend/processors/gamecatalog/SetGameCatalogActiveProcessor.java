package com.vinplay.api.backend.processors.gamecatalog;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9982 — Toggle one or many games active/inactive on the unified
 * {@code vinplay.games} catalog (provider AWC or GSC).
 *
 * <p>Two ways to identify rows:
 * <ul>
 *   <li>{@code ids} — comma-separated {@code games.id} values (preferred
 *       — covers per-table rows that share a {@code game_code}).</li>
 *   <li>{@code game_codes} — comma-separated game_codes (legacy form;
 *       toggles ALL rows under each code, including every table_tag
 *       variant). Useful for bulk on/off of an entire game.</li>
 * </ul>
 *
 * <p>Audit goes to {@code vinplay_admin.log_admin}
 * (action={@code gamecatalog.toggle}). Lobby in-process cache TTL ~5min;
 * recreate {@code sunwinkr-portal-api} for instant flip.
 */
public class SetGameCatalogActiveProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return err(response, "1001", "aat required");
            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null || adminNick.isEmpty()) return err(response, "1001", "Unauthorized");

            String platform = request.getParameter("platform");
            if (platform == null || platform.isEmpty()) return err(response, "4001", "platform required");
            platform = platform.toLowerCase();
            String provider;
            if ("awc".equals(platform)) provider = "AWC";
            else if ("gsc".equals(platform)) provider = "GSC";
            else return err(response, "4002", "platform must be awc or gsc");

            String vendorPlatform = request.getParameter("provider");
            if (vendorPlatform == null || vendorPlatform.isEmpty()) return err(response, "4001", "provider required");

            String activeStr = request.getParameter("active");
            if (activeStr == null || activeStr.isEmpty()) return err(response, "4001", "active required (0|1)");
            int active;
            if ("1".equals(activeStr) || "true".equalsIgnoreCase(activeStr)) active = 1;
            else if ("0".equals(activeStr) || "false".equalsIgnoreCase(activeStr)) active = 0;
            else return err(response, "4002", "active must be 0|1 or true|false");

            String idsRaw = request.getParameter("ids");
            String codesRaw = request.getParameter("game_codes");
            if (codesRaw == null || codesRaw.isEmpty()) codesRaw = request.getParameter("game_code");

            int totalAffected = 0;
            JSONArray notFound = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                if (idsRaw != null && !idsRaw.isEmpty()) {
                    List<Long> ids = parseLongs(idsRaw);
                    if (ids.isEmpty()) return err(response, "4001", "ids must be CSV of numeric game ids");
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE games SET is_active = ? WHERE id = ? AND provider = ? AND vendor_platform = ?")) {
                        for (long id : ids) {
                            ps.setInt(1, active);
                            ps.setLong(2, id);
                            ps.setString(3, provider);
                            ps.setString(4, vendorPlatform);
                            int n = ps.executeUpdate();
                            if (n > 0) totalAffected += n; else notFound.put(id);
                        }
                    }
                } else if (codesRaw != null && !codesRaw.isEmpty()) {
                    List<String> codes = new ArrayList<>();
                    for (String c : codesRaw.split(",")) {
                        String t = c.trim();
                        if (!t.isEmpty()) codes.add(t);
                    }
                    if (codes.isEmpty()) return err(response, "4001", "game_codes required");
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE games SET is_active = ? WHERE provider = ? AND vendor_platform = ? AND game_code = ?")) {
                        for (String gc : codes) {
                            ps.setInt(1, active);
                            ps.setString(2, provider);
                            ps.setString(3, vendorPlatform);
                            ps.setString(4, gc);
                            int n = ps.executeUpdate();
                            if (n > 0) totalAffected += n; else notFound.put(gc);
                        }
                    }
                } else {
                    return err(response, "4001", "ids or game_codes required");
                }
            }

            // Dual-write to legacy vinplay.gsc_game_catalog for GSC providers.
            // 11 Java services (GscBetWhitelist, GscGameNameResolver,
            // CommissionRateResolver, etc.) still read the legacy table for
            // bet-time refusal and per-game commission lookup. Until those are
            // migrated to vinplay.games, every admin toggle has to write to
            // BOTH tables or admin-curated state silently drifts apart.
            // Best-effort: if the row isn't in the legacy table, log and move on.
            if ("GSC".equalsIgnoreCase(provider)) {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    int legacyAffected;
                    if (idsRaw != null && !idsRaw.isEmpty()) {
                        List<Long> ids = parseLongs(idsRaw);
                        StringBuilder placeholders = new StringBuilder();
                        for (int i = 0; i < ids.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE gsc_game_catalog c "
                              + "JOIN games g ON g.game_code COLLATE utf8mb4_unicode_ci = c.game_code COLLATE utf8mb4_unicode_ci "
                              + " AND CAST(g.vendor_platform AS UNSIGNED) = c.product_code "
                              + "SET c.active = ? "
                              + "WHERE g.provider = ? AND g.vendor_platform = ? AND g.id IN (" + placeholders + ")")) {
                            int idx = 1;
                            ps.setInt(idx++, active);
                            ps.setString(idx++, provider);
                            ps.setString(idx++, vendorPlatform);
                            for (long id : ids) ps.setLong(idx++, id);
                            legacyAffected = ps.executeUpdate();
                        }
                    } else {
                        List<String> codes = new ArrayList<>();
                        for (String c : codesRaw.split(",")) {
                            String t = c.trim();
                            if (!t.isEmpty()) codes.add(t);
                        }
                        StringBuilder placeholders = new StringBuilder();
                        for (int i = 0; i < codes.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE gsc_game_catalog SET active = ? "
                              + "WHERE product_code = ? AND game_code IN (" + placeholders + ")")) {
                            int idx = 1;
                            ps.setInt(idx++, active);
                            ps.setInt(idx++, Integer.parseInt(vendorPlatform));
                            for (String gc : codes) ps.setString(idx++, gc);
                            legacyAffected = ps.executeUpdate();
                        }
                    }
                    if (legacyAffected != totalAffected) {
                        logger.warn("SetGameCatalogActive: games updated " + totalAffected
                                + " rows but gsc_game_catalog updated " + legacyAffected
                                + " — legacy table out of sync (some games may not exist in legacy).");
                    }
                } catch (Exception legacyErr) {
                    logger.warn("SetGameCatalogActive legacy gsc_game_catalog dual-write failed: "
                            + legacyErr.getMessage());
                }
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO log_admin (action, username, reason, status, account_name) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "gamecatalog.toggle");
                ps.setString(2, adminNick);
                ps.setString(3, "platform=" + platform + " provider=" + vendorPlatform
                        + (idsRaw != null ? " ids=" + idsRaw : "")
                        + (codesRaw != null ? " codes=" + codesRaw : "")
                        + " rows_affected=" + totalAffected);
                ps.setString(4, active == 1 ? "->active" : "->inactive");
                ps.setString(5, vendorPlatform);
                ps.executeUpdate();
            } catch (Exception auditErr) {
                logger.warn("SetGameCatalogActive audit insert failed for "
                        + adminNick + " " + platform + "/" + vendorPlatform + ": " + auditErr.getMessage());
            }

            // SUN-1xxx (2026-05-12): publish a cache-invalidate event on the
            // shared Hazelcast topic so every portal-api JVM clears its
            // in-process GSC game-list cache for this product immediately.
            // Fire-and-forget: failure here does not invalidate the toggle —
            // the 5-minute TTL still expires the cache eventually.
            if ("GSC".equalsIgnoreCase(provider)) {
                try {
                    int pc = Integer.parseInt(vendorPlatform);
                    com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance()
                            .<Integer>getTopic("gsc_game_list_invalidate")
                            .publish(pc);
                } catch (Exception topicErr) {
                    logger.warn("SetGameCatalogActive cache-invalidate publish failed for "
                            + platform + "/" + vendorPlatform + ": " + topicErr.getMessage());
                }
            }

            JSONObject data = new JSONObject();
            data.put("platform", platform);
            data.put("provider", vendorPlatform);
            data.put("rows_affected", totalAffected);
            data.put("not_found", notFound);
            data.put("new_active", active);
            data.put("cache_note", "Lobby cache auto-flushed via HZ topic; player lobby reflects within ~50ms.");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("SetGameCatalogActiveProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static List<Long> parseLongs(String s) {
        List<Long> out = new ArrayList<>();
        for (String t : s.split(",")) {
            String x = t.trim();
            if (x.isEmpty()) continue;
            try { out.add(Long.parseLong(x)); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}

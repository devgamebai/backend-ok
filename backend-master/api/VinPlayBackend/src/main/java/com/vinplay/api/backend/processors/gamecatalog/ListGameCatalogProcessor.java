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
import java.sql.ResultSet;

/**
 * c=9981 — List games for a platform/provider with paging + search.
 *
 * <p>Reads {@code vinplay.games} so the rows match what the
 * {@code AwcGameNameResolver} / {@code GscGameNameResolver} return at
 * read time. Per-table rows surface as separate items via the
 * {@code table_tag} column (e.g. SEXYBCRT MX-LIVE-001 row per Mexico
 * baccarat table).
 *
 * <p>Params:
 * <ul>
 *   <li>{@code platform} = awc | gsc</li>
 *   <li>{@code provider} = AWC vendor_platform (e.g. JILI) or GSC product_code</li>
 *   <li>{@code q} (optional) — substring filter on game_code, table_tag, or game_name</li>
 *   <li>{@code active} (optional) — 0|1, omit for all</li>
 *   <li>{@code page} default 1, {@code size} default 50 (max 200)</li>
 * </ul>
 */
public class ListGameCatalogProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            // For GSC the FE may send product_code as a string; we trust it
            // verbatim — vinplay.games stores it as VARCHAR in vendor_platform.

            String q = request.getParameter("q");
            String activeStr = request.getParameter("active");
            Integer activeFilter = null;
            if (activeStr != null && !activeStr.isEmpty()) {
                if ("1".equals(activeStr) || "true".equalsIgnoreCase(activeStr)) activeFilter = 1;
                else if ("0".equals(activeStr) || "false".equalsIgnoreCase(activeStr)) activeFilter = 0;
                else return err(response, "4002", "active must be 0|1 or omitted");
            }

            int page = parseIntOr(request.getParameter("page"), 1);
            if (page < 1) page = 1;
            int size = parseIntOr(request.getParameter("size"), 50);
            if (size < 1) size = 50;
            if (size > 200) size = 200;
            int offset = (page - 1) * size;

            String like = (q == null || q.isEmpty()) ? null : "%" + q + "%";
            StringBuilder where = new StringBuilder("WHERE provider = ? AND vendor_platform = ?");
            if (like != null) where.append(" AND (game_code LIKE ? OR game_name LIKE ? OR table_tag LIKE ?)");
            if (activeFilter != null) where.append(" AND is_active = ?");

            JSONArray rows = new JSONArray();
            int total = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM games " + where)) {
                    int idx = 1;
                    ps.setString(idx++, provider);
                    ps.setString(idx++, vendorPlatform);
                    if (like != null) {
                        ps.setString(idx++, like); ps.setString(idx++, like); ps.setString(idx++, like);
                    }
                    if (activeFilter != null) ps.setInt(idx, activeFilter);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getInt(1); }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, game_code, table_tag, game_name, category_id, is_active "
                                + "FROM games " + where
                                + " ORDER BY game_code, table_tag LIMIT ? OFFSET ?")) {
                    int idx = 1;
                    ps.setString(idx++, provider);
                    ps.setString(idx++, vendorPlatform);
                    if (like != null) {
                        ps.setString(idx++, like); ps.setString(idx++, like); ps.setString(idx++, like);
                    }
                    if (activeFilter != null) ps.setInt(idx++, activeFilter);
                    ps.setInt(idx++, size);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject r = new JSONObject();
                            r.put("id", rs.getLong("id"));
                            r.put("game_code", rs.getString("game_code"));
                            r.put("table_tag", rs.getString("table_tag"));
                            r.put("game_name", rs.getString("game_name"));
                            r.put("category_id", rs.getInt("category_id"));
                            r.put("active", rs.getInt("is_active"));
                            rows.put(r);
                        }
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("platform", platform);
            data.put("provider", vendorPlatform);
            data.put("page", page);
            data.put("size", size);
            data.put("total", total);
            data.put("items", rows);
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ListGameCatalogProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static int parseIntOr(String s, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}

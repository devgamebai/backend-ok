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
import java.util.HashMap;
import java.util.Map;

/**
 * c=9980 — List providers under a platform with game counts.
 *
 * <p>Param: {@code platform} = {@code awc} or {@code gsc}.
 * <p>Returns rows: {provider, provider_name, total, active}.
 *
 * <p>Reads {@code vinplay.games} (the unified catalog, SUN-GAME-FK Phase 4).
 * For AWC, {@code vendor_platform} carries the platform code (JILI, PG,
 * SEXYBCRT…). For GSC, {@code vendor_platform} carries the numeric
 * {@code product_code} as a string; {@code provider_name} is resolved
 * via {@code vinplay.gsc_product_map}.
 */
public class ListGameCatalogProvidersProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            JSONArray rows = new JSONArray();
            Map<Integer, String> gscNames = "GSC".equals(provider) ? loadGscProviderNames() : null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT vendor_platform, COUNT(*) AS total, SUM(is_active) AS active "
                                 + "FROM games WHERE provider = ? GROUP BY vendor_platform ORDER BY vendor_platform")) {
                ps.setString(1, provider);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String vp = rs.getString("vendor_platform");
                        if (vp == null) continue;
                        JSONObject r = new JSONObject();
                        r.put("provider", vp);
                        if (gscNames != null) {
                            try { r.put("provider_name", gscNames.getOrDefault(Integer.parseInt(vp), vp)); }
                            catch (NumberFormatException e) { r.put("provider_name", vp); }
                        } else {
                            r.put("provider_name", vp);
                        }
                        r.put("total", rs.getInt("total"));
                        r.put("active", rs.getInt("active"));
                        rows.put(r);
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("platform", platform);
            data.put("providers", rows);
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ListGameCatalogProvidersProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static Map<Integer, String> loadGscProviderNames() {
        Map<Integer, String> out = new HashMap<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT product_code, provider_name FROM gsc_product_map");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getInt(1), rs.getString(2));
            }
        } catch (Exception e) {
            logger.warn("loadGscProviderNames failed (returning empty): " + e.getMessage());
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

package com.vinplay.api.backend.processors.gsc;

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
 * c=9893 — SUN-885 + SUN-category.
 *
 * Default behaviour (no params): returns two virtual providers that the
 * admin CMS commission-config page binds to:
 * <pre>
 *   OFFLINE — our in-house games (TaiXiu, Slot_*, XocDia, TLMN, BaCay, …)
 *   LIVE    — all GSC third-party games collapsed into cross-provider categories
 * </pre>
 *
 * Legacy raw-provider listing (13 GSC providers — Evolution, PG Soft, SA
 * Gaming, Dream, JILI, JDB, Saba, SBO, …) is still available by passing
 * {@code ?raw=1}. Useful for ops tooling and backward-compat with older
 * admin UI builds.
 *
 * <p>Source for raw mode: {@code vinplay.gsc_product_map} wildcard rows
 * ({@code game_type = '*'}), 1 per provider.
 */
public class ListGSCProvidersProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String raw = request != null ? request.getParameter("raw") : null;

            JSONArray data = new JSONArray();

            if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) {
                // Legacy mode — list every GSC provider on the operator.
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT product_code, provider_name, category, game_key, game_label_vi "
                           + "FROM vinplay.gsc_product_map "
                           + "WHERE is_active = 1 AND game_type = '*' "
                           + "ORDER BY category, product_code")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("product_code", rs.getInt("product_code"));
                            row.put("provider_name", rs.getString("provider_name"));
                            row.put("category", rs.getString("category"));
                            row.put("game_key", rs.getString("game_key"));
                            row.put("label_vi", rs.getString("game_label_vi") != null ? rs.getString("game_label_vi") : "");
                            data.put(row);
                        }
                    }
                }
            } else {
                // Default mode — 2 virtual providers for category-based
                // commission configuration (SUN-category).
                data.put(new JSONObject()
                        .put("code", "OFFLINE")
                        .put("name", "Offline Games")
                        .put("label_vi", "Game Offline"));
                data.put(new JSONObject()
                        .put("code", "LIVE")
                        .put("name", "LIVE Third-party")
                        .put("label_vi", "Game LIVE"));
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("total", data.length());
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ListGSCProvidersProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.gsc;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9897 — GitLab #19.
 *
 * Admin CMS toggle for {@code vinplay.gsc_game_catalog.active}. Replaces
 * the "hide game X" workflow that used to be: dev greps DB for game_code,
 * writes a one-off SQL, PRs it, applies on staging then prod, restarts
 * portal-api. Now: one call, audit-logged, no eng hours per CR.
 *
 * <p>Params:
 * <ul>
 *   <li>{@code aat} — admin access token (Hazelcast {@code cacheToken})</li>
 *   <li>{@code product_code} — numeric GSC provider id (e.g. 1002 Evolution)</li>
 *   <li>{@code game_code} — GSC game code (exact match)</li>
 *   <li>{@code active} — 0|1 (also accepts true|false)</li>
 * </ul>
 *
 * <p>Portal lobby cache is in-process guava (5-min TTL) and we can't evict
 * it cross-container from here. The flip takes effect for all clients
 * within 5 minutes without any operator action. For immediate effect
 * (QA CR smoke), restart portal-api: {@code docker restart sunwinkr-portal-api}.
 */
public class SetGSCGameAvailableProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            String pcStr = request.getParameter("product_code");
            if (pcStr == null || pcStr.isEmpty()) return err(response, "4001", "product_code required");
            int productCode;
            try { productCode = Integer.parseInt(pcStr); }
            catch (NumberFormatException e) { return err(response, "4002", "product_code must be numeric"); }

            String gameCode = request.getParameter("game_code");
            if (gameCode == null || gameCode.isEmpty()) return err(response, "4001", "game_code required");

            String activeStr = request.getParameter("active");
            if (activeStr == null || activeStr.isEmpty()) return err(response, "4001", "active required (0|1)");
            int active;
            if ("1".equals(activeStr) || "true".equalsIgnoreCase(activeStr)) active = 1;
            else if ("0".equals(activeStr) || "false".equalsIgnoreCase(activeStr)) active = 0;
            else return err(response, "4002", "active must be 0|1 or true|false");

            // Read current value so the audit log records the before/after.
            Integer oldActive = null;
            String gameName = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT active, game_name FROM gsc_game_catalog WHERE product_code=? AND game_code=? LIMIT 1")) {
                ps.setInt(1, productCode);
                ps.setString(2, gameCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        oldActive = rs.getInt(1);
                        gameName = rs.getString(2);
                    }
                }
            }
            if (oldActive == null) return err(response, "1002", "game not found: product_code=" + productCode + " game_code=" + gameCode);

            // No-op short-circuit — still emit an audit entry so ops can see
            // the admin hit the button, but return rows_affected=0 so the UI
            // knows nothing actually changed.
            int rowsAffected = 0;
            if (oldActive != active) {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE gsc_game_catalog SET active=? WHERE product_code=? AND game_code=?")) {
                    ps.setInt(1, active);
                    ps.setInt(2, productCode);
                    ps.setString(3, gameCode);
                    rowsAffected = ps.executeUpdate();
                }
            }

            // Audit: use vinplay_admin.log_admin (existing admin action log).
            // action encodes the operation, reason holds the context, status
            // holds before→after for quick grep.
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO log_admin (action, username, reason, status, account_name) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "gsc.game.toggle");
                ps.setString(2, adminNick);
                ps.setString(3, "product_code=" + productCode + " game_code=" + gameCode
                        + " game_name=" + (gameName != null ? gameName : "")
                        + " rows_affected=" + rowsAffected);
                ps.setString(4, oldActive + "->" + active);
                ps.setString(5, gameCode);
                ps.executeUpdate();
            } catch (Exception auditErr) {
                // Audit failure must not mask the real work — log and continue.
                logger.warn("SetGSCGameAvailable audit insert failed for "
                        + adminNick + " " + productCode + "/" + gameCode + ": "
                        + auditErr.getMessage());
            }

            JSONObject data = new JSONObject();
            data.put("product_code", productCode);
            data.put("game_code", gameCode);
            data.put("game_name", gameName != null ? gameName : "");
            data.put("old_active", oldActive.intValue());
            data.put("new_active", active);
            data.put("rows_affected", rowsAffected);
            data.put("cache_ttl_seconds", 300);
            data.put("cache_note", "Portal lobby cache is in-process; change propagates within 5 minutes. "
                    + "For immediate effect: docker restart sunwinkr-portal-api");

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("SetGSCGameAvailableProcessor error", e);
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

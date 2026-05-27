package com.vinplay.api.backend.processors.gamecatalog;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.config.AwcApiClient;
import com.vinplay.vbee.common.config.AwcConfig;
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

/**
 * c=9983 — Sync AWC platform list into the unified
 * {@code vinplay.games} catalog.
 *
 * <p>AWC only exposes a platform list ({@code POST /fetch/getPlatformListByAgent})
 * — there is no per-game endpoint. This processor seeds one catch-all
 * placeholder row per platform: {@code (provider='AWC', vendor_platform=X,
 * game_code='*', table_tag='', game_name='X (default)')}. Per-game and
 * per-table rows are added separately (manually by ops or by parsing
 * observed {@code log_awc_bets} round_id prefixes).
 *
 * <p>GSC catalog is owned by ops migrations (1399 rows seeded) and
 * intentionally not handled here.
 */
public class SyncGameCatalogProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            String platformParam = request.getParameter("platform");
            if (platformParam == null || platformParam.isEmpty()) return err(response, "4001", "platform required");
            platformParam = platformParam.toLowerCase();
            if (!"awc".equals(platformParam)) {
                return err(response, "4002", "Only platform=awc supported. GSC catalog is seeded by ops.");
            }

            if (!AwcConfig.isEnabled()) return err(response, "5002", "AWC is disabled");

            JSONObject awcResp = AwcApiClient.getPlatformListByAgent();
            String status = awcResp.optString("status", "");
            if (!"0000".equals(status)) {
                return err(response, "5001", "AWC error: " + awcResp.optString("desc", status));
            }

            JSONArray platforms = awcResp.optJSONArray("platforms");
            if (platforms == null) {
                JSONObject data = new JSONObject();
                data.put("synced", 0);
                data.put("message", "AWC returned no platforms");
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
                return response.toString();
            }

            int synced = 0;
            JSONArray platformCodes = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO games (provider, vendor_platform, game_code, table_tag, game_name, category_id, is_active) "
                                 + "VALUES ('AWC', ?, '*', '', ?, 1, 1) "
                                 + "ON DUPLICATE KEY UPDATE updated_at = NOW()")) {
                for (int i = 0; i < platforms.length(); i++) {
                    Object item = platforms.get(i);
                    String platformCode;
                    if (item instanceof JSONObject) {
                        JSONObject po = (JSONObject) item;
                        platformCode = po.optString("platform", po.optString("code", ""));
                    } else {
                        platformCode = item.toString();
                    }
                    if (platformCode.isEmpty()) continue;
                    platformCodes.put(platformCode);
                    ps.setString(1, platformCode);
                    ps.setString(2, platformCode + " (default)");
                    ps.addBatch();
                    synced++;
                }
                if (synced > 0) ps.executeBatch();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO log_admin (action, username, reason, status, account_name) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "gamecatalog.sync");
                ps.setString(2, adminNick);
                ps.setString(3, "platform=awc synced=" + synced + " platforms=" + platformCodes);
                ps.setString(4, "ok");
                ps.setString(5, "awc");
                ps.executeUpdate();
            } catch (Exception auditErr) {
                logger.warn("SyncGameCatalog audit insert failed: " + auditErr.getMessage());
            }

            JSONObject data = new JSONObject();
            data.put("platform", "awc");
            data.put("synced", synced);
            data.put("platforms", platformCodes);
            data.put("note", "AWC does not expose a per-game list endpoint. "
                    + "Each platform seeded as game_code='*' placeholder in vinplay.games. "
                    + "Add per-game rows manually or via SQL migration.");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("SyncGameCatalogProcessor error", e);
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

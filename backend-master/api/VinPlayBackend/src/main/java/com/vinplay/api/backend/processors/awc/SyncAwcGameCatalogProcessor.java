package com.vinplay.api.backend.processors.awc;

import com.vinplay.vbee.common.config.AwcApiClient;
import com.vinplay.vbee.common.config.AwcConfig;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * c=9896 — Admin endpoint: sync AWC platform list into awc_platform_map.
 * Calls AWC getPlatformListByAgent and upserts results.
 */
public class SyncAwcGameCatalogProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            if (!AwcConfig.isEnabled()) {
                response.put("success", false);
                response.put("errorCode", "5002");
                response.put("message", "AWC is disabled");
                return response.toString();
            }

            JSONObject awcResp = AwcApiClient.getPlatformListByAgent();
            String status = awcResp.optString("status", "");
            if (!"0000".equals(status)) {
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "AWC error: " + awcResp.optString("desc", status));
                return response.toString();
            }

            JSONArray platforms = awcResp.optJSONArray("platforms");
            if (platforms == null) {
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("synced", 0);
                response.put("message", "No platforms returned");
                return response.toString();
            }

            int synced = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO awc_platform_map (platform, provider_name, game_type, category, is_active) " +
                         "VALUES (?, ?, '*', 'OTHER', 1) " +
                         "ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name), updated_at = NOW()")) {

                for (int i = 0; i < platforms.length(); i++) {
                    Object item = platforms.get(i);
                    String platformCode;
                    if (item instanceof JSONObject) {
                        platformCode = ((JSONObject) item).optString("platform", ((JSONObject) item).optString("code", ""));
                    } else {
                        platformCode = item.toString();
                    }
                    if (platformCode.isEmpty()) continue;

                    ps.setString(1, platformCode);
                    ps.setString(2, platformCode);
                    ps.addBatch();
                    synced++;
                }
                if (synced > 0) ps.executeBatch();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("synced", synced);

        } catch (Exception e) {
            logger.error("SyncAwcGameCatalogProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}

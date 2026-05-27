package com.vinplay.api.backend.processors.cashback;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.cashback.CashbackService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Admin API: Thêm hoặc cập nhật % hoàn cược cho một game trong một cashback program.
 * Command ID: 9817
 *
 * Params: at (required), config_id (required), game_code (required),
 *         game_name (required), rebate_percent (required, 0-100),
 *         is_active (optional, default 1)
 *
 * Nếu game_code đã tồn tại trong config → UPDATE, chưa tồn tại → INSERT.
 */
public class UpsertCashbackGameConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse params
            String configIdStr = request.getParameter("config_id");
            String gameCode = request.getParameter("game_code");
            String gameName = request.getParameter("game_name");
            String rebatePercentStr = request.getParameter("rebate_percent");
            String isActiveStr = request.getParameter("is_active");

            // Validate required fields
            if (configIdStr == null || configIdStr.isEmpty() ||
                    gameCode == null || gameCode.isEmpty() ||
                    gameName == null || gameName.isEmpty() ||
                    rebatePercentStr == null || rebatePercentStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Required: config_id, game_code, game_name, rebate_percent");
                return response.toString();
            }

            int configId = Integer.parseInt(configIdStr);
            double rebatePercent = Double.parseDouble(rebatePercentStr);
            boolean isActive = isActiveStr == null || "1".equals(isActiveStr) || "true".equalsIgnoreCase(isActiveStr);

            if (rebatePercent < 0 || rebatePercent > 100) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "rebate_percent must be between 0 and 100");
                return response.toString();
            }

            CashbackService service = new CashbackService();

            // Kiểm tra cashback program tồn tại
            if (service.getConfig(configId) == null) {
                response.put("success", false);
                response.put("errorCode", "4004");
                response.put("message", "Cashback config not found: " + configId);
                return response.toString();
            }

            boolean ok = service.upsertGameConfig(configId, gameCode.trim(), gameName.trim(),
                    rebatePercent, isActive);

            String adminUser = tokenMap.get(accessToken);
            if (ok) {
                service.logChange("GAME_CONFIG", configId, "UPSERT", gameCode,
                        null, "rebate=" + rebatePercent + "% active=" + isActive, adminUser);
                logger.info("UpsertCashbackGameConfig OK configId=" + configId +
                        " gameCode=" + gameCode + " rebate=" + rebatePercent + "% by=" + adminUser);
            }

            response.put("success", ok);
            if (!ok) {
                response.put("errorCode", "1001");
                response.put("message", "Upsert failed");
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("UpsertCashbackGameConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

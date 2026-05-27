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
 * Admin API: Xóa % hoàn cược theo game khỏi một cashback program.
 * Command ID: 9818
 *
 * Params: at (required), config_id (required), game_code (required)
 */
public class DeleteCashbackGameConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            String configIdStr = request.getParameter("config_id");
            String gameCode = request.getParameter("game_code");

            if (configIdStr == null || configIdStr.isEmpty() ||
                    gameCode == null || gameCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Required: config_id, game_code");
                return response.toString();
            }

            int configId = Integer.parseInt(configIdStr);
            CashbackService service = new CashbackService();

            boolean ok = service.deleteGameConfig(configId, gameCode.trim());

            String adminUser = tokenMap.get(accessToken);
            if (ok) {
                service.logChange("GAME_CONFIG", configId, "DELETE", gameCode,
                        gameCode, null, adminUser);
                logger.info("DeleteCashbackGameConfig OK configId=" + configId +
                        " gameCode=" + gameCode + " by=" + adminUser);
            }

            response.put("success", ok);
            if (!ok) {
                response.put("errorCode", "4004");
                response.put("message", "Game config not found or already deleted");
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid config_id format");
        } catch (Exception e) {
            logger.error("DeleteCashbackGameConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

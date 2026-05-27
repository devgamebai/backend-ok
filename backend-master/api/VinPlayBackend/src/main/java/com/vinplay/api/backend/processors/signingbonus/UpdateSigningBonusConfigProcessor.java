package com.vinplay.api.backend.processors.signingbonus;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.SigningBonusDao;
import com.vinplay.dal.dao.impl.SigningBonusDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Admin API: Update signing bonus configuration.
 * Command ID: 9762
 *
 * Params: at, config_id, bonus_amount, status, wager_enabled, wager_multiplier
 */
public class UpdateSigningBonusConfigProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
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

            // Parse parameters
            String configIdStr = request.getParameter("config_id");
            String bonusAmountStr = request.getParameter("bonus_amount");
            String statusStr = request.getParameter("status");
            String wagerEnabledStr = request.getParameter("wager_enabled");
            String wagerMultiplierStr = request.getParameter("wager_multiplier");

            if (configIdStr == null || bonusAmountStr == null || statusStr == null
                    || wagerEnabledStr == null || wagerMultiplierStr == null) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "config_id, bonus_amount, status, wager_enabled, and wager_multiplier are required");
                return response.toString();
            }

            int configId = Integer.parseInt(configIdStr);
            long bonusAmount = Long.parseLong(bonusAmountStr);
            int status = Integer.parseInt(statusStr);
            int wagerEnabled = Integer.parseInt(wagerEnabledStr);
            double wagerMultiplier = Double.parseDouble(wagerMultiplierStr);

            if (bonusAmount < 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "bonus_amount must be >= 0");
                return response.toString();
            }
            if (status != 0 && status != 1) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "status must be 0 or 1");
                return response.toString();
            }
            if (wagerEnabled != 0 && wagerEnabled != 1) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "wager_enabled must be 0 or 1");
                return response.toString();
            }
            if (wagerMultiplier < 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "wager_multiplier must be >= 0");
                return response.toString();
            }

            // Get admin username from token
            String adminUser = tokenMap.get(accessToken);

            SigningBonusDao dao = new SigningBonusDaoImpl();
            boolean updated = dao.updateConfig(configId, bonusAmount, status, wagerEnabled, wagerMultiplier, adminUser);

            if (updated) {
                response.put("success", true);
                logger.info("SigningBonus config updated: configId=" + configId +
                        " bonusAmount=" + bonusAmount + " status=" + status +
                        " wagerEnabled=" + wagerEnabled + " wagerMultiplier=" + wagerMultiplier +
                        " by=" + adminUser);
            } else {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Config not found or not updated");
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("UpdateSigningBonusConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

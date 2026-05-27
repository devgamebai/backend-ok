package com.vinplay.api.backend.processors.signingbonus;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.SigningBonusDao;
import com.vinplay.dal.dao.impl.SigningBonusDaoImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Admin API: Toggle signing bonus config for a specific user.
 * Command ID: 9765
 *
 * Admin can:
 *   - Enable/disable signing bonus for a specific user
 *   - Set payout mode: 'auto' or 'manual'
 *   - Provide reason for the change
 *
 * Params:
 *   at           - Admin access token (required)
 *   nick_name    - Target user's nickname (required)
 *   enabled      - 1 = eligible, 0 = blocked (required)
 *   payout_mode  - 'auto' or 'manual' (optional, default: 'auto')
 *   reason       - Reason for the change (optional)
 *
 * Response includes current user config after update.
 */
public class ToggleUserSigningBonusProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Access token required");
                return response.toString();
            }
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Invalid access token");
                return response.toString();
            }

            // Get admin username from token
            String adminUser = tokenMap.get(accessToken);

            // Parse parameters
            String nickName = request.getParameter("nick_name");
            String enabledStr = request.getParameter("enabled");
            String payoutMode = request.getParameter("payout_mode");
            String reason = request.getParameter("reason");

            if (nickName == null || nickName.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "nick_name is required");
                return response.toString();
            }

            if (enabledStr == null || enabledStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "enabled is required (0 or 1)");
                return response.toString();
            }

            int enabled;
            try {
                enabled = Integer.parseInt(enabledStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "enabled must be 0 or 1");
                return response.toString();
            }

            if (enabled != 0 && enabled != 1) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "enabled must be 0 or 1");
                return response.toString();
            }

            // Validate payout_mode
            if (payoutMode != null && !payoutMode.equals("auto") && !payoutMode.equals("manual")) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "payout_mode must be 'auto' or 'manual'");
                return response.toString();
            }
            if (payoutMode == null) {
                payoutMode = "auto";
            }

            // Get user ID from nickname
            UserDaoImpl userDao = new UserDaoImpl();
            int userId = userDao.getIdByNickname(nickName.trim());
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "2001");
                response.put("message", "User not found: " + nickName);
                return response.toString();
            }

            // Upsert user config
            SigningBonusDao dao = new SigningBonusDaoImpl();
            boolean success = dao.upsertUserConfig(userId, nickName.trim(), enabled,
                    payoutMode, reason, adminUser);

            if (success) {
                // Return updated config
                Map<String, Object> updatedConfig = dao.getUserConfig(userId);

                response.put("success", true);
                if (updatedConfig != null) {
                    response.put("data", new JSONObject(updatedConfig));
                }
                response.put("message", "User signing bonus config updated for " + nickName);

                logger.info("ToggleUserSigningBonus: user=" + nickName +
                        " enabled=" + enabled + " mode=" + payoutMode +
                        " reason=" + reason + " by=" + adminUser);
            } else {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Failed to update user config");
            }

        } catch (Exception e) {
            logger.error("ToggleUserSigningBonusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.signingbonus;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.SigningBonusService;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Admin API: Manual payout signing bonus for a specific user.
 * Command ID: 9764
 *
 * Used when:
 * - User has suspicious flag (nghi ngờ) = ON
 * - IP conflict or system detects potential fraud (multi-account, reinstall app)
 * - Admin verifies user is legitimate and approves manual payout
 *
 * Params:
 *   at           - Admin access token (required)
 *   nick_name    - Target user's nickname (required)
 *   bonus_amount - Custom bonus amount in won (required, must be > 0)
 *   reason       - Reason for manual payout (optional)
 *
 * Anti-duplicate:
 *   - Checks tbl_signing_bonus_log UNIQUE KEY on user_id
 *   - INSERT IGNORE prevents race condition duplicates
 *   - Returns error if user already received bonus (auto or manual)
 *
 * Transaction record:
 *   - Uses updateMoneyFromAdmin → logs to system transaction history
 *   - actionName = "SigningBonus" (consistent with auto payout)
 *   - description contains bonus amount + manual payout context
 */
public class ManualPayoutSigningBonusProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            String bonusAmountStr = request.getParameter("bonus_amount");
            String reason = request.getParameter("reason");

            if (nickName == null || nickName.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "nick_name is required");
                return response.toString();
            }

            if (bonusAmountStr == null || bonusAmountStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "bonus_amount is required");
                return response.toString();
            }

            long bonusAmount;
            try {
                bonusAmount = Long.parseLong(bonusAmountStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "bonus_amount must be a valid number");
                return response.toString();
            }

            if (bonusAmount <= 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "bonus_amount must be > 0");
                return response.toString();
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

            // Attempt manual payout (checks duplicate internally)
            SigningBonusService bonusService = new SigningBonusService();
            long logId = bonusService.manualPayout(userId, nickName.trim(), bonusAmount,
                    adminUser, reason != null ? reason : "Manual payout by admin");

            if (logId <= 0) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "User already received signing bonus (duplicate prevented)");
                return response.toString();
            }

            // Credit bonus to user's wallet via MoneyGateway
            String description = "Signing bonus payout " + bonusAmount + " won by admin " + adminUser;
            if (reason != null && !reason.trim().isEmpty()) {
                description += " | Reason: " + reason.trim();
            }

            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    userId, nickName.trim(), bonusAmount, "PROMO_BONUS", null, description);

            if (cr.success) {
                long requiredVolume = bonusService.applyWageringRequirement(userId, nickName.trim(), bonusAmount);
                response.put("success", true);
                response.put("log_id", logId);
                response.put("required_wager_volume", requiredVolume);
                response.put("message", "Manual signing bonus " + bonusAmount +
                        " credited to " + nickName);
                logger.info("ManualPayoutSigningBonus: SUCCESS user=" + nickName +
                        " amount=" + bonusAmount + " by=" + adminUser +
                        " logId=" + logId + " requiredVolumeAdded=" + requiredVolume);
            } else {
                // Wallet credit failed → ROLLBACK: delete bonus log so admin can retry
                // Without this, the UNIQUE KEY would permanently block the user
                try {
                    com.vinplay.dal.dao.impl.SigningBonusDaoImpl rollbackDao =
                            new com.vinplay.dal.dao.impl.SigningBonusDaoImpl();
                    rollbackDao.deleteBonusLog(logId);
                    logger.warn("ManualPayoutSigningBonus: ROLLED BACK logId=" + logId +
                            " because wallet credit failed for user=" + nickName);
                } catch (Exception rollbackEx) {
                    logger.error("ManualPayoutSigningBonus: ROLLBACK FAILED logId=" + logId +
                            " user=" + nickName + " — MANUAL FIX REQUIRED", rollbackEx);
                }

                logger.error("ManualPayoutSigningBonus: WALLET CREDIT FAILED user=" + nickName +
                        " amount=" + bonusAmount + " by=" + adminUser +
                        " error=" + cr.error);
                response.put("success", false);
                response.put("errorCode", cr.error);
                response.put("message", "Wallet credit failed. Bonus log rolled back. Please retry.");
            }

        } catch (Exception e) {
            logger.error("ManualPayoutSigningBonusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }
}

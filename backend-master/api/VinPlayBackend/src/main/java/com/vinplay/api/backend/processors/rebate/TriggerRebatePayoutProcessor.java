package com.vinplay.api.backend.processors.rebate;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.dal.rebate.TelegramRebateNotifier;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * c=9753 — Admin: trigger payout for a single rebate log.
 * Params: log_id (required), admin_nickname (required)
 * Flow: validate → check volume rule → credit balance → record payout → mark paid
 */
public class TriggerRebatePayoutProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            long logId = RebateProcessorHelper.longParam(request, "log_id", -1);
            String adminNickname = RebateProcessorHelper.strParam(request, "admin_nickname");

            if (logId <= 0) {
                response.put("success", false);
                response.put("message", "log_id is required");
                return response.toString();
            }
            if (adminNickname == null) {
                adminNickname = "admin";
            }

            // Get log details
            Map<String, Object> log = RebateService.getLog(logId);
            if (log == null) {
                response.put("success", false);
                response.put("message", "Rebate log not found");
                return response.toString();
            }

            String status = (String) log.get("status");
            if (!"PENDING".equals(status)) {
                response.put("success", false);
                response.put("message", "Log is not PENDING, current status: " + status);
                return response.toString();
            }

            // SUN-764: SELF rebate must be claimed by the player via the portal
            // Claim endpoint (c=3083). Admins cannot force-credit it from here —
            // the money would land in the wrong wallet logic and bypass the rolling
            // 7-day expiry contract.
            String rebateType = log.get("rebate_type") != null ? (String) log.get("rebate_type") : "DOWNLINE";
            if ("SELF".equals(rebateType)) {
                response.put("success", false);
                response.put("message", "SELF rebate must be claimed by the player (c=3083), not admin-triggered");
                return response.toString();
            }

            String agentNickname = (String) log.get("agent_nickname");
            long payoutAmount = ((Number) log.get("net_rebate")).longValue();

            if (payoutAmount <= 0) {
                response.put("success", false);
                response.put("message", "Payout amount is 0 or negative");
                return response.toString();
            }

            // Claim this log first to prevent double payout race (single vs batch).
            boolean claimed = RebateService.claimForPayout(logId, adminNickname);
            if (!claimed) {
                response.put("success", false);
                response.put("message", "Log is being processed or no longer pending");
                return response.toString();
            }

            // SUN-764: only DOWNLINE rebates reach this point (SELF is rejected above).
            //          Credit straight to the agency wallet.
            int agentUserId = ((Number) log.get("agent_user_id")).intValue();
            long balanceBefore = RebateService.getAgencyWalletBalance(agentUserId);
            boolean creditSuccess = RebateService.creditAgencyWallet(agentUserId, payoutAmount);

            if (!creditSuccess) {
                RebateService.revertPayoutClaim(logId);
                logger.error("TriggerRebatePayoutProcessor credit failed agentNickname=" + agentNickname
                        + " type=" + rebateType + " amount=" + payoutAmount);
                response.put("success", false);
                response.put("message", "Credit balance failed (" + rebateType + ")");
                return response.toString();
            }

            long balanceAfter = balanceBefore + payoutAmount;

            // Mark as paid
            boolean marked = RebateService.markPaid(logId, adminNickname);
            if (!marked) {
                // Rollback: revert claim AND debit the wallet back
                logger.error("CRITICAL: markPaid failed, rolling back credit. logId=" + logId
                        + " agentNickname=" + agentNickname + " amount=" + payoutAmount);
                RebateService.revertPayoutClaim(logId);
                RebateService.debitAgencyWallet(agentUserId, payoutAmount);
                response.put("success", false);
                response.put("message", "Payout status update failed — rolled back");
                return response.toString();
            }

            // Record payout with real balance info
            RebateService.createPayout(logId, agentUserId, agentNickname,
                    payoutAmount, balanceBefore, balanceAfter, adminNickname, "Single payout by " + adminNickname);

            // Telegram notification
            try {
                TelegramRebateNotifier.getInstance().notifyPayout(agentNickname, payoutAmount, adminNickname);
            } catch (Exception tgErr) {
                logger.warn("TriggerRebatePayoutProcessor Telegram failed", tgErr);
            }

            logger.info("TriggerRebatePayoutProcessor SUCCESS logId=" + logId + " agent=" + agentNickname
                    + " amount=" + payoutAmount + " by=" + adminNickname);

            response.put("success", true);
            response.put("payout_amount", payoutAmount);
            response.put("agent_nickname", agentNickname);
        } catch (Exception e) {
            logger.error("TriggerRebatePayoutProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

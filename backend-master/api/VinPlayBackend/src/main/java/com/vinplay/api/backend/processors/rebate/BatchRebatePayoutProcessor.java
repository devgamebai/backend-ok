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
import java.util.List;
import java.util.Map;

/**
 * c=9754 — Admin: batch payout all PENDING rebate logs.
 * Params: admin_nickname (required)
 */
public class BatchRebatePayoutProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminNickname = RebateProcessorHelper.strParam(request, "admin_nickname");
            if (adminNickname == null) adminNickname = "admin";

            List<Map<String, Object>> pendingLogs = RebateService.getPendingLogs();
            if (pendingLogs.isEmpty()) {
                response.put("success", true);
                response.put("message", "No pending rebate logs");
                response.put("paid_count", 0);
                return response.toString();
            }

            UserServiceImpl userService = new UserServiceImpl();
            int paidCount = 0;
            long totalPaid = 0;
            int failCount = 0;
            int skippedClaimCount = 0;
            int skippedSelfCount = 0;

            for (Map<String, Object> log : pendingLogs) {
                long logId = ((Number) log.get("id")).longValue();
                String agentNickname = (String) log.get("agent_nickname");
                int agentUserId = ((Number) log.get("agent_user_id")).intValue();
                long payoutAmount = ((Number) log.get("net_rebate")).longValue();

                // SUN-764: SELF rebate belongs to the player Claim flow (c=3083) and
                // must be credited to users.vin, NOT to agency_wallet. Skip here so
                // an admin batch sweep can never drain player PENDING into the wrong wallet.
                String rebateType = log.get("rebate_type") != null ? (String) log.get("rebate_type") : "DOWNLINE";
                if ("SELF".equals(rebateType)) {
                    skippedSelfCount++;
                    continue;
                }

                if (payoutAmount <= 0) continue;

                try {
                    // Claim first to avoid duplicate payout race with manual trigger.
                    boolean claimed = RebateService.claimForPayout(logId, adminNickname);
                    if (!claimed) {
                        skippedClaimCount++;
                        logger.warn("BatchPayout SKIP claim failed logId=" + logId + " (already processing/paid)");
                        continue;
                    }

                    boolean creditSuccess = RebateService.creditAgencyWallet(agentUserId, payoutAmount);

                    if (creditSuccess) {
                        RebateService.markPaid(logId, adminNickname);
                        RebateService.createPayout(logId, agentUserId, agentNickname,
                                payoutAmount, 0, 0, adminNickname, "Batch payout");
                        paidCount++;
                        totalPaid += payoutAmount;
                        logger.info("BatchPayout OK logId=" + logId + " agent=" + agentNickname + " amount=" + payoutAmount);
                    } else {
                        RebateService.revertPayoutClaim(logId);
                        failCount++;
                        logger.error("BatchPayout credit FAILED logId=" + logId + " agent=" + agentNickname);
                    }
                } catch (Exception itemErr) {
                    RebateService.revertPayoutClaim(logId);
                    failCount++;
                    logger.error("BatchPayout error logId=" + logId, itemErr);
                }
            }

            // Telegram notification
            try {
                if (paidCount > 0) {
                    TelegramRebateNotifier.getInstance().notifyBatchPayout(paidCount, totalPaid, adminNickname);
                }
            } catch (Exception tgErr) {
                logger.warn("BatchRebatePayoutProcessor Telegram failed", tgErr);
            }

            response.put("success", true);
            response.put("paid_count", paidCount);
            response.put("fail_count", failCount);
            response.put("skipped_claim_count", skippedClaimCount);
            response.put("skipped_self_count", skippedSelfCount);
            response.put("total_paid", totalPaid);
        } catch (Exception e) {
            logger.error("BatchRebatePayoutProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.withdraw.BankWithdrawalApprovalService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9643 — Admin approve a pending bank withdrawal.
 *
 * <p>Thin processor: token validation + parameter parsing here, all
 * data work in {@link BankWithdrawalApprovalService}. Mirrors how
 * c=9631 Approve calls into {@code CryptoWithdrawalApprovalService}.
 */
public class ApproveBankWithdrawalProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

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
            String adminNickname = tokenMap.get(accessToken);

            String txIdStr = request.getParameter("tx_id");
            if (txIdStr == null || txIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "tx_id is required");
                return response.toString();
            }
            long txId = Long.parseLong(txIdStr);

            BankWithdrawalApprovalService.Result result =
                    BankWithdrawalApprovalService.approve(txId, adminNickname);

            if (result.success) {
                // SUN-1220 — keep the Telegram message in sync when CMS
                // is the trigger. Best-effort; never breaks the CMS
                // response if the bot is offline.
                try {
                    BankWithdrawalApprovalService.WithdrawalRow row = result.row;
                    if (row != null && row.telegramMessageId > 0) {
                        new com.vinplay.dal.withdraw.TelegramBankWithdrawNotifier()
                                .editMessageApproved(row.telegramMessageId, row.txCode,
                                        row.amountKrw, row.nickName, row.bankName,
                                        row.bankNumber, row.customerName, adminNickname);
                    }
                } catch (Throwable tgErr) {
                    logger.warn("ApproveBankWithdrawalProcessor Telegram edit failed txId="
                            + txId, tgErr);
                }
                response.put("success", true);
            } else {
                response.put("success", false);
                response.put("errorCode", mapErrorCode(result.errorCode));
                response.put("message", result.errorMessage);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("ApproveBankWithdrawalProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    private static String mapErrorCode(String serviceCode) {
        if ("WITHDRAWAL_NOT_FOUND".equals(serviceCode)) return "4004";
        if ("NOT_PENDING".equals(serviceCode)) return "4005";
        return "1001";
    }
}

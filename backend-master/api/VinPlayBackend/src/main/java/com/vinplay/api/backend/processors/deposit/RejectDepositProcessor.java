package com.vinplay.api.backend.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.deposit.DepositApprovalService;
import com.vinplay.dal.deposit.DepositApprovalService.Channel;
import com.vinplay.dal.deposit.DepositApprovalService.Result;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9544 — Admin reject a pending deposit transaction.
 *
 * <p>Thin dispatcher: validates the admin's session token, then
 * delegates the actual reject work to {@link DepositApprovalService}.
 * Shared with the Telegram bot's reject callback.
 */
public class RejectDepositProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        long txId = -1;
        String adminNickname = null;
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
            adminNickname = tokenMap.get(accessToken);

            String txIdStr = request.getParameter("tx_id");
            if (txIdStr == null || txIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "tx_id is required");
                return response.toString();
            }
            txId = Long.parseLong(txIdStr);

            String reason = request.getParameter("reason");
            if (reason == null) reason = "";

            Result result = DepositApprovalService.reject(txId, adminNickname, reason, Channel.CMS);

            if (!result.success) {
                response.put("success", false);
                response.put("errorCode", mapErrorCode(result.errorCode));
                response.put("message", result.errorMessage);
                if ("LOCKED_BY_OTHER".equals(result.errorCode) && result.row != null) {
                    response.put("locked_by", result.row.operatorName);
                }
                return response.toString();
            }

            response.put("success", true);

            // Sync to Telegram — remove buttons, show rejected status.
            try {
                if (result.row != null && result.row.telegramMsgId > 0) {
                    com.vinplay.dal.deposit.TelegramDepositNotifier.getInstance()
                            .editMessageRejected(
                                    result.row.telegramMsgId,
                                    result.row.txCode,
                                    result.row.amount,
                                    result.row.nickName,
                                    adminNickname,
                                    reason);
                }
            } catch (Exception tgErr) {
                logger.warn("RejectDepositProcessor Telegram sync failed txId=" + txId, tgErr);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("RejectDepositProcessor error txId=" + txId, e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    private static String mapErrorCode(String serviceCode) {
        if (serviceCode == null) return "1001";
        switch (serviceCode) {
            case "TX_NOT_FOUND":     return "5020";
            case "TERMINAL_STATUS":  return "5020";
            case "LOCKED_BY_OTHER":  return "5002";
            case "CANNOT_PICK":      return "5011";
            case "CANNOT_REJECT":    return "5020";
            default:                 return "1001";
        }
    }
}

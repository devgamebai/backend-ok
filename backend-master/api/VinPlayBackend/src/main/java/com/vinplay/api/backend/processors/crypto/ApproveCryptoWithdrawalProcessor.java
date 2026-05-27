package com.vinplay.api.backend.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.crypto.CryptoWithdrawalApprovalService;
import com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.Result;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9631 — Admin approve a pending crypto withdrawal.
 *
 * <p>Thin dispatcher: validates the admin's session token, then delegates
 * the actual approve work to {@link CryptoWithdrawalApprovalService}, which
 * is the single source of truth shared with the Telegram bot's approve
 * callback. See that class for the full state-machine contract.
 */
public class ApproveCryptoWithdrawalProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Resolve admin nickname from session token (at / aat).
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

            // Parse tx_id.
            String txIdStr = request.getParameter("tx_id");
            if (txIdStr == null || txIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "tx_id is required");
                return response.toString();
            }
            long txId = Long.parseLong(txIdStr);

            // Delegate.
            Result result = CryptoWithdrawalApprovalService.approve(txId, adminNickname);

            if (!result.success) {
                response.put("success", false);
                response.put("errorCode", mapErrorCode(result.errorCode));
                response.put("message", result.errorMessage);
                return response.toString();
            }

            // Best-effort Telegram sync. The bot's own approve callback
            // already updates the message it owns, so this is for the case
            // where ops approved via the web admin while the message is
            // still showing PENDING in Telegram. Currently a no-op because
            // the schema does not store telegram_message_id; left in for
            // when that column is added back.
            // (See CryptoWithdrawalApprovalService for the rationale.)

            response.put("success", true);

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("ApproveCryptoWithdrawalProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    /** Map service-level error codes to the wire format the FE/admin expects. */
    private static String mapErrorCode(String serviceCode) {
        if (serviceCode == null) return "1001";
        switch (serviceCode) {
            case "WITHDRAWAL_NOT_FOUND": return "4004";
            case "NOT_PENDING":          return "4005";
            case "GATEWAY_ERROR":        return "5001";
            default:                     return "1001";
        }
    }
}

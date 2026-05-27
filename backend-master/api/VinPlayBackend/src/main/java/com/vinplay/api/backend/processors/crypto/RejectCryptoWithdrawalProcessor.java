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
 * c=9632 — Admin reject a pending crypto withdrawal (refunds balance).
 *
 * <p>Thin dispatcher: validates the admin's session token, then delegates
 * the actual reject work to {@link CryptoWithdrawalApprovalService} so the
 * bot and the admin web panel cannot drift. See that class for the full
 * contract (gateway-cancel best-effort, status flip, MoneyGateway refund).
 */
public class RejectCryptoWithdrawalProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            String reason = request.getParameter("reason");
            if (txIdStr == null || txIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "tx_id is required");
                return response.toString();
            }
            long txId = Long.parseLong(txIdStr);

            Result result = CryptoWithdrawalApprovalService.reject(txId, adminNickname, reason);

            if (!result.success) {
                response.put("success", false);
                response.put("errorCode", mapErrorCode(result.errorCode));
                response.put("message", result.errorMessage);
                return response.toString();
            }

            response.put("success", true);

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("RejectCryptoWithdrawalProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

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

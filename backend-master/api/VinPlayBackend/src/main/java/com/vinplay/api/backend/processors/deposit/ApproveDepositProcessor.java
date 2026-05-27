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
 * c=9543 — Admin approve a pending deposit transaction.
 *
 * <p>Thin dispatcher: validates the admin's session token, then
 * delegates the actual approve work to {@link DepositApprovalService}.
 * Same service is called by the Telegram bot's approve callback so
 * the two paths can't drift (SUN-1197).
 */
public class ApproveDepositProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            Result result = DepositApprovalService.approve(txId, adminNickname, Channel.CMS);

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

            // Sync to Telegram — remove buttons, show approved status with
            // bank info preserved (User request 2026-04-22 — so operators
            // can still match against the bank statement after approval).
            // The service has already done the data work; this is purely a
            // channel-side rendering concern.
            try {
                if (result.row != null && result.row.telegramMsgId > 0) {
                    String holderName = lookupHolderName(result.row.userId, result.row.bankNumber);
                    com.vinplay.dal.deposit.TelegramDepositNotifier.getInstance()
                            .editMessageApproved(
                                    result.row.telegramMsgId,
                                    result.row.txCode,
                                    result.row.amount,
                                    result.row.nickName,
                                    adminNickname,
                                    result.row.bankName,
                                    result.row.bankNumber,
                                    holderName);
                }
            } catch (Exception tgErr) {
                logger.warn("ApproveDepositProcessor Telegram sync failed txId=" + txId, tgErr);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("ApproveDepositProcessor error txId=" + txId, e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    /** Map service-level error codes to the wire format the FE/admin expects. */
    private static String mapErrorCode(String serviceCode) {
        if (serviceCode == null) return "1001";
        switch (serviceCode) {
            case "TX_NOT_FOUND":     return "5010";
            case "TERMINAL_STATUS":  return "5020";
            case "LOCKED_BY_OTHER":  return "5002";
            case "CANNOT_PICK":      return "5011";
            case "CANNOT_APPROVE":   return "5010";
            default:                 return "1001";
        }
    }

    /** Look up the player's bank-account holder name from users_bank by
     *  (user_id, bank_number). Same heuristic used in the original
     *  notification builder; service does not own this because it's only
     *  needed for the Telegram message render. */
    private static String lookupHolderName(long userId, String bankNumber) {
        if (userId <= 0 || bankNumber == null || bankNumber.isEmpty()) return null;
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT customer_name FROM vinplay.users_bank "
                             + "WHERE user_id = ? AND bank_number = ? LIMIT 1")) {
            ps.setLong(1, userId);
            ps.setString(2, bankNumber);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            logger.warn("ApproveDepositProcessor holder lookup failed userId=" + userId, e);
        }
        return null;
    }
}

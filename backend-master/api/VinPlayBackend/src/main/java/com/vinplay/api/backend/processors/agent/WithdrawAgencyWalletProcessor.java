package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Convert agency_wallet → users.vin (agent's own game wallet).
 *
 * SUN-1099 (2026-04-27): per Mr.DEAL clarification, this is an INTERNAL
 * TRANSFER between two wallets owned by the same identity (agent), NOT a
 * deposit. Type tagged as {@code CONVERT_AGENCY_TO_VIN} so reporting can
 * exclude it from deposit KPI totals.
 *
 * Audit trail (was missing in previous implementation — bug fixed):
 *  - {@code agency_wallet_transactions} row via {@link RebateService#debitAgencyWallet}
 *    with type {@code CONVERT_TO_GAME}, direction DEBIT.
 *  - {@code log_money_user} row via {@link MoneyGateway#creditUser} with
 *    source {@code CONVERT_AGENCY_TO_VIN} → also publishes RMQ for FE
 *    real-time balance push.
 *
 * Refund: if vin credit fails after agency debit, the agency_wallet is
 * refunded via {@link RebateService#creditAgencyWallet} with a reversal
 * note so the audit trail is balanced.
 */
public class WithdrawAgencyWalletProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final String DEBIT_TYPE  = "CONVERT_TO_GAME";
    private static final String CREDIT_TYPE = "CONVERT_AGENCY_TO_VIN";

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");

        try {
            String nickName = request.getParameter("nn");
            String amtStr   = request.getParameter("amount");
            String note     = request.getParameter("nt");

            if (nickName == null || nickName.isEmpty() || amtStr == null || amtStr.isEmpty()) {
                return response.toJson();
            }
            long amount;
            try { amount = Long.parseLong(amtStr); }
            catch (NumberFormatException nfe) { response.setErrorCode("1002"); return response.toJson(); }
            if (amount <= 0) { response.setErrorCode("1002"); return response.toJson(); }

            // Resolve agent id from useragent.nickname
            int agentId = -1;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement stm = conn.prepareStatement(
                         "SELECT id FROM vinplay_admin.useragent WHERE nickname = ? LIMIT 1")) {
                stm.setString(1, nickName);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) agentId = rs.getInt("id");
                }
            }
            if (agentId == -1) {
                response.setErrorCode("1003"); // Not an agent
                return response.toJson();
            }

            // Resolve user id from nickname for MoneyGateway.creditUser
            long userId = -1;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement(
                         "SELECT id FROM users WHERE nick_name = ? LIMIT 1")) {
                stm.setString(1, nickName);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) userId = rs.getLong("id");
                }
            }
            if (userId <= 0) {
                response.setErrorCode("1003");
                return response.toJson();
            }

            String reason = note != null && !note.isEmpty()
                    ? note
                    : "Convert agency_wallet → vin for agent=" + nickName;

            // 1. Debit agency_wallet (writes agency_wallet_transactions audit row)
            boolean debited = RebateService.debitAgencyWallet(agentId, amount, DEBIT_TYPE, reason);
            if (!debited) {
                response.setErrorCode("1004"); // Insufficient agency balance
                return response.toJson();
            }

            // 2. Credit users.vin via canonical MoneyGateway path (writes
            //    log_money_user + publishes RMQ balance push to FE)
            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    userId, nickName, amount, CREDIT_TYPE, null, reason);

            if (cr.success) {
                JSONObject ok = new JSONObject();
                ok.put("success", true);
                ok.put("errorCode", "0");
                ok.put("withdrawnAmount", amount);
                ok.put("newBalance", cr.newBalance);
                return ok.toString();
            }

            // 3. Credit failed — refund agency_wallet so audit is balanced.
            //    Use creditAgencyWallet with a reversal note that points
            //    back to the failing convert attempt for traceability.
            boolean refunded = RebateService.creditAgencyWallet(
                    agentId, amount, "CONVERT_TO_GAME_REVERSAL",
                    nickName, null,
                    "Refund — vin credit failed: " + cr.error);
            if (!refunded) {
                logger.error("WithdrawAgencyWallet: REFUND FAILED agentId=" + agentId
                        + " nick=" + nickName + " amount=" + amount
                        + " — agency_wallet drift detected, manual intervention required");
            }
            response.setErrorCode("1005"); // Failed to credit game wallet
            return response.toJson();

        } catch (Exception e) {
            logger.error("WithdrawAgencyWalletProcessor error", e);
            response.setErrorCode("1001");
            return response.toJson();
        }
    }
}

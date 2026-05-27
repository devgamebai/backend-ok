package com.vinplay.api.backend.processors.rebate;

import com.vinplay.dal.dao.AgencyWalletDao;
import com.vinplay.dal.dao.impl.AgencyWalletDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9759 — Revert a rebate log (spec §9 edge cases).
 *
 * Admin-triggered when:
 *   - Game result rolled back (invalid session, cheating detected)
 *   - Bet canceled after commission already logged
 *   - Provider callback arrives with delayed reversal
 *
 * Params:
 *   log_id = rebate_logs.id
 *   reason = free-text reason (stored in note)
 *
 * State transitions:
 *   PENDING  → REVERSED (no money movement, just mark record)
 *   APPROVED → REVERSED (same, no payout happened yet)
 *   PAID     → REVERSED + DEBIT agency_wallet (money already credited to agent, needs clawback)
 *   REJECTED → no-op (errorCode 4003 — already terminal)
 *   REVERSED → no-op (errorCode 4004 — idempotent)
 *
 * Response: { success, errorCode, logId, previousStatus, refundedAmount }
 */
public class RevertRebateLogProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("errorCode", "1001");

        try {
            HttpServletRequest request = param.get();
            String logIdStr = request.getParameter("log_id");
            String reason = request.getParameter("reason");

            if (logIdStr == null || logIdStr.isEmpty()) {
                response.put("errorCode", "4001");
                response.put("message", "log_id required");
                return response.toString();
            }
            long logId = Long.parseLong(logIdStr);
            if (reason == null) reason = "";

            // Load log
            int agentId = -1;
            long amount = 0;
            String prevStatus = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT agent_user_id, rebate_amount, status FROM vinplay.rebate_logs WHERE id = ?")) {
                ps.setLong(1, logId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentId = rs.getInt("agent_user_id");
                        // SUN-1150: rebate_amount is DECIMAL(20,2). Read as BigDecimal,
                        // then HALF_UP-round to long for wallet reversal (balance is integer KRW).
                        java.math.BigDecimal raw = rs.getBigDecimal("rebate_amount");
                        amount = raw == null ? 0L
                                : raw.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
                        prevStatus = rs.getString("status");
                    }
                }
            }
            if (prevStatus == null) {
                response.put("errorCode", "1002");
                response.put("message", "Log not found");
                return response.toString();
            }

            // Terminal states
            if ("REVERSED".equals(prevStatus)) {
                response.put("errorCode", "4004");
                response.put("message", "Already reversed");
                return response.toString();
            }
            if ("REJECTED".equals(prevStatus)) {
                response.put("errorCode", "4003");
                response.put("message", "Cannot revert — log already REJECTED (terminal)");
                return response.toString();
            }

            // Debit agency_wallet only if previously PAID
            long refunded = 0;
            if ("PAID".equals(prevStatus) && amount > 0) {
                AgencyWalletDao walletDao = new AgencyWalletDaoImpl();
                walletDao.addBalance(agentId, -amount);
                refunded = amount;
            }

            // Mark REVERSED
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE vinplay.rebate_logs SET status='REVERSED', note=CONCAT(COALESCE(note,''),' | REVERT: ',?) WHERE id = ?")) {
                ps.setString(1, reason);
                ps.setLong(2, logId);
                ps.executeUpdate();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("logId", logId);
            response.put("previousStatus", prevStatus);
            response.put("refundedAmount", refunded);
            response.put("message", "Log reversed");

            logger.info("RebateLog REVERTED id=" + logId + " prevStatus=" + prevStatus
                    + " agentId=" + agentId + " refunded=" + refunded + " reason=" + reason);

        } catch (Exception e) {
            logger.error("RevertRebateLogProcessor error", e);
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}

package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgencyWalletDao;
import com.vinplay.dal.dao.impl.AgencyWalletDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
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
 * c=9462 — Agent topup user from agency wallet.
 *
 * Flow: agent's agency_wallet.balance ──► user's vin balance
 *
 * Params:
 *   code = agent code (from session('info.code'))
 *   nn   = target user nickname (must be in agent's downline)
 *   am   = amount (VIN, positive integer)
 *   nt   = note (optional)
 *
 * Response: { success, errorCode, agentBalance, message }
 *
 * Validation:
 *   1. code/nn/am required
 *   2. Agent exists (useragent.code = code)
 *   3. User exists (users.nick_name = nn)
 *   4. User's referral_code matches agent.code (user belongs to this agent)
 *   5. Agent wallet balance >= amount
 *
 * Transaction (atomic best-effort):
 *   1. Debit agency_wallet
 *   2. Credit user vin via userService.updateMoneyFromAdmin()
 *   3. If user credit fails: refund agency_wallet
 */
public class TopupUser4AgentAdminProccessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("errorCode", "1001");

        try {
            HttpServletRequest request = param.get();

            // Path gating — same pattern as other /api_agent processors
            String serPath = request.getServletPath();
            if (serPath == null || !"/api_agent".equals(serPath)) {
                response.put("errorCode", "5");
                response.put("message", "Not allow access this api");
                return response.toString();
            }

            String agentCode = request.getParameter("code");
            String userNick = request.getParameter("nn");
            String amountStr = request.getParameter("am");
            String otpInput = request.getParameter("otp");
            String note = request.getParameter("nt");

            // Validate params
            if (agentCode == null || agentCode.isEmpty()) {
                response.put("errorCode", "4001");
                response.put("message", "Agent code required");
                return response.toString();
            }
            if (userNick == null || userNick.isEmpty()) {
                response.put("errorCode", "4002");
                response.put("message", "User nickname required");
                return response.toString();
            }
            if (amountStr == null || amountStr.isEmpty()) {
                response.put("errorCode", "4003");
                response.put("message", "Amount required");
                return response.toString();
            }

            long amount;
            try {
                amount = Long.parseLong(amountStr);
            } catch (NumberFormatException e) {
                response.put("errorCode", "4004");
                response.put("message", "Invalid amount");
                return response.toString();
            }
            if (amount <= 0) {
                response.put("errorCode", "4004");
                response.put("message", "Amount must be positive");
                return response.toString();
            }

            // Lookup agent id from useragent
            int agentId = -1;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement stm = conn.prepareStatement("SELECT id FROM vinplay_admin.useragent WHERE code = ? AND active = 1")) {
                stm.setString(1, agentCode);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) agentId = rs.getInt("id");
                }
            }
            if (agentId == -1) {
                response.put("errorCode", "1002");
                response.put("message", "Agent not found or inactive");
                return response.toString();
            }

            // Validate OTP (required) — consumed one-shot from cacheAgentOtp
            if (otpInput == null || otpInput.isEmpty()) {
                response.put("errorCode", "4006");
                response.put("message", "OTP required — call c=9464 first");
                return response.toString();
            }
            try {
                com.vinplay.vbee.common.cache.DistCache<String, String> otpMap =
                        com.vinplay.vbee.common.cache.CacheFactory.get("cacheAgentOtp", String.class);
                String cached = otpMap.get(agentCode);
                if (cached == null) {
                    response.put("errorCode", "4007");
                    response.put("message", "OTP expired or not sent");
                    return response.toString();
                }
                String cachedOtp = cached.split("\\|")[0];
                if (!cachedOtp.equals(otpInput)) {
                    response.put("errorCode", "4008");
                    response.put("message", "OTP incorrect");
                    return response.toString();
                }
                // One-shot consume
                otpMap.remove(agentCode);
            } catch (Exception otpEx) {
                logger.error("OTP validation error", otpEx);
                response.put("errorCode", "1006");
                response.put("message", "OTP validation failed");
                return response.toString();
            }

            // Verify user belongs to this agent (referral_code = agent.code)
            boolean userBelongsToAgent = false;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement("SELECT 1 FROM vinplay.users WHERE nick_name = ? AND referral_code = ? LIMIT 1")) {
                stm.setString(1, userNick);
                stm.setString(2, agentCode);
                try (ResultSet rs = stm.executeQuery()) {
                    userBelongsToAgent = rs.next();
                }
            }
            if (!userBelongsToAgent) {
                response.put("errorCode", "1003");
                response.put("message", "User does not belong to this agent");
                return response.toString();
            }

            // Check agent wallet balance
            AgencyWalletDao walletDao = new AgencyWalletDaoImpl();
            long agentBalance = walletDao.getBalance(agentId);
            if (agentBalance < amount) {
                response.put("errorCode", "4005");
                response.put("message", "Insufficient agent wallet balance");
                response.put("agentBalance", agentBalance);
                return response.toString();
            }

            // Debit agent wallet (atomic via SQL UPDATE with balance check)
            boolean debited = walletDao.addBalance(agentId, -amount);
            if (!debited) {
                response.put("errorCode", "1004");
                response.put("message", "Failed to debit agent wallet");
                return response.toString();
            }

            // Credit user's vin balance via MoneyGateway
            String noteMsg = (note != null && !note.isEmpty()) ? note : "Agent topup from " + agentCode;
            long gwUserId = 0;
            try (java.sql.Connection uc = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 java.sql.PreparedStatement ups = uc.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                ups.setString(1, userNick);
                try (java.sql.ResultSet urs = ups.executeQuery()) { if (urs.next()) gwUserId = urs.getLong("id"); }
            }
            com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                com.vinplay.dal.service.MoneyGateway.creditUser(
                    gwUserId, userNick, amount, "AGENT_TOPUP", null, noteMsg);

            if (!cr.success) {
                // Refund agency wallet (best effort)
                walletDao.addBalance(agentId, amount);
                response.put("errorCode", "1005");
                response.put("message", "Failed to credit user — refunded");
                return response.toString();
            }

            long newAgentBalance = walletDao.getBalance(agentId);
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", "Topup success");
            response.put("agentBalance", newAgentBalance);
            response.put("amount", amount);
            response.put("userNickname", userNick);

            logger.info("AgentTopup OK: agent=" + agentCode + " user=" + userNick + " amount=" + amount);

        } catch (Exception e) {
            logger.error("TopupUser4AgentAdminProccessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal error");
        }
        return response.toString();
    }
}

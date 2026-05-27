package com.vinplay.api.backend.processors.creditwallet;

import com.vinplay.dal.service.CreditWalletService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.vinplay.vbee.common.pools.ConnectionPool;

/**
 * c=9923 — Agent nạp Credit Wallet → Game Wallet (agent cùng nhánh HOẶC user downline).
 * Path: /api_agent
 * Params:
 *   code = agent code người gửi
 *   nn   = nickname người nhận (agent hoặc user)
 *   am   = amount (1 to 10,000,000)
 *   tt   = target type: "agent" hoặc "user"
 *   pwd  = withdrawal password
 *   nt   = note (optional)
 *
 * Effect: tính như khoản nạp thông thường — auto-approve, nhận KM (first-deposit / daily-deposit).
 *
 * Validations:
 *   tt=agent: target phải là agent cùng nhánh TĐL, không phải chính mình
 *   tt=user:  target phải là user downline (referral_code = sender.code)
 */
public class AgentCreditDepositProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String senderCode = request.getParameter("code");
            String targetNick = request.getParameter("nn");
            String amountStr = request.getParameter("am");
            String targetType = request.getParameter("tt");
            String pwdInput = request.getParameter("pwd");
            String note = request.getParameter("nt");

            // Required params
            if (isEmpty(senderCode)) return fail(response, "4001", "Sender code (code) required");
            if (isEmpty(targetNick)) return fail(response, "4001", "Target nickname (nn) required");
            if (isEmpty(amountStr)) return fail(response, "4001", "Amount (am) required");
            if (!"agent".equals(targetType) && !"user".equals(targetType)) {
                return fail(response, "4001", "Target type (tt) must be 'agent' or 'user'");
            }

            // Parse & validate amount
            long amount;
            try { amount = Long.parseLong(amountStr); }
            catch (NumberFormatException e) { return fail(response, "4002", "Invalid amount format"); }
            if (amount <= 0) return fail(response, "4002", "Amount must be positive");
            if (amount > CreditWalletService.MAX_TRANSFER_AMOUNT)
                return fail(response, "4002", "Amount exceeds 10,000,000 limit");

            // Resolve sender
            CreditWalletService.AgentInfo sender = CreditWalletService.getAgentInfoByCode(senderCode);
            if (sender == null) return fail(response, "1002", "Sender agent not found");
            if (sender.active != 1) return fail(response, "1009", "Sender account is frozen");

            // SUN-1099: SpecialAccount is read-only — block as actor.
            if (com.vinplay.dal.security.RoleGuard.isSpecialAccount(sender.nickname)) {
                return fail(response, com.vinplay.dal.security.RoleGuard.ERR_CODE_SPECIAL_ACCOUNT,
                        com.vinplay.dal.security.RoleGuard.ERR_MSG_SPECIAL_ACCOUNT);
            }
            // SUN-1099: SpecialAccount is read-only — block as recipient.
            if (com.vinplay.dal.security.RoleGuard.isSpecialAccount(targetNick)) {
                return fail(response, com.vinplay.dal.security.RoleGuard.ERR_CODE_SPECIAL_ACCOUNT,
                        com.vinplay.dal.security.RoleGuard.ERR_MSG_SPECIAL_ACCOUNT);
            }

            // Validate withdraw_password instead of OTP
            if (isEmpty(pwdInput)) return fail(response, "4006", "Withdraw password (pwd) required");

            String storedPwdHash = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement("SELECT withdraw_password FROM users WHERE nick_name = ?")) {
                ps.setString(1, sender.nickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) storedPwdHash = rs.getString("withdraw_password");
                }
            } catch (Exception ex) {
                logger.error("Error reading withdraw_password for agent " + sender.nickname, ex);
                return fail(response, "1001", "Internal error validating password");
            }

            if (isEmpty(storedPwdHash)) {
                return fail(response, "4004", "Bạn chưa cài đặt mật khẩu rút tiền");
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pwdInput.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));

            if (!storedPwdHash.equals(sb.toString())) {
                return fail(response, "4005", "Mật khẩu rút tiền không đúng");
            }

            // Resolve target + validate constraints
            long targetUserId;
            boolean isAgent = "agent".equals(targetType);

            if (isAgent) {
                CreditWalletService.AgentInfo targetAgent =
                        CreditWalletService.getAgentInfoByNick(targetNick);
                if (targetAgent == null) return fail(response, "4009", "Target agent not found");
                if (targetAgent.active != 1) return fail(response, "4009", "Target agent is frozen");
                if (targetAgent.id == sender.id)
                    return fail(response, "4011", "Cannot deposit to yourself");

                // Validate direct upline/downline relationship
                if (!CreditWalletService.isDirectUplineOrDownline(sender, targetAgent)) {
                    return fail(response, "4010", "Target agent must be a direct upline or downline");
                }

                // Lấy user_id của agent target qua username
                long[] userInfo = CreditWalletService.getUserIdAndVin(targetNick);
                if (userInfo == null) return fail(response, "1003", "Target agent has no user account");
                targetUserId = userInfo[0];

            } else {
                // target là user → phải là downline
                if (!CreditWalletService.isDownlineUser(sender.code, targetNick)) {
                    return fail(response, "1003", "User is not in this agent's downline");
                }
                long[] userInfo = CreditWalletService.getUserIdAndVin(targetNick);
                if (userInfo == null) return fail(response, "1003", "Target user not found");
                targetUserId = userInfo[0];
            }

            // Execute deposit
            CreditWalletService.CreditResult result =
                    CreditWalletService.depositToGameWallet(
                            sender.id, sender.nickname,
                            targetUserId, targetNick,
                            isAgent, amount, note);

            if (!result.success) return fail(response, result.errorCode, result.message);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("sender_credit_balance", result.senderBalance);
            response.put("amount", amount);
            response.put("target", targetNick);
            response.put("target_type", targetType);
            response.put("promo_applied", result.promoApplied);
            if (result.promoApplied) {
                response.put("bonus_amount", result.bonusAmount);
                response.put("promo_type", result.promoType != null ? result.promoType : "");
            }

        } catch (Exception e) {
            logger.error("AgentCreditDepositProcessor error", e);
            return fail(response, "1001", "Internal error");
        }
        return response.toString();
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private String fail(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}

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
 * c=9922 — Agent chuyển Credit Wallet → Credit Wallet (cùng nhánh TĐL).
 * Path: /api_agent
 * Params:
 *   code = agent code người gửi
 *   to   = nickname agent nhận
 *   am   = amount (1 to 10,000,000)
 *   pwd  = withdrawal password
 *   nt   = note (optional)
 *
 * Validations:
 *   1. Sender active=1, exists
 *   2. Receiver active=1, exists, là agent
 *   3. Sender ≠ Receiver
 *   4. Cùng nhánh TĐL
 *   5. Sender balance >= amount
 *   6. Amount <= 10M
 *   7. Withdraw password hợp lệ (như c=3056)
 */
public class AgentTransferCreditProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String senderCode = request.getParameter("code");
            String receiverNick = request.getParameter("to");
            String amountStr = request.getParameter("am");
            String pwdInput = request.getParameter("pwd");
            String note = request.getParameter("nt");

            // Required params
            if (isEmpty(senderCode)) return fail(response, "4001", "Sender code (code) required");
            if (isEmpty(receiverNick)) return fail(response, "4001", "Receiver nickname (to) required");
            if (isEmpty(amountStr)) return fail(response, "4001", "Amount (am) required");

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
            if (com.vinplay.dal.security.RoleGuard.isSpecialAccount(receiverNick)) {
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

            // Resolve receiver
            CreditWalletService.AgentInfo receiver = CreditWalletService.getAgentInfoByNick(receiverNick);
            if (receiver == null) return fail(response, "4009", "Receiver agent not found");
            if (receiver.active != 1) return fail(response, "4009", "Receiver agent is frozen");
            if (receiver.id == sender.id) return fail(response, "4011", "Cannot transfer to yourself");

            // Validate direct upline/downline relationship
            if (!CreditWalletService.isDirectUplineOrDownline(sender, receiver)) {
                return fail(response, "4010", "Receiver must be a direct upline or downline");
            }

            // Execute transfer
            CreditWalletService.CreditResult result =
                    CreditWalletService.transferToAgent(
                            sender.id, sender.nickname,
                            receiver.id, receiver.nickname,
                            amount, note);

            if (!result.success) return fail(response, result.errorCode, result.message);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("sender_balance", result.senderBalance);
            response.put("amount", amount);
            response.put("receiver", receiverNick);

        } catch (Exception e) {
            logger.error("AgentTransferCreditProcessor error", e);
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

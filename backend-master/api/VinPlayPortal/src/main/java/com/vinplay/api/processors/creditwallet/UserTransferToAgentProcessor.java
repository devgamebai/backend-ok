package com.vinplay.api.processors.creditwallet;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3056: User transfers money from their game wallet (vin) to parent agent's
 * Credit Wallet.
 *
 * Business rules:
 * - Target agent is ALWAYS the user's direct parent (referral_code in users table).
 *   FE cannot override this — prevents vượt cấp (cross-tier) transfers.
 * - Requires withdrawal password (SHA-256 hashed, same scheme as c=3031/3032).
 * - This is a pure debit from user game wallet. No deposit bonus is triggered.
 * - Atomic: only credits agent after user debit succeeds.
 *
 * Params:
 *   at  — User Access Token (required)
 *   am  — Amount to transfer, positive integer > 0 (required)
 *   pwd — Withdrawal password (plain text, will be SHA-256 hashed server-side)
 *
 * Success response:
 * {
 *   "success": true,
 *   "errorCode": "0",
 *   "current_balance": 150000
 * }
 *
 * Error codes:
 *   1001 — Unauthorized / token invalid
 *   1002 — Insufficient balance
 *   1003 — User has no parent agent / agent inactive
 *   4001 — Missing or invalid amount
 *   4004 — Withdrawal password not set
 *   4005 — Incorrect withdrawal password
 *   5001 — Internal transfer failure (DB / cache error)
 */
public class UserTransferToAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            String amountStr   = request.getParameter("am");
            String pwd         = request.getParameter("pwd");

            // --- 1. Auth validation ---
            if (accessToken == null || accessToken.isEmpty()) {
                return error(response, "1001", null);
            }

            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                return error(response, "1001", null);
            }
            String nickname = tokenMap.get(accessToken);

            // --- 2. Amount validation ---
            if (amountStr == null || amountStr.isEmpty()) {
                return error(response, "4001", "Số tiền không được để trống");
            }
            long amount;
            try {
                amount = Long.parseLong(amountStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                return error(response, "4001", "Số tiền không hợp lệ");
            }

            // --- 3. Password validation ---
            if (pwd == null || pwd.isEmpty()) {
                return error(response, "4001", "Mật khẩu rút tiền không được để trống");
            }

            // Load user data from DB (withdraw_password, referral_code, id)
            long userId = -1;
            String storedPwdHash = null;
            String referralCode = null;
            long currentBalance = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, withdraw_password, referral_code, vin FROM users WHERE nick_name = ?")) {
                ps.setString(1, nickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId       = rs.getLong("id");
                        storedPwdHash = rs.getString("withdraw_password");
                        referralCode  = rs.getString("referral_code");
                        currentBalance = rs.getLong("vin");
                    }
                }
            }

            if (userId <= 0) {
                return error(response, "1001", null);
            }

            // Check withdraw password setup
            if (storedPwdHash == null || storedPwdHash.isEmpty()) {
                return error(response, "4004", "Bạn chưa cài đặt mật khẩu rút tiền");
            }

            // Verify password (SHA-256)
            String inputHash = sha256(pwd);
            if (!storedPwdHash.equals(inputHash)) {
                return error(response, "4005", "Mật khẩu rút tiền không đúng");
            }

            // --- 3.5. Volume validation (Transfer policy requires betting volume) ---
            long totalRequiredVolume = 0;
            long totalActualVolume = 0;

            try (Connection connVol = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement psVol = connVol.prepareStatement(
                         "SELECT total_required_volume, total_actual_volume FROM user_volume_tracking WHERE user_id = ?")) {
                psVol.setLong(1, userId);
                try (ResultSet rsVol = psVol.executeQuery()) {
                    if (rsVol.next()) {
                        totalRequiredVolume = rsVol.getLong("total_required_volume");
                        totalActualVolume = rsVol.getLong("total_actual_volume");
                    }
                }
            } catch (Exception exVol) {
                logger.error("UserTransferToAgentProcessor check volume failed", exVol);
                return error(response, "5001", "Lỗi tải vòng cược, vui lòng thử lại");
            }

            boolean volumeMet = totalActualVolume >= totalRequiredVolume || (totalRequiredVolume == 0 && totalActualVolume == 0);
            if (!volumeMet) {
                long remaining = totalRequiredVolume - totalActualVolume;
                if (remaining < 0) remaining = 0;
                response.put("success", false);
                response.put("errorCode", "4007");
                response.put("message", "Chưa đủ volume cược để chuyển tiền. Còn thiếu: " + remaining);
                response.put("remaining_volume", remaining);
                return response.toString();
            }

            // --- 4. Resolve parent agent ---
            if (referralCode == null || referralCode.trim().isEmpty()) {
                return error(response, "1003", "Tài khoản không thuộc đại lý nào");
            }

            int agentId = 0;
            String agentNickname = null;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, nickname FROM vinplay_admin.useragent WHERE code = ? AND active = 1 LIMIT 1")) {
                ps.setString(1, referralCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentId      = rs.getInt("id");
                        agentNickname = rs.getString("nickname");
                    }
                }
            }

            if (agentId == 0) {
                return error(response, "1003", "Đại lý tuyến trên không tồn tại hoặc đã bị khóa");
            }

            // --- 5. Debit user game wallet (vin) via UserService cache-safe path ---
            UserServiceImpl userService = new UserServiceImpl();
            MoneyResponse debitResult = userService.updateMoney(
                    nickname,
                    -amount,            // negative = debit
                    "vin",
                    "TRANSFER_TO_AGENT",
                    "Chuyển tiền cho đại lý",
                    "Chuyển " + amount + " về ví Credit đại lý " + agentNickname,
                    0L,
                    null,
                    TransType.NO_VIPPOINT
            );

            if (!debitResult.isSuccess()) {
                String code = "1002".equals(debitResult.getErrorCode()) ? "1002" : "5001";
                String msg  = "1002".equals(debitResult.getErrorCode())
                        ? "Số dư không đủ để thực hiện giao dịch"
                        : "Lỗi xử lý giao dịch, vui lòng thử lại";
                return error(response, code, msg);
            }

            long newBalance = debitResult.getCurrentMoney();

            // --- 6. Credit agent Credit Wallet ---
            CreditWalletDao cwDao = new CreditWalletDaoImpl();
            boolean credited = cwDao.addBalance(agentId, amount);

            if (!credited) {
                // Rare — try to reverse the user debit (best-effort)
                logger.error("UserTransferToAgentProcessor: credit agent failed agentId=" + agentId
                        + " amount=" + amount + " — attempting user reversal for nick=" + nickname);
                userService.updateMoney(nickname, amount, "vin", "TRANSFER_REVERSAL",
                        "Hoàn tiền lỗi chuyển đại lý", "Hoàn tiền do lỗi cộng ví đại lý", 0L, null, TransType.NO_VIPPOINT);
                return error(response, "5001", "Lỗi cập nhật ví đại lý, giao dịch đã được hoàn tiền");
            }

            // --- 7. Log the transaction in credit_wallet_transactions ---
            long agentBalanceAfter = cwDao.getBalance(agentId);
            try {
                cwDao.logTransaction(
                        agentId,
                        agentNickname,
                        "USER_TRANSFER",   // type
                        "IN",              // direction: money flowing INTO agent
                        amount,
                        agentBalanceAfter,
                        nickname,          // related_user
                        null,              // related_agent_id (N/A)
                        "User " + nickname + " chuyển tiền"
                );
            } catch (Exception logEx) {
                // Non-fatal — money already moved, just log and continue
                logger.error("UserTransferToAgentProcessor: logTransaction failed", logEx);
            }

            logger.info("UserTransferToAgentProcessor: SUCCESS user=" + nickname
                    + " agent=" + agentNickname + " amount=" + amount
                    + " newBalance=" + newBalance + " agentCreditAfter=" + agentBalanceAfter);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("current_balance", newBalance);

        } catch (Exception e) {
            logger.error("UserTransferToAgentProcessor unexpected error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String error(JSONObject resp, String code, String message) {
        resp.put("success", false);
        resp.put("errorCode", code);
        if (message != null) resp.put("message", message);
        return resp.toString();
    }

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

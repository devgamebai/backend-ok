package com.vinplay.api.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * c=3041 — Create a bank withdrawal request.
 * Validates password, bank info, amount limits, volume status, deducts balance.
 */
public class WithdrawBankProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        String nickname = null;
        long amountKrw = 0;
        long feeKrw = 0;
        boolean balanceDeducted = false;

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Resolve user from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            nickname = tokenMap.get(accessToken);

            // SUN-1099: SpecialAccount is read-only — withdrawal forbidden.
            if (com.vinplay.dal.security.RoleGuard.isSpecialAccount(nickname)) {
                response.put("success", false);
                response.put("errorCode", com.vinplay.dal.security.RoleGuard.ERR_CODE_SPECIAL_ACCOUNT);
                response.put("message", com.vinplay.dal.security.RoleGuard.ERR_MSG_SPECIAL_ACCOUNT);
                return response.toString();
            }

            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse from POST body (form-encoded)
            String amountStr = null;
            String withdrawPwd = null;
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String ln;
                while ((ln = reader.readLine()) != null) sb.append(ln);
                String body = sb.toString().trim();
                if (body.contains("=")) {
                    for (String pair : body.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) {
                            String k = java.net.URLDecoder.decode(kv[0], "UTF-8");
                            String v = java.net.URLDecoder.decode(kv[1], "UTF-8");
                            if ("amount".equals(k)) amountStr = v;
                            if ("withdraw_password".equals(k) || "password".equals(k)) withdrawPwd = v;
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Fallback: read from query params
            if (amountStr == null || amountStr.isEmpty()) amountStr = request.getParameter("amount");
            if (withdrawPwd == null || withdrawPwd.isEmpty()) withdrawPwd = request.getParameter("withdraw_password");
            if (withdrawPwd == null || withdrawPwd.isEmpty()) withdrawPwd = request.getParameter("password");

            // Validate amount param
            if (amountStr == null || amountStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Amount is required");
                return response.toString();
            }
            amountKrw = Long.parseLong(amountStr);
            if (amountKrw <= 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Invalid amount");
                return response.toString();
            }

            // Validate withdrawal password
            if (withdrawPwd == null || withdrawPwd.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4004");
                response.put("message", "Withdrawal password is required");
                return response.toString();
            }

            // Check password against DB
            try (Connection connPwd = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement psPwd = connPwd.prepareStatement("SELECT withdraw_password FROM users WHERE id = ?")) {
                psPwd.setLong(1, userId);
                try (ResultSet rsPwd = psPwd.executeQuery()) {
                    if (rsPwd.next()) {
                        String storedHash = rsPwd.getString("withdraw_password");
                        if (storedHash == null || storedHash.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "4004");
                            response.put("message", "Withdrawal password not set");
                            return response.toString();
                        }
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] hash = md.digest(withdrawPwd.getBytes("UTF-8"));
                        StringBuilder hashSb = new StringBuilder();
                        for (byte b : hash) hashSb.append(String.format("%02x", b));
                        if (!storedHash.equals(hashSb.toString())) {
                            response.put("success", false);
                            response.put("errorCode", "4005");
                            response.put("message", "Incorrect withdrawal password");
                            return response.toString();
                        }
                    }
                }
            }

            // Check bank info
            String bankName = null;
            String bankNumber = null;
            String customerName = null;

            try (Connection connBank = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement psBank = connBank.prepareStatement(
                         "SELECT bank_name, bank_number, customer_name FROM users_bank WHERE user_id = ? LIMIT 1")) {
                psBank.setLong(1, userId);
                try (ResultSet rsBank = psBank.executeQuery()) {
                    if (rsBank.next()) {
                        bankName = rsBank.getString("bank_name");
                        bankNumber = rsBank.getString("bank_number");
                        customerName = rsBank.getString("customer_name");
                    }
                }
            }

            if (bankName == null || bankNumber == null) {
                response.put("success", false);
                response.put("errorCode", "4006");
                response.put("message", "No bank info saved");
                return response.toString();
            }

            // Check withdrawal_settings for min/max
            long minBankWithdraw = 0;
            long maxBankWithdraw = Long.MAX_VALUE;

            try (Connection connSettings = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement psSettings = connSettings.prepareStatement(
                         "SELECT setting_key, setting_value FROM withdrawal_settings WHERE setting_key IN ('min_bank_withdraw_krw', 'max_bank_withdraw_krw', 'bank_withdraw_fee_krw')")) {
                try (ResultSet rsSettings = psSettings.executeQuery()) {
                    while (rsSettings.next()) {
                        String key = rsSettings.getString("setting_key");
                        String val = rsSettings.getString("setting_value");
                        if ("min_bank_withdraw_krw".equals(key) && val != null && !val.isEmpty()) {
                            minBankWithdraw = Long.parseLong(val);
                        } else if ("max_bank_withdraw_krw".equals(key) && val != null && !val.isEmpty()) {
                            maxBankWithdraw = Long.parseLong(val);
                        } else if ("bank_withdraw_fee_krw".equals(key) && val != null && !val.isEmpty()) {
                            feeKrw = Long.parseLong(val);
                        }
                    }
                }
            }

            if (amountKrw < minBankWithdraw) {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Minimum withdrawal is " + minBankWithdraw + " KRW");
                return response.toString();
            }

            if (amountKrw > maxBankWithdraw) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Maximum withdrawal is " + maxBankWithdraw + " KRW");
                return response.toString();
            }

            // SUN-639: Check pending withdrawal limit
            int maxPending = 1; // default
            try (Connection connPend = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement psPend = connPend.prepareStatement(
                        "SELECT setting_value FROM withdrawal_settings WHERE setting_key='max_pending_withdrawals'")) {
                    try (ResultSet rsPend = psPend.executeQuery()) {
                        if (rsPend.next()) {
                            String val = rsPend.getString("setting_value");
                            if (val != null && !val.isEmpty()) maxPending = Integer.parseInt(val);
                        }
                    }
                }
                // Cross-product with crypto withdraw — a player who has a
                // PENDING crypto withdraw can't also create a bank withdraw
                // (and vice-versa, see WithdrawCryptoProcessor). Same
                // setting key, same gate.
                try (PreparedStatement psCnt = connPend.prepareStatement(
                        "SELECT " +
                        "(SELECT COUNT(*) FROM bank_withdrawals   WHERE user_id = ? AND status = 'PENDING') + " +
                        "(SELECT COUNT(*) FROM crypto_withdrawals WHERE user_id = ? AND status = 'PENDING')")) {
                    psCnt.setLong(1, userId);
                    psCnt.setLong(2, userId);
                    try (ResultSet rsCnt = psCnt.executeQuery()) {
                        int pendingCount = rsCnt.next() ? rsCnt.getInt(1) : 0;
                        if (pendingCount >= maxPending) {
                            response.put("success", false);
                            response.put("errorCode", "4006");
                            // Friendlier wording per ops: spell out the
                            // wait instead of just stating the count.
                            response.put("message", "Bạn đang có " + pendingCount
                                    + " lệnh rút chờ xử lý. Vui lòng đợi lệnh trước được duyệt hoặc từ chối rồi mới tạo lệnh mới (tối đa "
                                    + maxPending + " lệnh chờ tại một thời điểm).");
                            response.put("pending_count", pendingCount);
                            response.put("max_pending", maxPending);
                            return response.toString();
                        }
                    }
                }
            }

            // Check volume status
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
            }

            boolean volumeMet = totalActualVolume >= totalRequiredVolume || (totalRequiredVolume == 0 && totalActualVolume == 0);

            // SUN-1 TH3: When volume not met, check if user has commission/rebate earnings
            // If yes, allow withdrawal up to (total_commission - commission_already_withdrawn)
            if (!volumeMet) {
                long commissionAvailable = 0;
                long commissionWithdrawn = 0;
                try (Connection connFee = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement psFee = connFee.prepareStatement(
                             "SELECT COALESCE(SUM(fee), 0) AS total_fee, COALESCE(SUM(fee_withdrawn), 0) AS total_withdrawn " +
                             "FROM user_fee WHERE user_id = ?")) {
                    psFee.setLong(1, userId);
                    try (ResultSet rsFee = psFee.executeQuery()) {
                        if (rsFee.next()) {
                            commissionAvailable = rsFee.getLong("total_fee");
                            commissionWithdrawn = rsFee.getLong("total_withdrawn");
                        }
                    }
                }

                long maxCommissionWithdraw = commissionAvailable - commissionWithdrawn;
                if (maxCommissionWithdraw < 0) maxCommissionWithdraw = 0;

                if (maxCommissionWithdraw <= 0) {
                    // No commission available either — block withdrawal
                    long remaining = totalRequiredVolume - totalActualVolume;
                    if (remaining < 0) remaining = 0;
                    response.put("success", false);
                    response.put("errorCode", "4007");
                    response.put("message", "Chưa đủ volume để rút. Còn thiếu: " + remaining);
                    response.put("remaining_volume", remaining);
                    return response.toString();
                }

                // Has commission but volume not met — cap withdrawal at commission amount
                if (amountKrw > maxCommissionWithdraw) {
                    response.put("success", false);
                    response.put("errorCode", "4008");
                    response.put("message", "Chưa đủ volume. Tối đa được rút hoa hồng: " + maxCommissionWithdraw);
                    response.put("max_commission_withdraw", maxCommissionWithdraw);
                    return response.toString();
                }
                // Withdrawal amount <= commission available → allow (falls through to deduct)
            }

            // Generate tx code
            String txCode = com.vinplay.dal.utils.TxCodeGenerator.bankWithdraw();

            // SUN-1200: deduct via MoneyGateway.debitUser (atomic, DB-first,
            // dedup-keyed). The legacy UserServiceImpl.updateMoneyFromAdmin
            // mutated only the Hazelcast cache and relied on an async RMQ
            // consumer to persist users.vin — that consumer never ran for
            // BankWithdraw / CryptoWithdraw, so the DB never saw the debit.
            // Refund (REFUND_WITHDRAW) goes through MoneyGateway.creditUser
            // which DOES write the DB, leaving DB users.vin strictly higher
            // than before each cycle. That's the SUN-1200 exploit (KwonUSD
            // up 300k after 3 reject loops). debitUser closes the loop.
            com.vinplay.dal.service.MoneyGateway.CreditResult deductResult =
                    com.vinplay.dal.service.MoneyGateway.debitUser(
                            userId, nickname, amountKrw,
                            com.vinplay.dal.service.MoneyGateway.SOURCE_WITHDRAW_BANK,
                            txCode, "Bank withdrawal " + txCode);

            if (deductResult == null || !deductResult.success) {
                String errMsg = deductResult != null && deductResult.error != null
                        ? deductResult.error : "Insufficient balance";
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", errMsg);
                return response.toString();
            }
            balanceDeducted = true;

            // Insert into bank_withdrawals
            long dbId = -1;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String sql = "INSERT INTO bank_withdrawals (user_id, nick_name, bank_name, bank_number, customer_name, " +
                        "amount_krw, fee_krw, tx_code, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setString(3, bankName);
                    ps.setString(4, bankNumber);
                    ps.setString(5, customerName);
                    ps.setLong(6, amountKrw);
                    ps.setLong(7, feeKrw);
                    ps.setString(8, txCode);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            dbId = rs.getLong(1);
                        }
                    }
                }
            } catch (Exception insertErr) {
                logger.error("WithdrawBankProcessor insert failed, refunding txCode=" + txCode, insertErr);
                // SUN-1200: refund via MoneyGateway.creditUser so the DB
                // actually moves back. The legacy updateMoneyFromAdmin
                // path was the original cache-vs-DB drift culprit.
                try {
                    com.vinplay.dal.service.MoneyGateway.creditUser(
                            userId, nickname, amountKrw,
                            com.vinplay.dal.service.MoneyGateway.SOURCE_REFUND_WITHDRAW,
                            "INSERT-FAIL-" + txCode,
                            "Insert failed, refund withdrawal " + txCode);
                    balanceDeducted = false;
                } catch (Exception refundErr) {
                    logger.error("WithdrawBankProcessor refund failed txCode=" + txCode, refundErr);
                }
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // SUN-1220 — Telegram notify (best-effort, never blocks the
            // user-facing response). Mirrors WithdrawCryptoProcessor's
            // path: post pending message, capture message_id, persist on
            // the bank_withdrawals row so admin/bot approve+reject can
            // edit it back to terminal state.
            try {
                com.vinplay.dal.withdraw.TelegramBankWithdrawNotifier notifier =
                        new com.vinplay.dal.withdraw.TelegramBankWithdrawNotifier();
                if (notifier.isEnabled() && dbId > 0) {
                    long msgId = notifier.sendNewWithdrawal(
                            dbId, nickname, bankName, bankNumber, customerName, amountKrw, txCode);
                    if (msgId > 0) {
                        try (Connection conn2 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                             PreparedStatement ps = conn2.prepareStatement(
                                     "UPDATE bank_withdrawals SET telegram_message_id = ? WHERE id = ?")) {
                            ps.setLong(1, msgId);
                            ps.setLong(2, dbId);
                            ps.executeUpdate();
                        }
                    }
                }
            } catch (Throwable tgErr) {
                logger.warn("WithdrawBankProcessor Telegram notify failed dbId=" + dbId
                        + " (non-fatal — withdrawal already in DB)", tgErr);
            }

            JSONObject data = new JSONObject();
            data.put("id", dbId);
            data.put("status", "PENDING");
            data.put("amount_krw", amountKrw);
            // SUN-1219: include new_balance so the client can update display immediately
            // without waiting for WebSocket balance_update (which can be delayed or missed
            // on Hazelcast cache miss, leaving the client stuck at 0).
            data.put("new_balance", deductResult.newBalance);
            response.put("success", true);
            response.put("data", data);
            response.put("currentMoney", deductResult.newBalance);

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid amount");
        } catch (Exception e) {
            logger.error("WithdrawBankProcessor error", e);
            // SUN-1200: emergency refund via MoneyGateway.creditUser. The
            // (userId, txCode) needed for dedup are scoped inside the try
            // block above and not visible here, so the refund uses a
            // synthetic "EMERGENCY-<nickname>-<amount>-<ts>" txId. That
            // means a true-double-fault could double-refund — acceptable
            // tradeoff because the alternative (no refund) is worse, and
            // this only fires on an unexpected exception path which
            // should never happen in steady state.
            if (balanceDeducted && nickname != null) {
                try {
                    long emergencyUserIdLookup = -1;
                    try (java.sql.Connection conn2 = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         java.sql.PreparedStatement ps2 = conn2.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                        ps2.setString(1, nickname);
                        try (java.sql.ResultSet rs2 = ps2.executeQuery()) {
                            if (rs2.next()) emergencyUserIdLookup = rs2.getLong("id");
                        }
                    }
                    if (emergencyUserIdLookup > 0) {
                        com.vinplay.dal.service.MoneyGateway.creditUser(
                                emergencyUserIdLookup, nickname, amountKrw,
                                com.vinplay.dal.service.MoneyGateway.SOURCE_REFUND_WITHDRAW,
                                "EMERGENCY-bank-" + nickname + "-" + System.currentTimeMillis(),
                                "Error refund withdrawal");
                    }
                } catch (Exception refundErr) {
                    logger.error("WithdrawBankProcessor emergency refund failed", refundErr);
                }
            }
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

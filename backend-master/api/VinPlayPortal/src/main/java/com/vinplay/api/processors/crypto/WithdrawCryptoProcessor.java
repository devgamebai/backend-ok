package com.vinplay.api.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * c=3021 — Create a crypto (USDT TRC20) withdrawal request.
 * Deducts KRW balance, inserts into crypto_withdrawals, calls gateway.
 *
 * SUN-933: refactored from 6 separate getConnection() calls to a single
 * shared connection via try-with-resources (RAII pattern). Reduces peak
 * pool demand 6x for crypto withdrawals.
 */
public class WithdrawCryptoProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final long KRW_PER_USDT = 1350L;
    // Min/max are read from withdrawal_settings (`min_crypto_withdraw_krw`,
    // `max_crypto_withdraw_krw`) for parity with WithdrawBankProcessor — ops
    // can tune them at runtime without a redeploy. These constants are the
    // hard fallbacks if the settings rows are missing or unparseable.
    private static final long MIN_WITHDRAW_KRW_FALLBACK = 100_000L;
    private static final long MAX_WITHDRAW_KRW_FALLBACK = 50_000_000L;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        String nickname = null;
        long amountKrw = 0;
        boolean balanceDeducted = false;
        // SUN-1200: hoisted so the outer catch block's emergency
        // refund can find the right (userId, txCode) without
        // shadowing variables.
        long emergencyUserId = -1;
        String txCodeForRefund = null;

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

            // Parity with WithdrawBankProcessor (c=3041, line 58-64): SUN-1099
            // SpecialAccount is read-only — withdrawal forbidden. The crypto
            // path was missing this guard, so a SpecialAccount could withdraw
            // USDT even though the same account was blocked from bank
            // withdrawal.
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
            }

            // Verify withdrawal password + parse params
            String withdrawPwd = request.getParameter("withdraw_password");
            if (withdrawPwd == null) withdrawPwd = request.getParameter("password");
            try {
                String ct = request.getContentType();
                if (ct != null && withdrawPwd == null) {
                    StringBuilder sb = new StringBuilder();
                    java.io.BufferedReader reader = request.getReader();
                    String ln; while ((ln = reader.readLine()) != null) sb.append(ln);
                    String body = sb.toString().trim();
                    if (body.startsWith("{")) {
                        org.json.JSONObject jb = new org.json.JSONObject(body);
                        if (withdrawPwd == null) withdrawPwd = jb.optString("withdraw_password", null);
                        if (withdrawPwd == null) withdrawPwd = jb.optString("password", null);
                    }
                }
            } catch (Exception ignored) {}

            if (withdrawPwd == null || withdrawPwd.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4010");
                response.put("message", "Withdrawal password is required");
                return response.toString();
            }

            String toAddress = request.getParameter("to_address");
            String amountStr = request.getParameter("amount");

            if (toAddress == null || !toAddress.matches("^T[a-zA-Z1-9]{33}$")) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Invalid TRON address");
                return response.toString();
            }

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

            // ── Single shared connection for ALL DB operations ──
            // SUN-933: was 6 separate getConnection() calls; now 1.
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // Resolve userId if not in cache
                if (userId <= 0) {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                        ps.setString(1, nickname);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) userId = rs.getLong("id");
                        }
                    }
                }
                if (userId <= 0) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    return response.toString();
                }

                // Parity with WithdrawBankProcessor: read min / max from
                // withdrawal_settings so ops can tune at runtime. Fall back
                // to the hardcoded floor if the keys are missing.
                long minWithdraw = MIN_WITHDRAW_KRW_FALLBACK;
                long maxWithdraw = MAX_WITHDRAW_KRW_FALLBACK;
                try (PreparedStatement psSettings = conn.prepareStatement(
                        "SELECT setting_key, setting_value FROM withdrawal_settings " +
                        "WHERE setting_key IN ('min_crypto_withdraw_krw', 'max_crypto_withdraw_krw')")) {
                    try (ResultSet rsSettings = psSettings.executeQuery()) {
                        while (rsSettings.next()) {
                            String key = rsSettings.getString("setting_key");
                            String val = rsSettings.getString("setting_value");
                            if (val == null || val.isEmpty()) continue;
                            try {
                                if ("min_crypto_withdraw_krw".equals(key)) minWithdraw = Long.parseLong(val);
                                else if ("max_crypto_withdraw_krw".equals(key)) maxWithdraw = Long.parseLong(val);
                            } catch (NumberFormatException nfe) {
                                logger.warn("WithdrawCryptoProcessor: bad withdrawal_settings value key=" + key + " val=" + val);
                            }
                        }
                    }
                }

                if (amountKrw < minWithdraw) {
                    response.put("success", false);
                    response.put("errorCode", "4002");
                    response.put("message", "Minimum withdrawal is " + minWithdraw + " KRW");
                    return response.toString();
                }
                if (amountKrw > maxWithdraw) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "Maximum withdrawal is " + maxWithdraw + " KRW");
                    return response.toString();
                }

                // SUN-639 parity: cap concurrent PENDING withdrawals per user.
                // Crypto and bank share the same `max_pending_withdrawals` key
                // — a user with one pending bank withdrawal cannot also have
                // a pending crypto withdrawal (and vice-versa). Counting both
                // tables keeps the gate consistent regardless of the request
                // path the player took.
                int maxPending = 1;
                try (PreparedStatement psPend = conn.prepareStatement(
                        "SELECT setting_value FROM withdrawal_settings WHERE setting_key='max_pending_withdrawals'")) {
                    try (ResultSet rsPend = psPend.executeQuery()) {
                        if (rsPend.next()) {
                            String val = rsPend.getString("setting_value");
                            if (val != null && !val.isEmpty()) {
                                try { maxPending = Integer.parseInt(val); } catch (NumberFormatException ignore) {}
                            }
                        }
                    }
                }
                int pendingCount = 0;
                try (PreparedStatement psCnt = conn.prepareStatement(
                        "SELECT " +
                        "(SELECT COUNT(*) FROM crypto_withdrawals WHERE user_id = ? AND status = 'PENDING') + " +
                        "(SELECT COUNT(*) FROM bank_withdrawals   WHERE user_id = ? AND status = 'PENDING')")) {
                    psCnt.setLong(1, userId);
                    psCnt.setLong(2, userId);
                    try (ResultSet rsCnt = psCnt.executeQuery()) {
                        if (rsCnt.next()) pendingCount = rsCnt.getInt(1);
                    }
                }
                if (pendingCount >= maxPending) {
                    response.put("success", false);
                    response.put("errorCode", "4006");
                    // Friendlier wording per ops feedback: tell the player
                    // exactly what to do (wait for the previous request to
                    // finish) instead of just stating the count. Same
                    // wording on bank withdraw for parity.
                    response.put("message", "Bạn đang có " + pendingCount
                            + " lệnh rút chờ xử lý. Vui lòng đợi lệnh trước được duyệt hoặc từ chối rồi mới tạo lệnh mới (tối đa "
                            + maxPending + " lệnh chờ tại một thời điểm).");
                    response.put("pending_count", pendingCount);
                    response.put("max_pending", maxPending);
                    return response.toString();
                }

                // Check password against DB
                try (PreparedStatement stm = conn.prepareStatement("SELECT withdraw_password FROM users WHERE id = ?")) {
                    stm.setLong(1, userId);
                    try (ResultSet rs = stm.executeQuery()) {
                        if (rs.next()) {
                            String storedHash = rs.getString("withdraw_password");
                            if (storedHash == null || storedHash.isEmpty()) {
                                response.put("success", false);
                                response.put("errorCode", "4004");
                                response.put("message", "Withdrawal password not set");
                                return response.toString();
                            }
                            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
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

                // Check volume status
                long totalRequiredVolume = 0;
                long totalActualVolume = 0;
                try (PreparedStatement psVol = conn.prepareStatement(
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

                // SUN-892: commission carve-out parity with bank withdraw
                if (!volumeMet) {
                    long commissionAvailable = 0;
                    long commissionWithdrawn = 0;
                    try (PreparedStatement psFee = conn.prepareStatement(
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
                        long remaining = totalRequiredVolume - totalActualVolume;
                        if (remaining < 0) remaining = 0;
                        response.put("success", false);
                        response.put("errorCode", "4007");
                        response.put("message", "Chưa đủ volume để rút. Còn thiếu: " + remaining);
                        response.put("remaining_volume", remaining);
                        return response.toString();
                    }

                    if (amountKrw > maxCommissionWithdraw) {
                        response.put("success", false);
                        response.put("errorCode", "4008");
                        response.put("message", "Chưa đủ volume. Tối đa được rút hoa hồng: " + maxCommissionWithdraw);
                        response.put("max_commission_withdraw", maxCommissionWithdraw);
                        return response.toString();
                    }
                }

                // 6 decimals matches the crypto_withdrawals.amount_usdt column scale.
                BigDecimal amountUsdt = BigDecimal.valueOf(amountKrw)
                        .divide(BigDecimal.valueOf(KRW_PER_USDT), 6, RoundingMode.HALF_UP);
                String txCode = com.vinplay.dal.utils.TxCodeGenerator.cryptoWithdraw();
                txCodeForRefund = txCode;
                emergencyUserId = userId;

                // SUN-1200: deduct via MoneyGateway.debitUser, NOT
                // UserServiceImpl.updateMoneyFromAdmin. The legacy path
                // mutated only the Hazelcast cache for cached users; the
                // DB users.vin was never touched. Reject path used
                // MoneyGateway.creditUser which DID write the DB, so
                // each debit-then-refund cycle increased DB balance
                // without bound (KwonUSD: 3 reject loops netted +300k).
                // debitUser is atomic, DB-first, dedup-keyed on
                // (txCode, source) — replay returns "Duplicate
                // transaction" so a stale resubmit can't double-debit.
                com.vinplay.dal.service.MoneyGateway.CreditResult deductResult =
                        com.vinplay.dal.service.MoneyGateway.debitUser(
                                userId, nickname, amountKrw,
                                com.vinplay.dal.service.MoneyGateway.SOURCE_WITHDRAW_CRYPTO,
                                txCode, "USDT withdrawal " + txCode);

                if (deductResult == null || !deductResult.success) {
                    String errMsg = deductResult != null && deductResult.error != null
                            ? deductResult.error : "Insufficient balance";
                    response.put("success", false);
                    response.put("errorCode", "4003");
                    response.put("message", errMsg);
                    return response.toString();
                }
                balanceDeducted = true;

                // Insert into crypto_withdrawals
                long dbId = -1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO crypto_withdrawals (user_id, nick_name, to_address, amount_krw, amount_usdt, " +
                        "tx_code, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setString(3, toAddress);
                    ps.setLong(4, amountKrw);
                    ps.setBigDecimal(5, amountUsdt);
                    ps.setString(6, txCode);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) dbId = rs.getLong(1);
                    }
                }

                // ── Manual-approval gating (parity with bank withdraw flow) ───────
                // Per ops request: crypto withdraw must wait for admin approval
                // BEFORE the gateway is called, mirroring how bank_withdrawals
                // wait for an admin to push them through. The auto-gateway-call
                // here used to forward to TronGatewayClient.createWithdrawal +
                // approveWithdrawal immediately on player submit, which made the
                // on-chain payout effectively automatic — bypassing AML / fraud
                // review. The admin endpoints already exist and handle the
                // gateway call themselves:
                //   c=9630  ListPendingCryptoWithdrawalsProcessor
                //   c=9631  ApproveCryptoWithdrawalProcessor   (creates + approves
                //                                                gateway side, sets
                //                                                status=APPROVED)
                //   c=9632  RejectCryptoWithdrawalProcessor    (refunds vin, sets
                //                                                status=REJECTED)
                // Player money stays debited (frozen) on the PENDING row until
                // admin acts — same UX as bank withdraw. Reject refunds via
                // userService.updateMoneyFromAdmin in the admin processor.
                //
                // Telegram notification is sent below so ops sees the request
                // immediately and can act through the admin panel.
                String gatewayTxId = "";
                boolean gwOk = true;          // never call gateway here
                String gatewayError = null;

                // (Manual-approval gating: gateway is no longer called from this
                // processor. The auto-refund-on-gateway-error block from SUN-1171
                // is no longer reachable here — admin Approve / Reject endpoints
                // own the gateway interaction now. Reject (c=9632) refunds vin
                // via UserServiceImpl.updateMoneyFromAdmin, mirroring the same
                // refund-first / row-update-second invariants.)

                // SUN-1186: keep status=PENDING.
                //
                // Pre-fix: stamped status='APPROVED' immediately on gateway
                // accept, but the on-chain TX hadn't been broadcast yet —
                // ProcessWithdrawalToNetworkJob (gateway, hourly) is what
                // actually puts the USDT on TRON. The FE/game client renders
                // 'APPROVED' as "Thành công" (Success), so players saw their
                // withdraw marked "Success" while no money had moved on-chain.
                //
                // Per QC/PM: status must remain "Đang chờ" (PENDING) until the
                // on-chain transfer is mined and confirmed. The c=3027
                // NotifyCryptoWithdrawalProcessor flips PENDING → COMPLETED
                // when ProcessWithdrawalToNetworkJob's on-chain broadcast
                // succeeds and notifies us back with the tx_hash — that is
                // the single, real moment of "Thành công".
                //
                // Manual-approval gating: leave gateway_tx_id NULL and admin_by NULL.
                // The admin Approve endpoint (c=9631) will create the gateway record
                // on its first run and stamp admin_by with the admin's nickname.
                // No DB update needed here — the INSERT above already sets
                // status='PENDING' which is the correct waiting state.

                // Telegram notification
                try {
                    com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier notifier =
                            new com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier();
                    if (notifier.isEnabled()) {
                        long msgId = notifier.sendNewWithdrawal(dbId, nickname, toAddress, amountKrw, amountUsdt, txCode);
                        if (msgId > 0 && dbId > 0) {
                            try (PreparedStatement ps4 = conn.prepareStatement(
                                    "UPDATE crypto_withdrawals SET telegram_message_id = ? WHERE id = ?")) {
                                ps4.setLong(1, msgId);
                                ps4.setLong(2, dbId);
                                ps4.executeUpdate();
                            }
                        }
                    }
                } catch (Exception tgErr) {
                    logger.warn("WithdrawCryptoProcessor Telegram notification failed", tgErr);
                }

                JSONObject data = new JSONObject();
                data.put("id", dbId);
                data.put("status", "PENDING");
                data.put("amount_krw", amountKrw);
                // SUN-1219: include new_balance so the client can update display immediately
                data.put("new_balance", deductResult.newBalance);
                response.put("success", true);
                response.put("data", data);
                response.put("currentMoney", deductResult.newBalance);

            } // conn auto-closed here (RAII)

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid amount");
        } catch (Exception e) {
            logger.error("WithdrawCryptoProcessor error", e);
            if (balanceDeducted && nickname != null && emergencyUserId > 0) {
                // SUN-1200: emergency refund path uses MoneyGateway.creditUser
                // so the DB users.vin actually moves back. The legacy
                // updateMoneyFromAdmin only adjusted Hazelcast cache, leaving
                // DB at the post-debit value (DB had already been debited
                // because the new c=3021 path uses MoneyGateway.debitUser
                // for the deduct — both sides now go through the same
                // money_gateway_log audit, dedup-keyed on (txCode, source)).
                try {
                    com.vinplay.dal.service.MoneyGateway.creditUser(
                            emergencyUserId, nickname, amountKrw,
                            com.vinplay.dal.service.MoneyGateway.SOURCE_REFUND_WITHDRAW,
                            "EMERGENCY-" + (txCodeForRefund != null ? txCodeForRefund : "unknown"),
                            "Error refund withdrawal");
                } catch (Exception refundErr) {
                    logger.error("WithdrawCryptoProcessor emergency refund failed", refundErr);
                }
            }
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

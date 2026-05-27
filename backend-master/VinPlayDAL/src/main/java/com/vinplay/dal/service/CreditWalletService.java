package com.vinplay.dal.service;

import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.dal.withdraw.VolumeTrackingService;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CreditWalletService — tất cả business logic cho Credit Wallet.
 *
 * Credit Wallet là ví riêng của agent, tách biệt với Agency Wallet và Game Wallet.
 * In:  Admin bơm trực tiếp (c=9921)
 * Out: Credit→Credit cùng nhánh TĐL (c=9922)
 *      Credit→Game Wallet user/agent (c=9923) — auto-approve + nhận KM
 *
 * Business Rules:
 *   - Transfer limit: tối đa 10,000,000 mỗi giao dịch
 *   - Frozen: agent active=0 không được giao dịch (kiểm tra tại Processor layer)
 *   - "Chung nhánh": cùng TĐL gốc (level=1), xác định qua useragent.ancestors
 *   - OTP bắt buộc cho mọi giao dịch (kiểm tra tại Processor layer)
 */
public class CreditWalletService {

    private static final Logger logger = Logger.getLogger("api");

    /** Giới hạn tối đa mỗi giao dịch: 10 triệu */
    public static final long MAX_TRANSFER_AMOUNT = 10_000_000L;

    // ===== Inner classes =====

    /** Kết quả trả về từ tất cả service methods. */
    public static class CreditResult {
        public boolean success;
        public String errorCode;
        public String message;
        public long senderBalance;     // Credit wallet balance của sender sau giao dịch
        public boolean promoApplied;   // KM có được áp dụng không (chỉ khi deposit → game)
        public long bonusAmount;       // Số tiền KM (nếu có)
        public String promoType;       // Loại KM (FIRST_DEPOSIT, DAILY_DEPOSIT...)

        public static CreditResult ok(long balance) {
            CreditResult r = new CreditResult();
            r.success = true;
            r.errorCode = "0";
            r.senderBalance = balance;
            return r;
        }

        public static CreditResult fail(String code, String msg) {
            CreditResult r = new CreditResult();
            r.success = false;
            r.errorCode = code;
            r.message = msg;
            return r;
        }
    }

    /** Agent info từ vinplay_admin.useragent. */
    public static class AgentInfo {
        public int id;
        public String nickname;
        public String code;
        public int level;
        public String ancestors;
        public int active;
        public int parentid;
    }

    // ===== ① Admin topup Credit Wallet =====

    /**
     * Admin bơm tiền trực tiếp vào Credit Wallet của agent.
     * Không cần OTP, không cần duyệt.
     * Ghi log ADMIN_CREDIT.
     */
    public static CreditResult adminTopup(int agentId, String agentNick,
                                           long amount, String adminName, String note) {
        try {
            if (amount <= 0 || amount > MAX_TRANSFER_AMOUNT) {
                return CreditResult.fail("4002", "Amount must be 1 to " + MAX_TRANSFER_AMOUNT);
            }
            CreditWalletDao dao = new CreditWalletDaoImpl();
            boolean ok = dao.addBalance(agentId, amount);
            if (!ok) return CreditResult.fail("1004", "Failed to credit wallet");

            long newBal = dao.getBalance(agentId);
            String noteStr = (note != null && !note.isEmpty()) ? note : "Admin topup by " + adminName;
            dao.logTransaction(agentId, agentNick, "ADMIN_CREDIT", "CREDIT",
                    amount, newBal, adminName, null, noteStr);

            logger.info("CreditWallet ADMIN_CREDIT: agent=" + agentNick +
                    " amount=" + amount + " by=" + adminName + " balance=" + newBal);
            return CreditResult.ok(newBal);
        } catch (Exception e) {
            logger.error("CreditWalletService.adminTopup error agentId=" + agentId, e);
            return CreditResult.fail("1001", "Internal error");
        }
    }

    /**
     * Admin thu hồi (trừ) tiền từ Credit Wallet của agent.
     * Thu hồi sử dụng dao.debitBalance để tránh âm số dư.
     * Ghi log ADMIN_CREDIT nhưng với chiều DEBIT để tái sử dụng giao diện CMS.
     */
    public static CreditResult adminRevoke(int agentId, String agentNick,
                                           long amount, String adminName, String note) {
        try {
            if (amount <= 0 || amount > MAX_TRANSFER_AMOUNT) {
                return CreditResult.fail("4002", "Amount must be 1 to " + MAX_TRANSFER_AMOUNT);
            }
            CreditWalletDao dao = new CreditWalletDaoImpl();
            
            // Debit balance to prevent negative balance
            boolean ok = dao.debitBalance(agentId, amount);
            if (!ok) return CreditResult.fail("4005", "Insufficient credit wallet balance");

            long newBal = dao.getBalance(agentId);
            String noteStr = (note != null && !note.isEmpty()) ? note : "Admin revoke by " + adminName;
            // Use ADMIN_REVOKE type for explicit history tracking
            dao.logTransaction(agentId, agentNick, "ADMIN_REVOKE", "DEBIT",
                    amount, newBal, adminName, null, noteStr);

            logger.info("CreditWallet ADMIN_REVOKE: agent=" + agentNick +
                    " amount=" + amount + " by=" + adminName + " balance=" + newBal);
            return CreditResult.ok(newBal);
        } catch (Exception e) {
            logger.error("CreditWalletService.adminRevoke error agentId=" + agentId, e);
            return CreditResult.fail("1001", "Internal error");
        }
    }

    // ===== ② Agent transfer Credit → Credit (cùng nhánh) =====

    /**
     * Agent chuyển Credit Wallet sang Credit Wallet của agent khác cùng nhánh TĐL.
     * Không nhận KM (chỉ transfer nội bộ).
     * Ghi log TRANSFER_OUT (sender) và TRANSFER_IN (receiver).
     *
     * Caller (Processor) phải kiểm tra: active, OTP, amount limit, cùng nhánh.
     */
    public static CreditResult transferToAgent(int fromAgentId, String fromNick,
                                                int toAgentId, String toNick,
                                                long amount, String note) {
        try {
            if (amount <= 0 || amount > MAX_TRANSFER_AMOUNT) {
                return CreditResult.fail("4002", "Amount must be 1 to " + MAX_TRANSFER_AMOUNT);
            }
            if (fromAgentId == toAgentId) {
                return CreditResult.fail("4011", "Cannot transfer to yourself");
            }

            CreditWalletDao dao = new CreditWalletDaoImpl();

            // Debit sender (atomic: chỉ thành công nếu balance >= amount)
            boolean debited = dao.debitBalance(fromAgentId, amount);
            if (!debited) return CreditResult.fail("4005", "Insufficient credit wallet balance");

            long fromBal = dao.getBalance(fromAgentId);

            // Credit receiver
            boolean credited = dao.addBalance(toAgentId, amount);
            if (!credited) {
                // Refund sender best-effort
                try {
                    dao.addBalance(fromAgentId, amount);
                    logger.warn("CreditWallet TRANSFER: credit receiver failed, refunded sender=" + fromNick);
                } catch (Exception re) {
                    logger.error("CreditWallet REFUND FAILED after receiver credit fail: sender=" + fromNick, re);
                }
                return CreditResult.fail("1005", "Failed to credit receiver — sender refunded");
            }
            long toBal = dao.getBalance(toAgentId);

            // Log both sides
            String noteStr = (note != null && !note.isEmpty()) ? note : "Transfer to " + toNick;
            dao.logTransaction(fromAgentId, fromNick, "TRANSFER_OUT", "DEBIT",
                    amount, fromBal, toNick, toAgentId, noteStr);
            dao.logTransaction(toAgentId, toNick, "TRANSFER_IN", "CREDIT",
                    amount, toBal, fromNick, fromAgentId, "Transfer from " + fromNick);

            logger.info("CreditWallet TRANSFER: " + fromNick + " → " + toNick +
                    " amount=" + amount + " senderBal=" + fromBal);
            return CreditResult.ok(fromBal);
        } catch (Exception e) {
            logger.error("CreditWalletService.transferToAgent error fromId=" + fromAgentId + " toId=" + toAgentId, e);
            return CreditResult.fail("1001", "Internal error");
        }
    }

    // ===== ③④ Agent nạp Credit → Game Wallet (agent hoặc user) =====

    /**
     * Agent nạp Credit Wallet vào Game Wallet của target.
     * Target: agent cùng nhánh TĐL (③) hoặc user downline (④).
     * Tính như khoản nạp thông thường: auto-approve, nhận KM.
     *
     * Flow:
     *   1. Debit credit_wallet (atomic)
     *   2. MoneyGateway.creditUser(CREDIT_WALLET_DEPOSIT) → triggers KM
     *   3. VolumeTracking.addRequiredVolume
     *   4. Log credit_wallet_transactions
     *   5. Nếu MoneyGateway fail → refund credit_wallet (best-effort)
     *
     * @param isAgent true nếu target là agent, false nếu target là user
     */
    public static CreditResult depositToGameWallet(int fromAgentId, String fromNick,
                                                    long targetUserId, String targetNick,
                                                    boolean isAgent, long amount, String note) {
        try {
            if (amount <= 0 || amount > MAX_TRANSFER_AMOUNT) {
                return CreditResult.fail("4002", "Amount must be 1 to " + MAX_TRANSFER_AMOUNT);
            }

            CreditWalletDao dao = new CreditWalletDaoImpl();

            // Step 1: Debit credit_wallet (atomic)
            boolean debited = dao.debitBalance(fromAgentId, amount);
            if (!debited) return CreditResult.fail("4005", "Insufficient credit wallet balance");

            long fromBal = dao.getBalance(fromAgentId);

            // Step 2: Credit game wallet via MoneyGateway (triggers KM)
            String txId = "cw-" + fromAgentId + "-" + targetNick + "-" + System.currentTimeMillis();
            String desc = "Credit wallet deposit from agent " + fromNick;

            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    targetUserId, targetNick, amount, "CREDIT_WALLET_DEPOSIT", txId, desc);

            if (!cr.success) {
                // Step 5: Refund sender (best-effort)
                try {
                    dao.addBalance(fromAgentId, amount);
                    logger.warn("CreditWallet DEPOSIT: MoneyGateway failed, refunded sender=" + fromNick +
                            " amount=" + amount + " error=" + cr.error);
                } catch (Exception re) {
                    logger.error("CreditWallet REFUND FAILED after MoneyGateway fail: sender=" + fromNick, re);
                }
                return CreditResult.fail("1005", "Failed to credit game wallet — sender refunded");
            }

            // Step 2b: Create DepositTransaction so it appears in User's Deposit History
            if (!isAgent) {
                try {
                    com.vinplay.dal.dao.DepositTransactionDao depositDao = new com.vinplay.dal.dao.impl.DepositTransactionDaoImpl();
                    AgentInfo fromAgent = getAgentInfo(fromAgentId);
                    String agentCode = (fromAgent != null) ? fromAgent.code : "";

                    // INSERT trực tiếp với status=APPROVED + credit_status=CREDITED
                    // tránh 2 bước PENDING→APPROVED có thể fail do WHERE clause
                    long depositTxId = depositDao.createApprovedTransaction(
                            targetUserId, targetNick, amount, "CREDIT_WALLET",
                            fromNick, agentCode, "CreditWalletSystem", txId);

                    if (depositTxId <= 0) {
                        logger.warn("CreditWallet: createApprovedTransaction returned non-positive id=" + depositTxId
                                + " for user=" + targetNick + " txId=" + txId);
                    } else {
                        logger.info("CreditWallet: deposit_transactions record created id=" + depositTxId
                                + " user=" + targetNick + " amount=" + amount);
                    }
                } catch (Exception e) {
                    // Non-fatal: tiền đã vào game wallet. Chỉ log record thất bại.
                    logger.error("CreditWallet: Failed to log deposit_transactions for user=" + targetNick
                            + " amount=" + amount + " cause=" + e.getMessage(), e);
                }
            }

            // Step 3: VolumeTracking
            try {
                VolumeTrackingService.addRequiredVolume((int) targetUserId, targetNick, amount, 0);
                VolumeTrackingService.updateWithdrawalStatus((int) targetUserId);
            } catch (Exception volErr) {
                logger.warn("CreditWallet: VolumeTracking failed target=" + targetNick + ": " + volErr.getMessage());
            }

            // Step 4: Log
            String txType = isAgent ? "DEPOSIT_TO_AGENT" : "DEPOSIT_TO_USER";
            String noteStr = (note != null && !note.isEmpty()) ? note : desc;
            dao.logTransaction(fromAgentId, fromNick, txType, "DEBIT",
                    amount, fromBal, targetNick, null, noteStr);

            logger.info("CreditWallet DEPOSIT_TO_" + (isAgent ? "AGENT" : "USER") +
                    ": " + fromNick + " → " + targetNick +
                    " amount=" + amount + " promo=" + cr.promoApplied +
                    " bonus=" + cr.bonusAmount);

            CreditResult res = CreditResult.ok(fromBal);
            res.promoApplied = cr.promoApplied;
            res.bonusAmount = cr.bonusAmount;
            res.promoType = cr.promoType;
            return res;
        } catch (Exception e) {
            logger.error("CreditWalletService.depositToGameWallet error fromId=" + fromAgentId + " target=" + targetNick, e);
            return CreditResult.fail("1001", "Internal error");
        }
    }

    // ===== Validation Helpers =====

    /**
     * Kiểm tra 2 agent có phải là CẤP TRÊN - CẤP DƯỚI TRỰC TIẾP không.
     * TĐL -> ĐL1 (hợp lệ), ĐL1 -> TĐL (hợp lệ), ĐL2 -> ĐL1 (hợp lệ).
     * TĐL -> ĐL2 (không hợp lệ vì nhảy cóc cấp).
     */
    public static boolean isDirectUplineOrDownline(AgentInfo a, AgentInfo b) {
        if (a == null || b == null) return false;
        // Cha con trực tiếp: parentid của người này = id của người kia
        return a.parentid == b.id || b.parentid == a.id;
    }

    /**
     * Kiểm tra user có phải downline của agent không.
     * "Downline" = users.referral_code = agent.code
     */
    public static boolean isDownlineUser(String agentCode, String userNick) {
        String sql = "SELECT 1 FROM vinplay.users WHERE nick_name = ? AND referral_code = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userNick);
            ps.setString(2, agentCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.error("isDownlineUser error agentCode=" + agentCode + " user=" + userNick, e);
            return false;
        }
    }

    // ===== Agent/User lookups =====

    /** Lấy AgentInfo theo agent_id. Return null nếu không tìm thấy. */
    public static AgentInfo getAgentInfo(int agentId) {
        String sql = "SELECT id, nickname, code, level, ancestors, active, parentid " +
                     "FROM vinplay_admin.useragent WHERE id = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            return extractAgentInfo(ps);
        } catch (Exception e) {
            logger.error("getAgentInfo error agentId=" + agentId, e);
            return null;
        }
    }

    /** Lấy AgentInfo theo nickname. Return null nếu không tìm thấy. */
    public static AgentInfo getAgentInfoByNick(String nickname) {
        String sql = "SELECT id, nickname, code, level, ancestors, active, parentid " +
                     "FROM vinplay_admin.useragent WHERE nickname = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            return extractAgentInfo(ps);
        } catch (Exception e) {
            logger.error("getAgentInfoByNick error nick=" + nickname, e);
            return null;
        }
    }

    /** Lấy AgentInfo theo code (chỉ active=1). Return null nếu không tìm thấy. */
    public static AgentInfo getAgentInfoByCode(String code) {
        String sql = "SELECT id, nickname, code, level, ancestors, active, parentid " +
                     "FROM vinplay_admin.useragent WHERE code = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            return extractAgentInfo(ps);
        } catch (Exception e) {
            logger.error("getAgentInfoByCode error code=" + code, e);
            return null;
        }
    }

    private static AgentInfo extractAgentInfo(PreparedStatement ps) throws Exception {
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                AgentInfo info = new AgentInfo();
                info.id = rs.getInt("id");
                info.nickname = rs.getString("nickname");
                info.code = rs.getString("code");
                info.level = rs.getInt("level");
                info.ancestors = rs.getString("ancestors");
                if (info.ancestors == null) info.ancestors = "";
                info.active = rs.getInt("active");
                info.parentid = rs.getInt("parentid");
                return info;
            }
        }
        return null;
    }

    /**
     * Lấy user_id và vin (game wallet balance) từ vinplay.users.
     * @return long[]{userId, vin} hoặc null nếu không tìm thấy
     */
    public static long[] getUserIdAndVin(String nickname) {
        String sql = "SELECT id, vin FROM vinplay.users WHERE nick_name = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new long[]{rs.getLong("id"), rs.getLong("vin")};
                }
            }
        } catch (Exception e) {
            logger.error("getUserIdAndVin error nick=" + nickname, e);
        }
        return null;
    }

    // ===== Private Helpers =====

    /**
     * Lấy TĐL id (level=1) của một agent từ info.
     * - Bản thân là TĐL (level=1) → return agentId
     * - Parse ancestors "SA,TDL,DL1,..." → phần tử index 1 = TĐL id
     * - Return -1 nếu không xác định được
     */
    private static int getTdlId(int agentId, int level, String ancestors) {
        if (level == 1) return agentId;
        if (ancestors == null || ancestors.isEmpty()) return -1;
        String[] parts = ancestors.split(",");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Validate và consume OTP từ Hazelcast "cacheAgentOtp".
     * One-shot: OTP bị xóa sau khi validate thành công.
     * OTP format trong cache: "123456|timestamp" hoặc "123456"
     *
     * @return null nếu valid, error message nếu invalid
     */
    public static String validateAndConsumeOtp(String agentCode, String otpInput) {
        if (otpInput == null || otpInput.isEmpty()) {
            return "OTP required — please call c=9464 first";
        }
        try {
            com.vinplay.vbee.common.cache.DistCache<String, String> otpMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheAgentOtp", String.class);
            String cached = otpMap.get(agentCode);
            if (cached == null) return "OTP expired or not sent";
            // OTP có thể stored dạng "otp|metadata"
            String cachedOtp = cached.split("\\|")[0];
            if (!cachedOtp.equals(otpInput)) return "OTP incorrect";
            otpMap.remove(agentCode); // one-shot consume
            return null; // valid
        } catch (Exception e) {
            logger.error("OTP validation error agentCode=" + agentCode, e);
            return "OTP validation failed";
        }
    }
}

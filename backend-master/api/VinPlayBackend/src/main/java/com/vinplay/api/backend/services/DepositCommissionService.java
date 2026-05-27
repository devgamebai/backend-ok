package com.vinplay.api.backend.services;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Service tính và ghi hoa hồng nạp tiền (Deposit Commission).
 *
 * Logic (Differential Commission - chênh lệch %):
 *   Khi user nạp X VND, hệ thống duyệt chuỗi ancestors của đại lý:
 *     ĐL2 (deposit_rate=1%) -> ĐL1 (deposit_rate=3%) -> TĐL (deposit_rate=5%)
 *   ĐL2  nhận: X * (1% - 0%)  = X * 1%   (0% = rate user = 0)
 *   ĐL1  nhận: X * (3% - 1%)  = X * 2%
 *   TĐL  nhận: X * (5% - 3%)  = X * 2%
 *
 * Gọi tại: Luồng duyệt Nạp thành công (ApproveDeposit, CryptoDepositEvent, GatewayCallback...)
 */
public class DepositCommissionService {

    private static final Logger logger = Logger.getLogger("backend");

    /**
     * Điểm vào chính: xử lý hoa hồng khi user nạp thành công.
     *
     * @param sourceNickname nickname của user vừa nạp
     * @param depositAmount  số tiền nạp (đơn vị giống currency của hệ thống: VIN)
     * @param transactionId  mã giao dịch nạp (để idempotency check)
     */
    public static void processDeposit(String sourceNickname, long depositAmount, String transactionId) {
        if (sourceNickname == null || sourceNickname.isEmpty() || depositAmount <= 0) return;

        try {
            // Idempotency: nếu transaction_id đã xử lý, bỏ qua
            if (isAlreadyProcessed(transactionId)) {
                logger.info("[DepositCommission] txnId already processed: " + transactionId);
                return;
            }

            // 1. Lấy thông tin user + referral_code
            UserDepositInfo userInfo = getUserInfo(sourceNickname);
            if (userInfo == null || userInfo.referralCode == null || userInfo.referralCode.isEmpty()) {
                logger.info("[DepositCommission] user=" + sourceNickname + " has no referral agent, skip.");
                return;
            }

            // 2. Xây dựng chuỗi agent (direct -> ancestor)
            List<AgentNode> chain = buildAgentChain(userInfo.referralCode);
            if (chain.isEmpty()) {
                logger.info("[DepositCommission] agent chain empty for code=" + userInfo.referralCode);
                return;
            }

            // 3. Tính và ghi hoa hồng chênh lệch cho từng cấp
            double prevChildRate = 0; // rate của cấp dưới trực tiếp (user = 0%)
            for (AgentNode agent : chain) {
                // BUG FIX: dùng epsilon để so sánh double tránh floating point issues
                double diffRate = agent.depositRate - prevChildRate;
                if (diffRate < 0.001) { // nhỏ hơn 0.001% thì bỏ qua
                    prevChildRate = agent.depositRate;
                    continue;
                }
                long commissionAmount = Math.round(depositAmount * diffRate / 100.0);
                if (commissionAmount <= 0) {
                    prevChildRate = agent.depositRate;
                    continue;
                }

                // BUG FIX: Ghi log PENDING trước, cộng tiền sau.
                // Nếu credit thất bại, log vẫn ở PENDING để retry/audit sau.
                // Nếu ghi log thất bại (exception), sẽ throw ra ngoài và toàn bộ
                // giao dịch không được thực hiện (atomicity per-agent).
                insertCommissionLog(
                    agent.id, agent.nickname, agent.level,
                    userInfo.userId, sourceNickname,
                    transactionId, depositAmount,
                    agent.depositRate, prevChildRate, diffRate,
                    commissionAmount, "PENDING"
                );

                // Cộng tiền vào ví agent
                boolean paid = creditAgentWallet(agent.nickname, commissionAmount, transactionId);
                if (paid) {
                    // Cập nhật status thành PAID
                    updateCommissionLogStatus(transactionId, agent.id, "PAID");
                } else {
                    logger.error("[DepositCommission] FAILED to credit agent=" + agent.nickname
                        + " amount=" + commissionAmount + " txn=" + transactionId
                        + " -> log remains PENDING for retry");
                }

                logger.info("[DepositCommission] agent=" + agent.nickname
                    + " level=" + agent.level
                    + " ownRate=" + agent.depositRate
                    + " childRate=" + prevChildRate
                    + " diffRate=" + diffRate
                    + " commission=" + commissionAmount
                    + " paid=" + paid);

                prevChildRate = agent.depositRate;
            }

        } catch (Exception e) {
            logger.error("[DepositCommission] Error processing deposit for user=" + sourceNickname + 
                " txn=" + transactionId, e);
        }
    }

    // ======================================================
    // Private Helpers
    // ======================================================

    private static boolean isAlreadyProcessed(String transactionId) throws Exception {
        // BUG FIX: null transactionId should be treated as "not processed" but we must
        // guard against processing the same txn twice. If transactionId is null/empty,
        // return false (will allow processing, but insertLog will store empty string).
        if (transactionId == null || transactionId.isEmpty()) return false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM vinplay_admin.deposit_commission_logs WHERE transaction_id = ? LIMIT 1")) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static UserDepositInfo getUserInfo(String nickname) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, referral_code FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserDepositInfo info = new UserDepositInfo();
                    info.userId = rs.getLong("id");
                    info.referralCode = rs.getString("referral_code");
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Duyệt chuỗi agent từ direct -> ancestors.
     * Trả về agents theo thứ tự: [direct_agent (ĐL2), parent (ĐL1), grandparent (TĐL), ...]
     *
     * BUG FIX: ancestors field chứa TOÀN BỘ path kể cả ID của chính directAgent.
     * Ví dụ: TĐL(id=1) -> ĐL1(id=5) -> ĐL2(id=12), ancestors của ĐL2 = "1,5,12"
     * Phải loại bỏ id=12 khỏi danh sách khi lấy parents.
     *
     * BUG FIX: Không dùng string concatenation cho IN clause vì ancestors đã được
     * validate bằng regex ^[0-9,]+$ nên an toàn, nhưng phải đảm bảo trim() đúng.
     */
    private static List<AgentNode> buildAgentChain(String referralCode) throws Exception {
        List<AgentNode> chain = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {

            // Lấy direct agent bằng referral_code
            AgentNode directAgent = null;
            String ancestorsStr = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, level, IFNULL(ancestors, '') as ancestors, IFNULL(deposit_rate, 0) AS deposit_rate " +
                    "FROM vinplay_admin.useragent WHERE code = ? LIMIT 1")) {
                ps.setString(1, referralCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        directAgent = new AgentNode();
                        directAgent.id = rs.getInt("id");
                        directAgent.nickname = rs.getString("nickname");
                        directAgent.level = rs.getInt("level");
                        directAgent.depositRate = rs.getDouble("deposit_rate");
                        ancestorsStr = rs.getString("ancestors");
                    }
                }
            }
            if (directAgent == null) return chain;
            chain.add(directAgent);

            // Duyệt ancestors (cha, ông, ...) theo ancestors path
            if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
                // ancestors format: "1,5,12" hoặc "1.5.12" - normalize về comma
                String normalized = ancestorsStr.trim().replaceAll("\\.", ",");
                // BUG FIX: strict validation để chống SQL injection
                if (!normalized.matches("^[0-9]+(,[0-9]+)*$")) {
                    logger.warn("[DepositCommission] Invalid ancestors format, skip parents: " + ancestorsStr);
                    return chain;
                }
                // Loại bỏ id của direct agent khỏi ancestors list
                String[] parts = normalized.split(",");
                List<String> parentIds = new ArrayList<>();
                for (String p : parts) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty() && !trimmed.equals(String.valueOf(directAgent.id))) {
                        parentIds.add(trimmed);
                    }
                }
                if (!parentIds.isEmpty()) {
                    // Safe: inClause chỉ chứa integers đã validate
                    String inClause = String.join(",", parentIds);
                    // ORDER BY level DESC: ĐL1 (level=2) trước TĐL (level=1) => closest to direct first
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id, nickname, level, IFNULL(deposit_rate, 0) AS deposit_rate " +
                            "FROM vinplay_admin.useragent WHERE id IN (" + inClause + ") ORDER BY level DESC")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                AgentNode node = new AgentNode();
                                node.id = rs.getInt("id");
                                node.nickname = rs.getString("nickname");
                                node.level = rs.getInt("level");
                                node.depositRate = rs.getDouble("deposit_rate");
                                chain.add(node);
                            }
                        }
                    }
                }
            }
        }
        return chain;
    }

    private static void insertCommissionLog(
            int agentId, String agentNickname, int agentLevel,
            long sourceUserId, String sourceNickname,
            String transactionId, long depositAmount,
            double ownRate, double childRate, double diffRate,
            long commissionAmount, String status) throws Exception {

        String sql = "INSERT INTO vinplay_admin.deposit_commission_logs " +
            "(agent_id, agent_nickname, agent_level, source_user_id, source_nickname, " +
            "transaction_id, deposit_amount, own_rate, child_rate, diff_rate, commission_amount, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setString(2, agentNickname);
            ps.setInt(3, agentLevel);
            ps.setLong(4, sourceUserId);
            ps.setString(5, sourceNickname);
            ps.setString(6, transactionId);
            ps.setLong(7, depositAmount);
            ps.setDouble(8, ownRate);
            ps.setDouble(9, childRate);
            ps.setDouble(10, diffRate);
            ps.setLong(11, commissionAmount);
            ps.setString(12, status);
            ps.executeUpdate();
        }
    }

    /**
     * Cộng tiền vào ví agent — delegates to the canonical agency_wallet table
     * via RebateService.creditAgencyWallet() instead of the redundant
     * useragent.wallet_balance column (Phase 5 cleanup, SUN-735).
     */
    private static boolean creditAgentWallet(String agentNickname, long amount, String txnId) {
        try {
            int agentId = com.vinplay.dal.rebate.RebateService.getAgentIdByNickname(agentNickname);
            if (agentId <= 0) {
                logger.error("[DepositCommission] creditAgentWallet: agent not found for " + agentNickname);
                return false;
            }
            return com.vinplay.dal.rebate.RebateService.creditAgencyWallet(agentId, amount,
                    "COMMISSION_DEPOSIT", null, "DepositApprove",
                    "Deposit commission txn=" + txnId);
        } catch (Exception e) {
            logger.error("[DepositCommission] creditAgentWallet failed for " + agentNickname, e);
            return false;
        }
    }

    /**
     * Cập nhật trạng thái log sau khi ghi tiền thành công.
     * Dùng (transaction_id + agent_id) để định danh chính xác bản ghi cần update.
     */
    private static void updateCommissionLogStatus(String transactionId, int agentId, String status) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE vinplay_admin.deposit_commission_logs SET status = ? " +
                 "WHERE transaction_id = ? AND agent_id = ? AND status = 'PENDING'")) {
            ps.setString(1, status);
            ps.setString(2, transactionId);
            ps.setInt(3, agentId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("[DepositCommission] updateCommissionLogStatus failed txn=" + transactionId + " agentId=" + agentId, e);
        }
    }

    // ======================================================
    // Inner Classes
    // ======================================================

    private static class UserDepositInfo {
        long userId;
        String referralCode;
    }

    private static class AgentNode {
        int id;
        String nickname;
        int level;
        double depositRate;
    }
}

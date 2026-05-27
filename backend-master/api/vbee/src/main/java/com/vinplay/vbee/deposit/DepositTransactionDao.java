package com.vinplay.vbee.deposit;

import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Data access for the deposit_transactions table.
 * Uses the existing VBee ConnectionPool to get MySQL connections.
 *
 * Table: vinplay.deposit_transactions
 * Columns:
 *   id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   tx_code VARCHAR(50) UNIQUE,
 *   user_id INT,
 *   nick_name VARCHAR(50),
 *   amount BIGINT,
 *   bank_name VARCHAR(100),
 *   bank_number VARCHAR(50),
 *   status ENUM('PENDING','PROCESSING','APPROVED','REJECTED','EXPIRED') DEFAULT 'PENDING',
 *   locked_by VARCHAR(100),
 *   lock_platform VARCHAR(20),
 *   reject_reason VARCHAR(255),
 *   credit_status ENUM('NONE','OK','FAILED') DEFAULT 'NONE',
 *   telegram_msg_id BIGINT,
 *   telegram_chat_id VARCHAR(50),
 *   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
 *   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 */
public class DepositTransactionDao {

    private static final Logger logger = Logger.getLogger("api");
    private static final String POOL_NAME = "mysqlpoolname";

    // -----------------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------------

    public static class DepositTx {
        public long id;
        public String txCode;
        public int userId;
        public String nickName;
        public long amount;
        public String bankName;
        public String bankNumber;
        public String status;
        public String operatorName;
        public String operatorSource;
        public String rejectReason;
        public String creditStatus;
        public int forceApproved;
        public String forceApprovedBy;
        public java.sql.Timestamp forceApprovedAt;
        public long telegramMsgId;
        public String telegramChatId;
        public java.sql.Timestamp createdAt;
        public java.sql.Timestamp updatedAt;
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /**
     * Get a single transaction by ID.
     */
    public DepositTx getTransaction(long txId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "SELECT * FROM deposit_transactions WHERE id = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, txId);
            ResultSet rs = stm.executeQuery();
            DepositTx tx = null;
            if (rs.next()) {
                tx = mapRow(rs);
            }
            rs.close();
            stm.close();
            return tx;
        } catch (SQLException e) {
            logger.error("getTransaction error for txId=" + txId, e);
            return null;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Get orphaned PENDING transactions (no telegram_msg_id, less than maxAgeMinutes old).
     */
    public List<DepositTx> getOrphanedPending(int maxAgeMinutes) {
        Connection conn = null;
        List<DepositTx> result = new ArrayList<DepositTx>();
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "SELECT * FROM deposit_transactions "
                    + "WHERE status = 'PENDING' AND telegram_msg_id IS NULL "
                    + "AND created_at > DATE_SUB(NOW(), INTERVAL ? MINUTE) "
                    + "ORDER BY created_at ASC";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, maxAgeMinutes);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            rs.close();
            stm.close();
        } catch (SQLException e) {
            logger.error("getOrphanedPending error", e);
        } finally {
            releaseConnection(conn);
        }
        return result;
    }

    /**
     * Get transactions with failed credit status.
     */
    public List<DepositTx> getFailedCredits() {
        Connection conn = null;
        List<DepositTx> result = new ArrayList<DepositTx>();
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "SELECT * FROM deposit_transactions "
                    + "WHERE status = 'APPROVED' AND credit_status IN ('CREDIT_FAILED','FAILED') "
                    + "ORDER BY created_at ASC LIMIT 50";
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            rs.close();
            stm.close();
        } catch (SQLException e) {
            logger.error("getFailedCredits error", e);
        } finally {
            releaseConnection(conn);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Write operations (atomic via WHERE status checks)
    // -----------------------------------------------------------------------

    /**
     * Set the Telegram message ID for a transaction.
     */
    public boolean setTelegramMessageId(long txId, long msgId, String chatId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions SET telegram_msg_id = ?, telegram_chat_id = ? WHERE id = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, msgId);
            stm.setString(2, chatId);
            stm.setLong(3, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("setTelegramMessageId error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Atomically pick a PENDING transaction. Sets status=PROCESSING.
     * Returns true if the update succeeded (row was PENDING).
     */
    public boolean pickTransaction(long txId, String operatorName, String source) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions "
                    + "SET status = 'PROCESSING', locked_by = ?, lock_platform = ? "
                    + "WHERE id = ? AND status = 'PENDING'";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, operatorName);
            stm.setString(2, source);
            stm.setLong(3, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("pickTransaction error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Atomically approve a PROCESSING transaction. Sets status=APPROVED.
     */
    public boolean approveTransaction(long txId, String operatorName, long amount) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions "
                    + "SET status = 'APPROVED', locked_by = ?, credit_status = 'PENDING_CREDIT', "
                    + "processed_by = ?, processed_at = NOW() "
                    + "WHERE id = ? AND status = 'PROCESSING'";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, operatorName);
            stm.setString(2, operatorName);
            stm.setLong(3, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("approveTransaction error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Atomically reject a PENDING or PROCESSING transaction. Sets status=REJECTED.
     */
    public boolean rejectTransaction(long txId, String operatorName, String reason) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions "
                    + "SET status = 'REJECTED', locked_by = ?, reject_reason = ? "
                    + "WHERE id = ? AND status IN ('PENDING', 'PROCESSING')";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, operatorName);
            stm.setString(2, reason);
            stm.setLong(3, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("rejectTransaction error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Release a PROCESSING transaction back to PENDING.
     */
    public boolean releaseTransaction(long txId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions "
                    + "SET status = 'PENDING', locked_by = NULL, lock_platform = NULL "
                    + "WHERE id = ? AND status = 'PROCESSING'";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("releaseTransaction error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Expire PENDING transactions older than maxAgeMinutes.
     * Returns the list of expired transactions (for Telegram message updates).
     */
    public List<DepositTx> expireOldTransactions(int maxAgeMinutes) {
        Connection conn = null;
        List<DepositTx> expired = new ArrayList<DepositTx>();
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);

            // First, fetch the ones that will be expired
            String selectSql = "SELECT * FROM deposit_transactions "
                    + "WHERE status = 'PENDING' "
                    + "AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            PreparedStatement selectStm = conn.prepareStatement(selectSql);
            selectStm.setInt(1, maxAgeMinutes);
            ResultSet rs = selectStm.executeQuery();
            while (rs.next()) {
                expired.add(mapRow(rs));
            }
            rs.close();
            selectStm.close();

            if (!expired.isEmpty()) {
                // Now expire them
                String updateSql = "UPDATE deposit_transactions "
                        + "SET status = 'EXPIRED' "
                        + "WHERE status = 'PENDING' "
                        + "AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
                PreparedStatement updateStm = conn.prepareStatement(updateSql);
                updateStm.setInt(1, maxAgeMinutes);
                int rows = updateStm.executeUpdate();
                updateStm.close();
                logger.info("Expired " + rows + " old PENDING deposit transactions");
            }
        } catch (SQLException e) {
            logger.error("expireOldTransactions error", e);
        } finally {
            releaseConnection(conn);
        }
        return expired;
    }

    /**
     * Release stale PROCESSING transactions (lock expired).
     */
    public List<DepositTx> releaseStaleProcessing(int maxAgeMinutes) {
        Connection conn = null;
        List<DepositTx> stale = new ArrayList<DepositTx>();
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);

            String selectSql = "SELECT * FROM deposit_transactions "
                    + "WHERE status = 'PROCESSING' "
                    + "AND updated_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            PreparedStatement selectStm = conn.prepareStatement(selectSql);
            selectStm.setInt(1, maxAgeMinutes);
            ResultSet rs = selectStm.executeQuery();
            while (rs.next()) {
                stale.add(mapRow(rs));
            }
            rs.close();
            selectStm.close();

            if (!stale.isEmpty()) {
                String updateSql = "UPDATE deposit_transactions "
                        + "SET status = 'PENDING', locked_by = NULL, lock_platform = NULL "
                        + "WHERE status = 'PROCESSING' "
                        + "AND updated_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
                PreparedStatement updateStm = conn.prepareStatement(updateSql);
                updateStm.setInt(1, maxAgeMinutes);
                int rows = updateStm.executeUpdate();
                updateStm.close();
                logger.info("Released " + rows + " stale PROCESSING deposit transactions");
            }
        } catch (SQLException e) {
            logger.error("releaseStaleProcessing error", e);
        } finally {
            releaseConnection(conn);
        }
        return stale;
    }

    /**
     * Mark a transaction's credit as failed (for retry by recovery worker).
     */
    public boolean markCreditFailed(long txId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions SET credit_status = 'CREDIT_FAILED' WHERE id = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("markCreditFailed error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Mark credit as OK after successful balance update.
     */
    public boolean markCreditOk(long txId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions SET credit_status = 'CREDITED' WHERE id = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("markCreditOk error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Credit user balance using the existing stored procedure.
     * This calls the vinplay.update_money_from_admin procedure which is used
     * by the admin panel for manual balance adjustments.
     */
    public boolean creditUserBalance(int userId, long amount, String txCode) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            // Use stored procedure for atomic balance credit
            // update_money_from_admin(userId, amount, moneyType, actionName, description)
            PreparedStatement stm = conn.prepareStatement(
                    "CALL update_money_from_admin(?, ?, 'vin', 'DepositApprove', ?)");
            stm.setInt(1, userId);
            stm.setLong(2, amount);
            stm.setString(3, "Deposit " + txCode);
            stm.executeUpdate();
            stm.close();
            return true;
        } catch (SQLException e) {
            logger.error("creditUserBalance error for userId=" + userId + " amount=" + amount, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Atomically force approve an EXPIRED or REJECTED transaction.
     * Sets status=APPROVED, force_approved=1, and records the operator.
     */
    public boolean forceApproveTransaction(long txId, String operatorName, long amount) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "UPDATE deposit_transactions "
                    + "SET status = 'APPROVED', locked_by = ?, credit_status = 'PENDING_CREDIT', "
                    + "processed_by = ?, processed_at = NOW(), "
                    + "force_approved = 1, force_approved_by = ?, force_approved_at = NOW() "
                    + "WHERE id = ? AND status IN ('EXPIRED', 'REJECTED')";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, operatorName);
            stm.setString(2, operatorName);
            stm.setString(3, operatorName);
            stm.setLong(4, txId);
            int rows = stm.executeUpdate();
            stm.close();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("forceApproveTransaction error for txId=" + txId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Insert an audit log entry for deposit actions.
     */
    public void insertAuditLog(long txId, String txCode, String action, String operatorName,
            String platform, String detail) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            String sql = "INSERT INTO deposit_audit_logs (tx_id, tx_code, action, operator_id, "
                    + "operator_name, platform, detail, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, txId);
            stm.setString(2, txCode);
            stm.setString(3, action);
            stm.setString(4, operatorName);
            stm.setString(5, operatorName);
            stm.setString(6, platform);
            stm.setString(7, detail);
            stm.executeUpdate();
            stm.close();
        } catch (SQLException e) {
            logger.error("insertAuditLog error for txId=" + txId, e);
        } finally {
            releaseConnection(conn);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private DepositTx mapRow(ResultSet rs) throws SQLException {
        DepositTx tx = new DepositTx();
        tx.id = rs.getLong("id");
        tx.txCode = rs.getString("tx_code");
        tx.userId = rs.getInt("user_id");
        tx.nickName = rs.getString("nick_name");
        tx.amount = rs.getLong("amount");
        tx.bankName = rs.getString("user_bank_name");
        tx.bankNumber = rs.getString("user_bank_number");
        tx.status = rs.getString("status");
        tx.operatorName = rs.getString("locked_by");
        tx.operatorSource = rs.getString("lock_platform");
        tx.rejectReason = rs.getString("reject_reason");
        tx.creditStatus = rs.getString("credit_status");
        tx.forceApproved = rs.getInt("force_approved");
        tx.forceApprovedBy = rs.getString("force_approved_by");
        tx.forceApprovedAt = rs.getTimestamp("force_approved_at");
        tx.telegramMsgId = rs.getLong("telegram_msg_id");
        tx.telegramChatId = rs.getString("telegram_chat_id");
        tx.createdAt = rs.getTimestamp("created_at");
        tx.updatedAt = rs.getTimestamp("updated_at");
        return tx;
    }

    private void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch (Exception e) {
                logger.error("Error releasing connection", e);
            }
        }
    }
}

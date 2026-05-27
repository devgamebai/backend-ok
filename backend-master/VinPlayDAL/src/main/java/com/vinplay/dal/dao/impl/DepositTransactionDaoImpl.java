package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.vbee.common.pools.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class DepositTransactionDaoImpl implements DepositTransactionDao {

    private static final Logger logger = Logger.getLogger("api");

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<String, Object>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = meta.getColumnLabel(i);
            row.put(colName, rs.getObject(i));
        }
        return row;
    }

    @Override
    public long createTransaction(long userId, String nickName, long amount, String depositType,
            String userBankName, String userBankNumber, String userBankCode,
            int companyBankId, String idempotencyKey) throws SQLException {
        String sql = "INSERT INTO deposit_transactions (user_id, nick_name, amount, deposit_type, " +
                "user_bank_name, user_bank_number, user_bank_code, company_bank_id, " +
                "idempotency_key, status, credit_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 'NONE', NOW(), NOW())";
        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setLong(3, amount);
            stm.setString(4, depositType);
            stm.setString(5, userBankName);
            stm.setString(6, userBankNumber);
            stm.setString(7, userBankCode);
            stm.setInt(8, companyBankId);
            stm.setString(9, idempotencyKey);
            int affected = stm.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = stm.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }
        }
        if (generatedId > 0) {
            String txCode = generateTxCode(generatedId);
            String updateSql = "UPDATE deposit_transactions SET tx_code = ? WHERE id = ?";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement(updateSql)) {
                stm.setString(1, txCode);
                stm.setLong(2, generatedId);
                stm.executeUpdate();
            }
        }
        return generatedId;
    }

    @Override
    public long createApprovedTransaction(long userId, String nickName, long amount, String depositType,
            String agentNick, String agentCode, String operatorName, String idempotencyKey) throws SQLException {
        // INSERT trực tiếp với status=APPROVED + credit_status=CREDITED — 1 bước
        // user_bank_name = agentNick (tên ĐL), user_bank_code = agentCode (mã ĐL)
        String sql = "INSERT INTO deposit_transactions (user_id, nick_name, amount, deposit_type, " +
                "user_bank_name, user_bank_number, user_bank_code, company_bank_id, " +
                "idempotency_key, status, credit_status, amount_vin, " +
                "processed_by, processed_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 'APPROVED', 'CREDITED', ?, ?, NOW(), NOW(), NOW())";
        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setLong(3, amount);
            stm.setString(4, depositType);
            stm.setString(5, agentNick != null ? agentNick : "");   // user_bank_name = agent nickname
            stm.setString(6, "");                                    // user_bank_number (không dùng)
            stm.setString(7, agentCode != null ? agentCode : "");   // user_bank_code = agent code
            stm.setString(8, idempotencyKey);
            stm.setLong(9, amount);                                  // amount_vin = amount
            stm.setString(10, operatorName);                         // processed_by
            int affected = stm.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = stm.getGeneratedKeys()) {
                    if (rs.next()) generatedId = rs.getLong(1);
                }
            }
        }
        if (generatedId > 0) {
            String txCode = generateTxCode(generatedId);
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement(
                         "UPDATE deposit_transactions SET tx_code = ? WHERE id = ?")) {
                stm.setString(1, txCode);
                stm.setLong(2, generatedId);
                stm.executeUpdate();
            }
        }
        return generatedId;
    }

    @Override
    public Map<String, Object> getTransaction(long txId) throws SQLException {
        Map<String, Object> result = null;
        String sql = "SELECT * FROM deposit_transactions WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, txId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result = rowToMap(rs);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getTransactionByCode(String txCode) throws SQLException {
        Map<String, Object> result = null;
        String sql = "SELECT * FROM deposit_transactions WHERE tx_code = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, txCode);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result = rowToMap(rs);
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTransactionsByStatus(String status, int page, int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        int offset = (page - 1) * limit;
        boolean hasStatus = status != null && !status.isEmpty();

        // UNION bank deposits + crypto deposits
        String bankSql = "SELECT id, tx_code, user_id, nick_name, amount, " +
                "CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                "deposit_type, status, credit_status, " +
                "user_bank_name, user_bank_number, user_bank_code, company_bank_id, " +
                "'' AS address, '' AS tx_hash, " +
                "locked_by, locked_at, lock_platform, " +
                "processed_by, processed_at, reject_reason, " +
                "force_approved, force_approved_by, force_approved_at, " +
                "created_at, updated_at " +
                "FROM deposit_transactions" + (hasStatus ? " WHERE status = ?" : "");

        String cryptoSql = "SELECT id, IFNULL(gateway_tx_id, CONCAT('CRYPTO-',id)) AS tx_code, " +
                "user_id, nick_name, amount_krw AS amount, amount_usdt, " +
                "'CRYPTO' AS deposit_type, status, '' AS credit_status, " +
                "'' AS user_bank_name, '' AS user_bank_number, '' AS user_bank_code, 0 AS company_bank_id, " +
                "address, IFNULL(tx_hash,'') AS tx_hash, " +
                "NULL AS locked_by, NULL AS locked_at, NULL AS lock_platform, " +
                "'' AS processed_by, NULL AS processed_at, '' AS reject_reason, " +
                "0 AS force_approved, NULL AS force_approved_by, NULL AS force_approved_at, " +
                "created_at, created_at AS updated_at " +
                "FROM crypto_deposits" + (hasStatus ? " WHERE status = ?" : "");

        String sql = "SELECT * FROM (" + bankSql + " UNION ALL " + cryptoSql +
                ") _t ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasStatus) stm.setString(idx++, status);  // bank WHERE
            if (hasStatus) stm.setString(idx++, status);  // crypto WHERE
            stm.setInt(idx++, limit);
            stm.setInt(idx++, offset);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getUserTransactions(long userId, int page, int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        // SUN-601 + SUN-85x: JOIN admin_banks (the table admin CMS + admin-next
        // actually manage) instead of the abandoned company_banks seed.
        // admin_banks.customer_name is the account-holder field; alias it
        // to cb_account_holder so the downstream map keys stay unchanged.
        // 2026-05-12: also JOIN banks via admin_banks.bank_id FK so the
        // canonical Korean bank_name flows through (not the stored short code).
        // 2026-05-13: bank_name combined as "Korean (CODE)".
        String sql = "SELECT dt.*, " +
                "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                "     ELSE cb.bank_name END AS cb_bank_name, " +
                "cb.bank_number AS cb_bank_number, cb.customer_name AS cb_account_holder " +
                "FROM deposit_transactions dt " +
                "LEFT JOIN admin_banks cb ON cb.id = dt.company_bank_id " +
                "LEFT JOIN banks b ON b.id = cb.bank_id " +
                "WHERE dt.user_id = ? ORDER BY dt.created_at DESC LIMIT ? OFFSET ?";
        int offset = (page - 1) * limit;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setInt(2, limit);
            stm.setInt(3, offset);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = rowToMap(rs);
                    // Add company bank fields from JOIN
                    row.put("cb_bank_name", rs.getString("cb_bank_name"));
                    row.put("cb_bank_number", rs.getString("cb_bank_number"));
                    row.put("cb_account_holder", rs.getString("cb_account_holder"));
                    results.add(row);
                }
            }
        }
        return results;
    }

    @Override
    public int countUserPendingTransactions(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM deposit_transactions WHERE user_id = ? AND status = 'PENDING'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean pickTransaction(long txId, String operatorName, String platform) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'PROCESSING', locked_by = ?, " +
                "locked_at = NOW(), lock_platform = ?, updated_at = NOW() " +
                "WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, operatorName);
            stm.setString(2, platform);
            stm.setLong(3, txId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean approveTransaction(long txId, String operatorName, long amountVin) throws SQLException {
        // Allow approve from PENDING (direct) or PROCESSING (locked by this admin)
        String sql = "UPDATE deposit_transactions SET status = 'APPROVED', processed_by = ?, " +
                "processed_at = NOW(), amount_vin = ?, credit_status = 'PENDING_CREDIT', updated_at = NOW() " +
                "WHERE id = ? AND (status = 'PENDING' OR (status = 'PROCESSING' AND locked_by = ?))";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, operatorName);
            stm.setLong(2, amountVin);
            stm.setLong(3, txId);
            stm.setString(4, operatorName);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean rejectTransaction(long txId, String operatorName, String reason) throws SQLException {
        // Allow reject from PENDING (not yet picked up) or PROCESSING (locked by this admin)
        String sql = "UPDATE deposit_transactions SET status = 'REJECTED', processed_by = ?, " +
                "processed_at = NOW(), reject_reason = ?, updated_at = NOW() " +
                "WHERE id = ? AND (status = 'PENDING' OR (status = 'PROCESSING' AND locked_by = ?))";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, operatorName);
            stm.setString(2, reason);
            stm.setLong(3, txId);
            stm.setString(4, operatorName);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean forceApproveTransaction(long txId, String operatorName, long amountVin) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'APPROVED', processed_by = ?, " +
                "processed_at = NOW(), amount_vin = ?, credit_status = 'PENDING_CREDIT', " +
                "force_approved = 1, force_approved_by = ?, force_approved_at = NOW(), " +
                "updated_at = NOW() " +
                "WHERE id = ? AND status IN ('EXPIRED', 'REJECTED')";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, operatorName);
            stm.setLong(2, amountVin);
            stm.setString(3, operatorName);
            stm.setLong(4, txId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cancelTransaction(long txId, long userId) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'CANCELLED', updated_at = NOW() " +
                "WHERE id = ? AND user_id = ? AND status = 'PENDING'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, txId);
            stm.setLong(2, userId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public int expireOldTransactions(int minutesThreshold) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'EXPIRED', updated_at = NOW() " +
                "WHERE status = 'PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE) LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, minutesThreshold);
            return stm.executeUpdate();
        }
    }

    @Override
    public List<Map<String, Object>> getOrphanedPending(int minutesThreshold) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        String sql = "SELECT * FROM deposit_transactions WHERE status = 'PENDING' " +
                "AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE) ORDER BY created_at ASC LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, minutesThreshold);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public int releaseStaleProcessing(int minutesThreshold) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'PENDING', locked_by = NULL, " +
                "locked_at = NULL, lock_platform = NULL, updated_at = NOW() " +
                "WHERE status = 'PROCESSING' AND locked_at < DATE_SUB(NOW(), INTERVAL ? MINUTE) LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, minutesThreshold);
            return stm.executeUpdate();
        }
    }

    @Override
    public List<Map<String, Object>> getFailedCredits() throws SQLException {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        String sql = "SELECT * FROM deposit_transactions WHERE status = 'APPROVED' " +
                "AND credit_status = 'CREDIT_FAILED' ORDER BY processed_at ASC LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public boolean updateCreditStatus(long txId, String creditStatus) throws SQLException {
        String sql = "UPDATE deposit_transactions SET credit_status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, creditStatus);
            stm.setLong(2, txId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean setTelegramMessageId(long txId, long messageId, String chatId) throws SQLException {
        String sql = "UPDATE deposit_transactions SET telegram_message_id = ?, telegram_chat_id = ?, " +
                "updated_at = NOW() WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, messageId);
            stm.setString(2, chatId);
            stm.setLong(3, txId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean unlockTransaction(long txId) throws SQLException {
        String sql = "UPDATE deposit_transactions SET status = 'PENDING', locked_by = NULL, " +
                "locked_at = NULL, lock_platform = NULL, updated_at = NOW() " +
                "WHERE id = ? AND status = 'PROCESSING'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, txId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public void insertAuditLog(long txId, String txCode, String action, String operatorId,
            String operatorName, String platform, String detail) throws SQLException {
        String sql = "INSERT INTO deposit_audit_logs (tx_id, tx_code, action, operator_id, " +
                "operator_name, platform, detail, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, txId);
            stm.setString(2, txCode);
            stm.setString(3, action);
            stm.setString(4, operatorId);
            stm.setString(5, operatorName);
            stm.setString(6, platform);
            stm.setString(7, detail);
            stm.executeUpdate();
        }
    }

    @Override
    public String generateTxCode(long txId) throws SQLException {
        return com.vinplay.dal.utils.TxCodeGenerator.bankDeposit();
    }
}

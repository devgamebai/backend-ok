package com.vinplay.dal.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DepositTransactionDao {

    long createTransaction(long userId, String nickName, long amount, String depositType,
        String userBankName, String userBankNumber, String userBankCode,
        int companyBankId, String idempotencyKey) throws SQLException;

    /**
     * INSERT 1 bước với status=APPROVED, credit_status=CREDITED.
     * Dùng cho Credit Wallet deposit — tiền đã credit xong, không cần workflow duyệt.
     */
    long createApprovedTransaction(long userId, String nickName, long amount, String depositType,
        String agentNick, String agentCode, String operatorName, String idempotencyKey) throws SQLException;

    Map<String, Object> getTransaction(long txId) throws SQLException;

    Map<String, Object> getTransactionByCode(String txCode) throws SQLException;

    List<Map<String, Object>> getTransactionsByStatus(String status, int page, int limit) throws SQLException;

    List<Map<String, Object>> getUserTransactions(long userId, int page, int limit) throws SQLException;

    int countUserPendingTransactions(long userId) throws SQLException;

    // Atomic status transitions (WHERE clause checks current status)
    boolean pickTransaction(long txId, String operatorName, String platform) throws SQLException;

    boolean approveTransaction(long txId, String operatorName, long amountVin) throws SQLException;

    boolean rejectTransaction(long txId, String operatorName, String reason) throws SQLException;

    boolean forceApproveTransaction(long txId, String operatorName, long amountVin) throws SQLException;

    boolean cancelTransaction(long txId, long userId) throws SQLException;

    // Recovery operations
    int expireOldTransactions(int minutesThreshold) throws SQLException;

    List<Map<String, Object>> getOrphanedPending(int minutesThreshold) throws SQLException;

    int releaseStaleProcessing(int minutesThreshold) throws SQLException;

    List<Map<String, Object>> getFailedCredits() throws SQLException;

    boolean updateCreditStatus(long txId, String creditStatus) throws SQLException;

    // Telegram
    boolean setTelegramMessageId(long txId, long messageId, String chatId) throws SQLException;

    // Utilities
    boolean unlockTransaction(long txId) throws SQLException;

    void insertAuditLog(long txId, String txCode, String action, String operatorId,
        String operatorName, String platform, String detail) throws SQLException;

    String generateTxCode(long txId) throws SQLException;
}

package com.vinplay.dal.dao;

import java.sql.SQLException;

/**
 * DAO interface for Credit Wallet operations.
 * Credit Wallet is a separate wallet for agents (distinct from agency_wallet and game wallet).
 * All balance operations are atomic at the SQL level.
 */
public interface CreditWalletDao {

    /**
     * Credit amount into credit_wallet.
     * Uses INSERT ON DUPLICATE KEY UPDATE for atomic upsert.
     * @return true if successful
     */
    boolean addBalance(int agentId, long amount) throws SQLException;

    /**
     * Debit amount from credit_wallet, only if balance >= amount.
     * SQL: UPDATE credit_wallet SET balance = balance - ? WHERE agent_id = ? AND balance >= ?
     * @return true if debit succeeded (1 row affected), false if insufficient balance
     */
    boolean debitBalance(int agentId, long amount) throws SQLException;

    /**
     * Get current balance. Returns 0 if no record exists yet.
     */
    long getBalance(int agentId) throws SQLException;

    /**
     * Log a transaction to credit_wallet_transactions.
     * @param type      ADMIN_CREDIT | TRANSFER_OUT | TRANSFER_IN | DEPOSIT_TO_AGENT | DEPOSIT_TO_USER
     * @param direction CREDIT | DEBIT
     */
    void logTransaction(int agentId, String agentNickname, String type, String direction,
                        long amount, long balanceAfter, String relatedUser,
                        Integer relatedAgentId, String note) throws SQLException;
}

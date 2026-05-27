package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * CreditWalletDaoImpl — MySQL implementation.
 * All balance operations are atomic at the SQL level to prevent race conditions.
 * Pool "mysqlpoolname" connects to vinplay.* database.
 */
public class CreditWalletDaoImpl implements CreditWalletDao {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public boolean addBalance(int agentId, long amount) throws SQLException {
        if (amount == 0) return true;
        String sql = "INSERT INTO vinplay.credit_wallet (agent_id, balance) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE balance = balance + ?, updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setLong(2, amount);
            ps.setLong(3, amount);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean debitBalance(int agentId, long amount) throws SQLException {
        String sql = "UPDATE vinplay.credit_wallet " +
                     "SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE agent_id = ? AND balance >= ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setInt(2, agentId);
            ps.setLong(3, amount);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                logger.warn("CreditWalletDao.debitBalance: insufficient balance agentId=" + agentId + " amount=" + amount);
            }
            return affected > 0;
        }
    }

    @Override
    public long getBalance(int agentId) throws SQLException {
        String sql = "SELECT balance FROM vinplay.credit_wallet WHERE agent_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("balance");
            }
        }
        return 0L;
    }

    @Override
    public void logTransaction(int agentId, String agentNickname, String type, String direction,
                               long amount, long balanceAfter, String relatedUser,
                               Integer relatedAgentId, String note) throws SQLException {
        String sql = "INSERT INTO vinplay.credit_wallet_transactions " +
                     "(agent_id, agent_nickname, type, direction, amount, balance_after, " +
                     "related_user, related_agent_id, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setString(2, agentNickname);
            ps.setString(3, type);
            ps.setString(4, direction);
            ps.setLong(5, amount);
            ps.setLong(6, balanceAfter);
            ps.setString(7, relatedUser);
            if (relatedAgentId != null) {
                ps.setInt(8, relatedAgentId);
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("CreditWalletDao.logTransaction failed agentId=" + agentId + " type=" + type, e);
            throw e;
        }
    }
}

package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.AgencyWalletDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgencyWalletDaoImpl implements AgencyWalletDao {
    @Override
    public boolean addBalance(int agentId, long amount) throws SQLException {
        boolean res = false;
        if (amount == 0) return true;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sql = "INSERT INTO vinplay.agency_wallet (agent_id, balance) VALUES (?, ?) " +
                         "ON DUPLICATE KEY UPDATE balance = balance + ?, updated_at = CURRENT_TIMESTAMP";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, agentId);
            stm.setLong(2, amount);
            stm.setLong(3, amount);
            if (stm.executeUpdate() > 0) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public long getBalance(int agentId) throws SQLException {
        long bal = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            PreparedStatement stm = conn.prepareStatement("SELECT balance FROM vinplay.agency_wallet WHERE agent_id = ?");
            stm.setInt(1, agentId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                bal = rs.getLong("balance");
            }
            rs.close();
            stm.close();
        }
        return bal;
    }
}

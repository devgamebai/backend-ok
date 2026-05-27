package com.vinplay.dal.dao;

import java.sql.SQLException;

public interface AgencyWalletDao {
    public boolean addBalance(int agentId, long amount) throws SQLException;
    public long getBalance(int agentId) throws SQLException;
}

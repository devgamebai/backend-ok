/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.agent.GameCommissionRate;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface GameCommissionRateDao {
    public List<GameCommissionRate> searchByNickName(String var1) throws SQLException;

    public Map<Integer, Double> getRateMapByNickName(String var1) throws SQLException;

    public Map<String, Map<Integer, Double>> getRateMapByNickNames(List<String> var1) throws SQLException;

    public boolean insertOrUpdate(GameCommissionRate var1) throws SQLException;

    public boolean delete(long var1) throws SQLException;

    public boolean deleteByNickNameAndGameId(String var1, int var2) throws SQLException;
}


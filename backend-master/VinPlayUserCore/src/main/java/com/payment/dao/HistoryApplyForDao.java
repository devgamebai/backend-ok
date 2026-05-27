/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.HistoryApplyForEntity;
import java.sql.SQLException;
import java.util.List;

public interface HistoryApplyForDao {
    public boolean create(HistoryApplyForEntity var1) throws SQLException;

    public boolean updateStatus(String var1, int var2, long var3) throws SQLException;

    public HistoryApplyForEntity getByRequestId(String var1) throws SQLException;

    public List<HistoryApplyForEntity> getAllByNickname(String var1) throws SQLException;
}


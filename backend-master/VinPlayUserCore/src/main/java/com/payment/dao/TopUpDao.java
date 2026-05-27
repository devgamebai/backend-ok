/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.TopUpEntity;
import java.sql.SQLException;
import java.util.List;

public interface TopUpDao {
    public boolean create(TopUpEntity var1) throws SQLException;

    public boolean updateStatus(String var1, int var2, String var3, long var4) throws SQLException;

    public boolean updateStatus(String var1, int var2, String var3) throws SQLException;

    public TopUpEntity getByRequestId(String var1) throws SQLException;

    public List<TopUpEntity> getAllByNickname(String var1) throws SQLException;
}


/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.TopUpEntity;
import java.sql.SQLException;

public interface TopUpLiveDao {
    public boolean create(TopUpEntity var1) throws SQLException;

    public boolean updateStatus(String var1, int var2, String var3, long var4) throws SQLException;

    public boolean updateStatus(String var1, int var2, String var3) throws SQLException;
}


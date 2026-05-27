/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.HistoryBankEntity;
import java.sql.SQLException;

public interface HistoryBankLiveDao {
    public boolean create(HistoryBankEntity var1) throws SQLException;

    public boolean updateStatus(String var1, int var2, long var3) throws SQLException;
}


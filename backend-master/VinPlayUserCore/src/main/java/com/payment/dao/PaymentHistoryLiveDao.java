/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.PaymentHistoryEntity;
import java.sql.SQLException;
import java.util.List;

public interface PaymentHistoryLiveDao {
    public List<PaymentHistoryEntity> getAllByNickname(String var1, int var2, int var3) throws SQLException;

    public int getTotalCount(String var1) throws SQLException;
}


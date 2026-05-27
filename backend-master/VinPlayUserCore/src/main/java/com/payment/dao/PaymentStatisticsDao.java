/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao;

import com.payment.entities.PaymentSummaryEntity;
import java.sql.SQLException;

public interface PaymentStatisticsDao {
    public PaymentSummaryEntity getPaymentSummaryByNickName(String var1) throws SQLException;

    public PaymentSummaryEntity getPaymentSummaryToday() throws SQLException;
}


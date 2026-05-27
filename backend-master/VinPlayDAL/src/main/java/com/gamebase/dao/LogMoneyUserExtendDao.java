/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao;

import com.gamebase.dao.model.SumUserDepositTimeResult;
import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;

public interface LogMoneyUserExtendDao {
    public LogMoneyUserNapTieuVinModel getFirstUserDepositTime(String var1, String var2, String var3, String var4);

    public Long countUserDepositTime(String var1, String var2, String var3, String var4);

    public SumUserDepositTimeResult sumUserDepositTime(String var1, String var2, String var3, String var4);
}


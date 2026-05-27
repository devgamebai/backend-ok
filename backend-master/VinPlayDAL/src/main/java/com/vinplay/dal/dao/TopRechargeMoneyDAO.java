/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TopRechargeMoneyResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.TopRechargeMoneyResponse;
import java.sql.SQLException;
import java.util.List;

public interface TopRechargeMoneyDAO {
    public List<TopRechargeMoneyResponse> getTopRechargeMoney(int var1, String var2, int var3, int var4) throws SQLException;
}


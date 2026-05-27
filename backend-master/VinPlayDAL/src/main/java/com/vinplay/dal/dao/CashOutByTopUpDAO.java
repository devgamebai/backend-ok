/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByTopUpResponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.CashOutByTopUpResponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public interface CashOutByTopUpDAO {
    public List<CashOutByTopUpResponse> searchCashOutByTopUp(String var1, String var2, String var3, String var4, String var5, String var6, int var7, String var8, String var9, String var10);

    public int countSearchCashOutByTopUp(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalCashOutByTopup(String var1, String var2, String var3, String var4, String var5);

    public List<CashOutByTopUpResponse> exportDataCashOutByTopup(String var1, String var2, String var3, String var4, String var5, String var6, String var7);
}


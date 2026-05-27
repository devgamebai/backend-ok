/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByCardResponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.CashOutByCardResponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public interface CashOutByCardDAO {
    public List<CashOutByCardResponse> searchCashOutByCard(String var1, String var2, String var3, String var4, String var5, String var6, int var7, String var8, String var9);

    public int countSearchCashOutByCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalCashOutByCard(String var1, String var2, String var3, String var4);

    public List<CashOutByCardResponse> exportDataCashOutByCard(String var1, String var2, String var3, String var4, String var5, String var6);
}


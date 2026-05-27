/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByBankReponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.CashOutByBankReponse;
import java.util.List;

public interface CashOutByBankService {
    public List<CashOutByBankReponse> searchCashOutByBank(String var1, String var2, String var3, String var4, String var5, String var6, int var7, String var8);

    public int countSearchCashOutByBank(String var1, String var2, String var3, String var4, String var5, String var6, String var7);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7);
}


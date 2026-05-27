/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.RechargeByCardReponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeByCardReponse;
import java.util.List;

public interface RechargeByCardService {
    public List<RechargeByCardReponse> searchRechargeByCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, int var8, String var9);

    public int countSearchRechargeByCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<MoneyTotalRechargeByCardReponse> doiSoatRechargeByCard(int var1, String var2, String var3);

    public List<RechargeByCardReponse> exportDataRechargeByCard(String var1, String var2, String var3, String var4, String var5);
}


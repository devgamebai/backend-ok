/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.VinCardResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.VinCardResponse;
import java.util.List;

public interface RechargeByVinCardDAO {
    public List<VinCardResponse> searchRechargeByVinCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, int var8, String var9);

    public int countSearchRechargeByVinCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByVinplayCard(String var1, String var2, String var3);
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.megacard.MegaCardResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.megacard.MegaCardResponse;
import java.util.List;

public interface RechargeByMegaCardService {
    public List<MegaCardResponse> searchRechargeByMegaCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, int var8, String var9);

    public int countSearchRechargeByMegaCard(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public long moneyTotal(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByMegaCard(String var1, String var2, String var3);
}


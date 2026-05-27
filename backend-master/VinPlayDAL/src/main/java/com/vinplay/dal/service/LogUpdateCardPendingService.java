/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LogUpdateCardPendingReponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.LogUpdateCardPendingReponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public interface LogUpdateCardPendingService {
    public List<LogUpdateCardPendingReponse> searchLogUpdateCardPending(String var1, String var2, String var3, String var4, String var5, String var6, String var7, int var8, String var9, String var10);

    public int countTotalRecordLogUpdateCardPending(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9);

    public List<MoneyTotalRechargeByCardReponse> moneyTotalUpdateCardPengding(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9);
}


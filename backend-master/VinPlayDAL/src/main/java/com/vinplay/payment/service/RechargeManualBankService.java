/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import java.util.Map;

public interface RechargeManualBankService {
    public RechargePaywellResponse create(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, long var10, String var12);

    public RechargePaywellResponse topupByCash(String var1, String var2, String var3, String var4, long var5, long var7, String var9);

    public RechargePaywellResponse update(String var1, int var2, String var3);

    public RechargePaywellResponse Approved(String var1, String var2);

    public RechargePaywellResponse Reject(String var1, String var2);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public Map<String, Object> FindTransaction(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8);

    public Map<String, Object> FindTransactionUserToAgent(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8);

    public boolean UpdateTransactionDetail(String var1, String var2, String var3, int var4);
}


/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.DepositPaygateReponse;
import java.util.Map;

public interface RechargePayaSecService {
    public RechargePaywellResponse createTransaction(String var1, String var2, String var3, String var4, long var5, String var7, String var8, String var9);

    public RechargePaywellResponse notification(String var1, String var2, String var3, String var4, String var5, String var6, long var7, long var9, long var11, int var13, String var14);

    public RechargePaywellResponse find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public DepositPaygateReponse search(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


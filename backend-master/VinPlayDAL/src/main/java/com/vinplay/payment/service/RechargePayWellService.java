/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.entities.PaywellNotifyRequest;
import java.util.Map;

public interface RechargePayWellService {
    public RechargePaywellResponse createTransaction(String var1, String var2, String var3, String var4, long var5, String var7, String var8);

    public RechargePaywellResponse notification(PaywellNotifyRequest var1);

    public RechargePaywellResponse callback(String var1, String var2, long var3, long var5, Integer var7, long var8, String var10);

    public RechargePaywellResponse checkStatusTrans(String var1);

    public RechargePaywellResponse find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public DepositPaygateReponse search(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.DepositPaygateReponse;

public interface RechargePrincePayService {
    public RechargePaywellResponse createTransaction(String var1, String var2, String var3, long var4, String var6, String var7, String var8, String var9);

    public RechargePaywellResponse notify(int var1, String var2, String var3);

    public RechargePaywellResponse checkStatusTrans(String var1);

    public RechargePaywellResponse find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public DepositPaygateReponse search(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


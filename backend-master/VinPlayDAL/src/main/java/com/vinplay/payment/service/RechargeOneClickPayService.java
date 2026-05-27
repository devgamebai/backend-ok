/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.BankOneClick;
import com.vinplay.payment.entities.DepositPaygateReponse;
import java.util.List;

public interface RechargeOneClickPayService {
    public RechargePaywellResponse createTransaction(String var1, String var2, String var3, long var4, String var6, String var7, String var8, String var9);

    public RechargePaywellResponse notify(String var1, String var2, String var3, String var4, String var5);

    public List<BankOneClick> getListBankSupport();

    public List<BankOneClick> getLstOneClickBank();

    public RechargePaywellResponse checkStatus(String var1);

    public RechargePaywellResponse getDataTrans(String var1);

    public RechargePaywellResponse find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public DepositPaygateReponse search(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


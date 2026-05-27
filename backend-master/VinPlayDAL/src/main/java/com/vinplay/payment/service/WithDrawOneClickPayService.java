/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import java.util.Map;

public interface WithDrawOneClickPayService {
    public RechargePaywellResponse withdrawal(String var1, String var2, String var3);

    public boolean notify(WithDrawPaygateModel var1, int var2);

    public RechargePaywellResponse checkStatus(String var1);

    public RechargePaywellResponse getDataTrans(String var1);

    public RechargePaywellResponse find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public boolean reject(String var1, String var2, String var3);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


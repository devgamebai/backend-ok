/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;

public interface WithDrawPrincePayService {
    public RechargePaywellResponse requestWithdrawUser(String var1, String var2, String var3, long var4, String var6);

    public RechargePaywellResponse withdrawal(String var1, String var2, String var3, String var4);

    public boolean notify(int var1, String var2, String var3);

    public RechargePaywellResponse findWithDraw(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}


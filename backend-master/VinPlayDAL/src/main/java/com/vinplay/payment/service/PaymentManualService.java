/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.utils.PayCommon;

public interface PaymentManualService {
    public RechargePaywellResponse withdrawal(String var1, String var2, String var3);

    public RechargePaywellResponse withdrawal(String var1, String var2, String var3, PayCommon.PAYSTATUS var4);

    public RechargePaywellResponse withdrawal(String var1, String var2, String var3, PayCommon.PAYSTATUS var4, String var5);

    public Boolean withdrawalSystemNote(String var1, PayCommon.PAYSTATUS var2, String var3);

    public RechargePaywellResponse deposit(String var1, String var2, Long var3, String var4, String var5, String var6, String var7, String var8);

    public RechargePaywellResponse depositConfirm(String var1, String var2, String var3, int var4, String var5);
}


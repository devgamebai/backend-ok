/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.service;

import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.Response;
import java.util.List;

public interface PaymentConfigService {
    public List<PaymentConfig> getConfig();

    public PaymentConfig getConfigByKey(String var1);

    public Response getConfig(String var1);

    public Response getBanks(String var1);

    public Response getBankWithdraw(String var1, Integer var2);
}


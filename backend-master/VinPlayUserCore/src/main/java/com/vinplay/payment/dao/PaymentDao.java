/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.response.LogExchangeMoneyResponse
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 */
package com.vinplay.payment.dao;

import com.vinplay.usercore.response.LogExchangeMoneyResponse;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import java.util.List;

public interface PaymentDao {
    public boolean logExchangeMoney(ExchangeMessage var1);

    public boolean checkMerchantTransId(String var1, String var2);

    public LogExchangeMoneyResponse getLogExchangeMoney(String var1, String var2, String var3, String var4, String var5, int var6, String var7, String var8, int var9) throws Exception;

    public long getTotalMoney(String var1, String var2, String var3, String var4, String var5) throws Exception;

    public List<ExchangeMessage> getExchangeMoney(String var1, String var2, String var3, String var4, String var5, int var6, String var7, String var8) throws Exception;
}


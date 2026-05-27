/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.PhoneCardType
 *  com.vinplay.vbee.common.enums.ProviderType
 */
package com.vinplay.dichvuthe.service;

import com.vinplay.dichvuthe.response.CashoutResponse;
import com.vinplay.dichvuthe.response.SoftpinResponse;
import com.vinplay.vbee.common.enums.PhoneCardType;
import com.vinplay.vbee.common.enums.ProviderType;
import java.util.Map;

public interface CashOutService {
    public SoftpinResponse cashOutByCard(String var1, ProviderType var2, PhoneCardType var3, int var4, boolean var5) throws Exception;

    public Map<String, Long> reCheckCashOutByCard() throws Exception;

    public CashoutResponse cashOutByTopUp(String var1, String var2, PhoneCardType var3, byte var4, boolean var5) throws Exception;

    public void cashOutByBank(String var1, int var2);
}


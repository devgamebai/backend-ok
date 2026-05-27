/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.RechardByIAPResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.RechardByIAPResponse;
import java.util.List;

public interface RechargeByIAPService {
    public List<RechardByIAPResponse> ListRechargeIAP(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public long totalMoney(String var1, String var2, String var3, String var4, String var5, String var6);

    public long countListRechargeIAP(String var1, String var2, String var3, String var4, String var5, String var6);
}


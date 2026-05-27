/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.PotModel
 *  com.vinplay.vbee.common.response.PotResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.PotModel;
import com.vinplay.vbee.common.response.PotResponse;

public interface PotService {
    public PotModel getPot(String var1);

    public PotResponse addMoneyPot(String var1, long var2, boolean var4);

    public PotResponse noHu(String var1, String var2, String var3, String var4, long var5, String var7, String var8, double var9, int var11, String var12);
}


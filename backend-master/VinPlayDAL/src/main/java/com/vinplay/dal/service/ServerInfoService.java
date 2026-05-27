/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.LogCCUModel
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.LogCCUModel;
import java.util.List;

public interface ServerInfoService {
    public void logCCU(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8);

    public List<LogCCUModel> getLogCCU(String var1, String var2);
}


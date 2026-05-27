/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.LogCCUModel
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.LogCCUModel;
import java.util.List;

public interface ServerInfoDAO {
    public List<LogCCUModel> getLogCCU(String var1, String var2);
}


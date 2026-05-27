/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LogCaoThapResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.LogCaoThapResponse;
import java.util.List;

public interface LogCaoThapDAO {
    public List<LogCaoThapResponse> listCaoThap(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public int countCaoThap(String var1, String var2, String var3, String var4, String var5, String var6);
}


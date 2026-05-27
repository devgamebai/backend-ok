/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LuckyNewHistoryResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.LuckyNewHistoryResponse;
import java.util.List;

public interface LuckyNewHistoryService {
    public List<LuckyNewHistoryResponse> searchLuckyNewHistory(String var1, String var2, String var3, int var4);
}


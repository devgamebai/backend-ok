/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LuckyVipHistoryResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.LuckyVipHistoryResponse;
import java.util.List;

public interface LuckyVipHistoryService {
    public List<LuckyVipHistoryResponse> searchLuckyVipHistory(String var1, String var2, String var3, int var4);
}


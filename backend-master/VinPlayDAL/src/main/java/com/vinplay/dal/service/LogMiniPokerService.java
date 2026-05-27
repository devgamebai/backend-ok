/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MiniPokerResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.MiniPokerResponse;
import java.util.List;

public interface LogMiniPokerService {
    public List<MiniPokerResponse> listMiniPoker(String var1, String var2, String var3, String var4, String var5, int var6);

    public int countMiniPoker(String var1, String var2, String var3, String var4, String var5);
}


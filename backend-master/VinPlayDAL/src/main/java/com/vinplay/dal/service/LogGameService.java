/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.LogGameMessage
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.messages.LogGameMessage;
import java.util.List;

public interface LogGameService {
    public boolean saveLogGameByNickName(LogGameMessage var1);

    public boolean saveLogGameDetail(LogGameMessage var1);

    public List<LogGameMessage> searchLogGameByNickName(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public int countSearchLogGameByNickName(String var1, String var2, String var3, String var4, String var5, String var6);

    public LogGameMessage getLogGameDetailBySessionID(String var1, String var2, String var3);
}


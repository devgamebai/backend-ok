/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.LogGameMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.LogGameMessage;

public interface LogGameDao {
    public boolean saveLogGameByNickName(LogGameMessage var1);

    public boolean saveLogGameDetail(LogGameMessage var1);
}


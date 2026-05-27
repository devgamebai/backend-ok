/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.LogGameMessage
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.messages.LogGameMessage;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface LogGameService {
    public boolean saveLogGameByNickName(LogGameMessage var1) throws IOException, TimeoutException, InterruptedException;

    public boolean saveLogGameDetail(LogGameMessage var1) throws IOException, TimeoutException, InterruptedException;
}


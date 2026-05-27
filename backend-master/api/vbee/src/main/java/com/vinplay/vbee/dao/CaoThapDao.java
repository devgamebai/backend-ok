/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage;

public interface CaoThapDao {
    public void logCaoThap(LogCaoThapMessage var1);

    public void logCaoThapWin(LogCaoThapWinMessage var1);
}


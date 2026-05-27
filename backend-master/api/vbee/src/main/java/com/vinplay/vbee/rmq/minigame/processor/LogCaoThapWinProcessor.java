/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage;
import com.vinplay.vbee.dao.impl.CaoThapDaoImpl;

public class LogCaoThapWinProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogCaoThapWinMessage message = (LogCaoThapWinMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        CaoThapDaoImpl dao = new CaoThapDaoImpl();
        dao.logCaoThapWin(message);
        return true;
    }
}


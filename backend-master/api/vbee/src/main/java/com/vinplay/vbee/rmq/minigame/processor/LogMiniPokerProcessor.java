/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage;
import com.vinplay.vbee.dao.impl.MiniPokerDaoImpl;

public class LogMiniPokerProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogMiniPokerMessage message = (LogMiniPokerMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        MiniPokerDaoImpl dao = new MiniPokerDaoImpl();
        dao.logMiniPoker(message);
        return true;
    }
}


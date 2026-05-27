/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogGameMessage
 */
package com.vinplay.vbee.rmq.gamebai.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogGameMessage;
import com.vinplay.vbee.dao.impl.LogGameDaoImpl;

public class SaveLogGameDetailProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogGameMessage message = (LogGameMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        LogGameDaoImpl dao = new LogGameDaoImpl();
        dao.saveLogGameDetail(message);
        return true;
    }
}


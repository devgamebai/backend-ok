/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogTanLocMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;

public class LogTanLocProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogTanLocMessage message = (LogTanLocMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        TaiXiuDaoImpl dao = new TaiXiuDaoImpl();
        dao.logTanLoc(message);
        return true;
    }
}


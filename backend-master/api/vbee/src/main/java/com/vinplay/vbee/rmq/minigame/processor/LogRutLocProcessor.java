/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogRutLocMessge
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;

public class LogRutLocProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogRutLocMessge message = (LogRutLocMessge)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        TaiXiuDaoImpl dao = new TaiXiuDaoImpl();
        dao.logRutLoc(message);
        return true;
    }
}


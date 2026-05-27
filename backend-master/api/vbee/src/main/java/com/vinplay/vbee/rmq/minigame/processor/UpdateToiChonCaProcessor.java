/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg;
import com.vinplay.vbee.dao.impl.BauCuaDaoImpl;

public class UpdateToiChonCaProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        ToiChonCaMsg msg = (ToiChonCaMsg)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        BauCuaDaoImpl dao = new BauCuaDaoImpl();
        dao.updateToiChonCa(msg);
        return true;
    }
}


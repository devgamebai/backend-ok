/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg;
import com.vinplay.vbee.dao.impl.BauCuaDaoImpl;

public class SaveTransactionBauCuaProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        TransactionBauCuaMsg message = (TransactionBauCuaMsg)BaseMessage.fromBytes((byte[])body);
        BauCuaDaoImpl dao = new BauCuaDaoImpl();
        boolean success = true;
        dao.saveTransactionBauCua(message);
        return true;
    }
}


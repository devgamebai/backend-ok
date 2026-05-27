/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage;
import com.vinplay.vbee.dao.impl.PokeGoDaoImpl;

public class LogPokeGoProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogPokeGoMessage message = (LogPokeGoMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        PokeGoDaoImpl dao = new PokeGoDaoImpl();
        dao.log(message);
        return true;
    }
}


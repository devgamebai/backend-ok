/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage
 */
package com.vinplay.vbee.rmq.slot.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage;
import com.vinplay.vbee.dao.impl.SlotDaoImpl;

public class LogNoHuSlotProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogNoHuSlotMessage msg = (LogNoHuSlotMessage)LogNoHuSlotMessage.fromBytes((byte[])((byte[])param.get()));
        SlotDaoImpl dao = new SlotDaoImpl();
        dao.logNoHu(msg);
        return true;
    }
}


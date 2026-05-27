/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage
 */
package com.vinplay.vbee.rmq.slot.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage;
import com.vinplay.vbee.dao.impl.SlotDaoImpl;

public class LogSlotMachineProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogSlotMachineMessage message = (LogSlotMachineMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        SlotDaoImpl dao = new SlotDaoImpl();
        dao.log(message);
        return true;
    }
}


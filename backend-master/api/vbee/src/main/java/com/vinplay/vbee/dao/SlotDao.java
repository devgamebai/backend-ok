/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage
 *  com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage;
import com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage;

public interface SlotDao {
    public void log(LogSlotMachineMessage var1);

    public void logNoHu(LogNoHuSlotMessage var1);
}


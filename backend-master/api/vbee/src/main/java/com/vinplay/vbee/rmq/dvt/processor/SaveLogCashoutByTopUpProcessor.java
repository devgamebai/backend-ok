/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage
 */
package com.vinplay.vbee.rmq.dvt.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage;
import com.vinplay.vbee.dao.impl.CashoutDaoImpl;

public class SaveLogCashoutByTopUpProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        CashoutByTopUpMessage message = (CashoutByTopUpMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        CashoutDaoImpl dao = new CashoutDaoImpl();
        dao.logCashoutByTopUp(message);
        return true;
    }
}


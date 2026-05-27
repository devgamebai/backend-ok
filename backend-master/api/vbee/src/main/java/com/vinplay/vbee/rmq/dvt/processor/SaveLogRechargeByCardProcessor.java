/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 */
package com.vinplay.vbee.rmq.dvt.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.dao.impl.RechargeDaoImpl;

public class SaveLogRechargeByCardProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        RechargeByCardMessage message = (RechargeByCardMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        RechargeDaoImpl dao = new RechargeDaoImpl();
        dao.logRechargeByCard(message);
        return true;
    }
}


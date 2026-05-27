/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 */
package com.vinplay.vbee.rmq.dvt.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;
import com.vinplay.vbee.dao.impl.RechargeDaoImpl;

public class SaveLogRefundFeeAgentProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        RefundFeeAgentMessage message = (RefundFeeAgentMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        RechargeDaoImpl dao = new RechargeDaoImpl();
        dao.logRefundFeeAgent(message);
        return true;
    }
}


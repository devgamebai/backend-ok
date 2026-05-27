/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogCCUMessage
 */
package com.vinplay.vbee.rmq.serverinfo.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogCCUMessage;
import com.vinplay.vbee.dao.impl.ServerInfoDaoImpl;

public class LogCCUProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogCCUMessage msg = (LogCCUMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        ServerInfoDaoImpl dao = new ServerInfoDaoImpl();
        dao.logCCU(msg);
        return true;
    }
}


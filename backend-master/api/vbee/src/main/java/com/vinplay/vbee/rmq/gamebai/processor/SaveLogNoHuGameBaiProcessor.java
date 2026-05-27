/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 */
package com.vinplay.vbee.rmq.gamebai.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.dao.impl.LogMoneyUserDaoImpl;

public class SaveLogNoHuGameBaiProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogNoHuGameBaiMessage message = (LogNoHuGameBaiMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        LogMoneyUserDaoImpl dao = new LogMoneyUserDaoImpl();
        dao.logNoHuGameBai(message);
        return true;
    }
}


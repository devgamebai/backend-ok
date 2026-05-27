/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 */
package com.vinplay.vbee.rmq.log.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import com.vinplay.vbee.dao.impl.LogMoneyUserDaoImpl;

public class LogExchangeMoneyProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        ExchangeMessage message = (ExchangeMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        LogMoneyUserDaoImpl dao = new LogMoneyUserDaoImpl();
        dao.logExchangeMoney(message);
        return true;
    }
}


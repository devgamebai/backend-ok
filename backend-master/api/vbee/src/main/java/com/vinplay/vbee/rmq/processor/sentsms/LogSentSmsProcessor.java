/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.sentsms.LogSentSmsMessage
 */
package com.vinplay.vbee.rmq.processor.sentsms;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.sentsms.LogSentSmsMessage;
import com.vinplay.vbee.dao.impl.LogSentSmsImpl;

public class LogSentSmsProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogSentSmsMessage message = (LogSentSmsMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        LogSentSmsImpl dao = new LogSentSmsImpl();
        dao.saveLogSentSms(message);
        return false;
    }
}


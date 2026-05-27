/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.OtpMessage
 */
package com.vinplay.vbee.rmq.otp.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.dao.impl.OtpDaoImpl;

public class SaveLogOTPProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        OtpMessage message = (OtpMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        OtpDaoImpl dao = new OtpDaoImpl();
        dao.saveLogOtp(message);
        return true;
    }
}


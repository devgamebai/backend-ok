/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.BrandnameMessage
 */
package com.vinplay.vbee.rmq.otp.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.BrandnameMessage;
import com.vinplay.vbee.dao.impl.OtpDaoImpl;

public class SaveLogBrandnameProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        BrandnameMessage message = (BrandnameMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        OtpDaoImpl dao = new OtpDaoImpl();
        dao.saveLogBrandname(message);
        return true;
    }
}


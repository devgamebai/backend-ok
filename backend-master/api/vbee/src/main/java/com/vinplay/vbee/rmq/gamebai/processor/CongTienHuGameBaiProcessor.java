/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.PotMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.gamebai.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.PotMessage;
import com.vinplay.vbee.dao.impl.ExceptionDaoImpl;
import com.vinplay.vbee.dao.impl.PotDaoImpl;
import org.apache.log4j.Logger;

public class CongTienHuGameBaiProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        PotMessage message = (PotMessage)BaseMessage.fromBytes((byte[])body);
        try {
            PotDaoImpl dao = new PotDaoImpl();
            dao.logHuGameBai(message);
            dao.addMoneyPot(message);
            return true;
        }
        catch (Exception e) {
            ExceptionDaoImpl ex = new ExceptionDaoImpl();
            try {
                ex.insertLogExceptionDB("vin", message.toJson(), e.getMessage());
            }
            catch (JsonProcessingException e2) {
                e2.printStackTrace();
            }
            logger.debug((Object)e);
            return false;
        }
    }
}


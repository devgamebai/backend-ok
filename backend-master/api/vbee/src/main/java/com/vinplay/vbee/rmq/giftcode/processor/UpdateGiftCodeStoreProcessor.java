/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.giftcode.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.dao.impl.GiftCodeDaoImpl;
import org.apache.log4j.Logger;

public class UpdateGiftCodeStoreProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        GiftCodeMessage message = (GiftCodeMessage)BaseMessage.fromBytes((byte[])body);
        try {
            GiftCodeDaoImpl dao = new GiftCodeDaoImpl();
            dao.updateGiftCodeStore(message.getGiftCode());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
        }
        return false;
    }
}


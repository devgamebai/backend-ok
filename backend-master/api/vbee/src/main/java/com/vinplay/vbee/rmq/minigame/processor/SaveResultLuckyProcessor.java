/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LuckyMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LuckyMessage;
import com.vinplay.vbee.dao.impl.LuckyDaoImpl;
import org.apache.log4j.Logger;

public class SaveResultLuckyProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        LuckyMessage message = (LuckyMessage)BaseMessage.fromBytes((byte[])body);
        LuckyDaoImpl dao = new LuckyDaoImpl();
        boolean success = false;
        try {
            dao.saveLuckyHistory(message);
            success = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return success;
    }
}


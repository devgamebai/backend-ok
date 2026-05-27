/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.userMission.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage;
import com.vinplay.vbee.dao.impl.UserDaoImpl;
import org.apache.log4j.Logger;

public class LogReceivedRewardMissionProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        LogReceivedRewardMissionMessage message = (LogReceivedRewardMissionMessage)BaseMessage.fromBytes((byte[])body);
        try {
            UserDaoImpl dao = new UserDaoImpl();
            dao.logReceivedRewardMission(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
        }
        return true;
    }
}


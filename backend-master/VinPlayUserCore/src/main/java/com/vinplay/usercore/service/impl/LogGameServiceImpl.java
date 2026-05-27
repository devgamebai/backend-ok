/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.service.LogGameService;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogGameMessage;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LogGameServiceImpl
implements LogGameService {
    @Override
    public boolean saveLogGameByNickName(LogGameMessage message) throws IOException, TimeoutException, InterruptedException {
        MessageBusFactory.get("queue_log_gamebai").publish((String)"queue_log_gamebai", (BaseMessage)message, (int)501);
        return true;
    }

    @Override
    public boolean saveLogGameDetail(LogGameMessage message) throws IOException, TimeoutException, InterruptedException {
        MessageBusFactory.get("queue_log_gamebai").publish((String)"queue_log_gamebai", (BaseMessage)message, (int)502);
        return true;
    }
}


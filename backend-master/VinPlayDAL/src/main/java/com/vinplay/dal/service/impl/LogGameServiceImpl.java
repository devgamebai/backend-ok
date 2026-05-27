/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogGameDAOImpl;
import com.vinplay.dal.service.LogGameService;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogGameMessage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class LogGameServiceImpl
implements LogGameService {
    @Override
    public boolean saveLogGameByNickName(LogGameMessage message) {
        try {
            MessageBusFactory.get("queue_log_gamebai").publish((String)"queue_log_gamebai", (BaseMessage)message, (int)501);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean saveLogGameDetail(LogGameMessage message) {
        try {
            MessageBusFactory.get("queue_log_gamebai").publish((String)"queue_log_gamebai", (BaseMessage)message, (int)502);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<LogGameMessage> searchLogGameByNickName(String sessionId, String nickName, String gameName, String timeStart, String timeEnd, String moneyType, int page) {
        LogGameDAOImpl dao = new LogGameDAOImpl();
        List<LogGameMessage> result = null;
        try {
            result = dao.searchLogGameByNickName(sessionId, nickName, gameName, timeStart, timeEnd, moneyType, page);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int countSearchLogGameByNickName(String sessionId, String nickName, String gameName, String timeStart, String timeEnd, String moneyType) {
        LogGameDAOImpl dao = new LogGameDAOImpl();
        int record = 0;
        try {
            record = dao.countSearchLogGameByNickName(sessionId, nickName, gameName, timeStart, timeEnd, moneyType);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }

    @Override
    public LogGameMessage getLogGameDetailBySessionID(String sessionId, String gameName, String timelog) {
        LogGameDAOImpl dao = new LogGameDAOImpl();
        LogGameMessage result = null;
        try {
            result = dao.getLogGameDetailBySessionID(sessionId, gameName, timelog);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}


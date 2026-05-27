/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogCCUMessage
 *  com.vinplay.vbee.common.models.LogCCUModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.ServerInfoDAOImpl;
import com.vinplay.dal.service.ServerInfoService;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogCCUMessage;
import com.vinplay.vbee.common.models.LogCCUModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ServerInfoServiceImpl
implements ServerInfoService {
    @Override
    public void logCCU(int ccu, int ccuWeb, int ccuAD, int ccuIOS, int ccuWP, int ccuFB, int ccuDT, int ccuOT) {
        String timestamp = DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss");
        LogCCUMessage msg = new LogCCUMessage();
        msg.ccu = ccu;
        msg.ccuWeb = ccuWeb;
        msg.ccuAD = ccuAD;
        msg.ccuIOS = ccuIOS;
        msg.ccuWP = ccuWP;
        msg.ccuFB = ccuFB;
        msg.ccuDT = ccuDT;
        msg.ccuOT = ccuOT;
        msg.timestamp = timestamp;
        try {
            MessageBusFactory.get("queue_server_info").publish("queue_server_info", (BaseMessage)msg, (int)1);
        }
        catch (Exception ex2) {
            Exception e = ex2;
            e.printStackTrace();
        }
    }

    @Override
    public List<LogCCUModel> getLogCCU(String startTime, String endTime) {
        ServerInfoDAOImpl dao = new ServerInfoDAOImpl();
        List<LogCCUModel> results = dao.getLogCCU(startTime, endTime);
        return results;
    }
}


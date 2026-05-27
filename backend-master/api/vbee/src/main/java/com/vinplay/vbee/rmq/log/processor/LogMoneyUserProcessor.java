/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.statics.Consts
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.log.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.dao.impl.LogMoneyUserDaoImpl;
import java.util.List;
import org.apache.log4j.Logger;

public class LogMoneyUserProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        LogMoneyUserMessage message = (LogMoneyUserMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        if (message.isBot() && Math.abs(message.getMoneyExchange()) <= 100000L) {
            logger.info((Object)("Khong xu ly bot: " + message.getNickname() + ", money exchange= " + message.getMoneyExchange()));
        } else {
            LogMoneyUserDaoImpl dao = new LogMoneyUserDaoImpl();
            long transId = 0L;
            int queryType = -1;
            if (message.getMoneyType().equals("vin")) {
                transId = ++com.vinplay.vbee.main.VBeeMain.moneyVinReferenceId;
            } else if (message.getMoneyType().equals("xu")) {
                transId = ++com.vinplay.vbee.main.VBeeMain.moneyXuReferenceId;
            }
            dao.saveLogMoneyUser(message, transId, message.isBot(), message.isVp());
            if (message.getMoneyType().equalsIgnoreCase("vin")) {
                if (message.getMoneyExchange() > 0L) {
                    if (Consts.NAP_VIN.contains(message.getActionName())) {
                        queryType = 3;
                        dao.saveLogMoneyUserVinOther(message, transId, queryType);
                    }
                } else if (Consts.TIEU_VIN.contains(message.getActionName())) {
                    queryType = 5;
                    dao.saveLogMoneyUserVinOther(message, transId, queryType);
                }
            }
        }
        return true;
    }
}


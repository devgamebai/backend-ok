/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;

import org.apache.log4j.Logger;

import java.sql.SQLException;

public class SaveResultTaiXiuProcessor
        implements BaseProcessor<byte[], Boolean> {
    private static final org.apache.log4j.Logger logger = Logger.getLogger((String)"vbee");
    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        ResultTaiXiuMessage message = (ResultTaiXiuMessage)BaseMessage.fromBytes((byte[])body);
        logger.info("SaveResultTaiXiuProcessor - isMD5: " + message.isMD5
                + " md5: " + message.md5 + " moneytype: " + message.moneyType);
        TaiXiuDao dao = new TaiXiuDaoImpl();
        if(message.isMD5)
            dao = new TaiXiuMD5DaoImpl();
        boolean success = false;
        try {
            success = dao.saveResultTaiXiu(message);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }
}


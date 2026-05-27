/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;

import java.sql.SQLException;

public class UpdatePotProcessor
        implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        UpdatePotMessage message = (UpdatePotMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        TaiXiuDaoImpl dao = new TaiXiuDaoImpl();

        TaiXiuMD5DaoImpl daoMD5 = new TaiXiuMD5DaoImpl();
        boolean success = false;
        try {
            if(message.isMD5)
                success = daoMD5.updatePot(message);
            else
                success = dao.updatePot(message);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }
}


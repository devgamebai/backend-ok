/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;

import java.sql.SQLException;

public class UpdateLuotRutLocProcessor
        implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        UpdateLuotRutLocMessage message = (UpdateLuotRutLocMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        TaiXiuDao dao = new TaiXiuDaoImpl();
        if(message.isMD5)
            dao = new TaiXiuMD5DaoImpl();
        boolean success = false;
        try {
            success = dao.updateLuotRutLoc(message);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }
}


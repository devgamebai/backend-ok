/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import java.sql.SQLException;

public class UpdateFundProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        UpdateFundMessage message = (UpdateFundMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        TaiXiuDaoImpl dao = new TaiXiuDaoImpl();
        boolean success = false;
        try {
            success = dao.updateFund(message);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }
}


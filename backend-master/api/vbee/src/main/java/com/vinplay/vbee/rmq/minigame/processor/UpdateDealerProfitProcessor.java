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

import com.vinplay.dal.dao.DealerProfitDao;
import com.vinplay.dal.dao.impl.DealerProfitImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateDealerProfitMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class UpdateDealerProfitProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");
    public Boolean execute(Param<byte[]> param) {
        UpdateDealerProfitMessage message = (UpdateDealerProfitMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        DealerProfitDao dao = new DealerProfitImpl();
        boolean success = false;
        try {
            success = dao.addDealerProfit(message.phienid, message.result, message.total_money_tai, message.total_money_xiu, message.total_profit, message.last_balance);
        }
        catch (SQLException e) {
            logger.error((Object)"Handle save transaction error ", (Throwable)e);
        }
        return success;
    }
}


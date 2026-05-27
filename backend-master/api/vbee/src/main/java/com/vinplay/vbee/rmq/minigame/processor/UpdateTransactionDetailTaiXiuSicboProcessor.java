package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.dao.impl.TaiXiuSicboDaoImpl;

import java.sql.SQLException;

public class UpdateTransactionDetailTaiXiuSicboProcessor implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[]) param.get();
        TransactionTaiXiuDetailMessage message = (TransactionTaiXiuDetailMessage) BaseMessage.fromBytes(body);
        try {
            return new TaiXiuSicboDaoImpl().updateTransactionTaiXiuDetail(message);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.dao.impl.TaiXiuSicboDaoImpl;

import java.sql.SQLException;

public class SaveTransactionDetailTaiXiuSicboProcessor implements BaseProcessor<byte[], Boolean> {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[]) param.get();
        TransactionTaiXiuDetailMessage message = (TransactionTaiXiuDetailMessage) BaseMessage.fromBytes(body);
        try {
            new TaiXiuSicboDaoImpl().saveTransactionTaiXiuDetail(message);

            // SUN-685 + SUN-938: dual-write sicbo bets to MongoDB log_sicbo.
            // Filter includes bet_side so different sides don't overwrite each
            // other. Uses $inc for bet_value so multiple bets on the same side
            // accumulate instead of the last bet overwriting the total.
            try {
                com.mongodb.client.MongoDatabase db = com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory.getDB();
                org.bson.Document filter = new org.bson.Document("reference_id", message.referenceId)
                        .append("user_name", message.username)
                        .append("money_type", message.moneyType)
                        .append("bet_side", (int) message.betSide);
                org.bson.Document incFields = new org.bson.Document("bet_value", message.betValue);
                org.bson.Document setOnInsert = new org.bson.Document("create_time", new java.util.Date())
                        .append("user_id", message.userId)
                        .append("user_name", message.username)
                        .append("nick_name", message.username)
                        .append("money_type", message.moneyType)
                        .append("bet_side", (int) message.betSide)
                        .append("reference_id", message.referenceId)
                        .append("prize", 0L)
                        .append("refund", 0L)
                        .append("input_time", message.inputTime)
                        .append("current_money", message.currentMoney);
                org.bson.Document update = new org.bson.Document("$inc", incFields)
                        .append("$setOnInsert", setOnInsert);
                db.getCollection("log_sicbo").updateOne(filter, update,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception mongoErr) {
                logger.warn("SUN-685 log_sicbo detail write failed: " + mongoErr.getMessage());
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

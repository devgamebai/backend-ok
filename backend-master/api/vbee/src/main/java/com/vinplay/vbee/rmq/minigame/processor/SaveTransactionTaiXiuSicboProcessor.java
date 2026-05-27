package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.dao.impl.TaiXiuSicboDaoImpl;

import java.sql.SQLException;

public class SaveTransactionTaiXiuSicboProcessor implements BaseProcessor<byte[], Boolean> {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[]) param.get();
        TransactionTaiXiuMessage message = (TransactionTaiXiuMessage) BaseMessage.fromBytes(body);
        try {
            new TaiXiuSicboDaoImpl().saveTransactionTaiXiu(message);

            // SUN-669: dual-write to MongoDB log_sicbo for consistency with other game logs
            try {
                com.mongodb.client.MongoDatabase db = com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory.getDB();
                org.bson.Document filter = new org.bson.Document("reference_id", message.referenceId)
                        .append("user_name", message.username)
                        .append("money_type", message.moneyType);
                org.bson.Document setFields = new org.bson.Document()
                        .append("user_id", message.userId)
                        .append("user_name", message.username)
                        .append("nick_name", message.username)
                        .append("money_type", message.moneyType)
                        .append("bet_value", message.betValue)
                        .append("bet_side", (int) message.betSide)
                        .append("prize", message.prize)
                        .append("refund", message.refund);
                org.bson.Document update = new org.bson.Document("$set", setFields)
                        .append("$setOnInsert", new org.bson.Document("create_time", new java.util.Date()));
                // Preserve current_money if the detail event already stored it.
                db.getCollection("log_sicbo").updateOne(filter, update,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception mongoErr) {
                logger.warn("SUN-669 log_sicbo write failed (MySQL succeeded, ignoring): " + mongoErr.getMessage());
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

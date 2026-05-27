/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;
import org.apache.log4j.Logger;

public class SaveTransactionTaiXiuProcessor
        implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        try {
            TransactionTaiXiuMessage message = (TransactionTaiXiuMessage)TransactionTaiXiuMessage.fromBytes((byte[])body);
            TaiXiuDao dao = new TaiXiuDaoImpl();
            if(message.isMD5)
                dao = new TaiXiuMD5DaoImpl();
            dao.saveTransactionTaiXiu(message);
            logger.debug((Object)("Handle message : " + message.referenceId));

            // SUN-669: dual-write to MongoDB log_taixiu for consistency with other game logs
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
                        .append("refund", message.refund)
                        .append("jackpot", message.jackpot)
                        .append("is_md5", message.isMD5);
                org.bson.Document update = new org.bson.Document("$set", setFields)
                        .append("$setOnInsert", new org.bson.Document("create_time", new java.util.Date()));
                // Preserve fields written by the detail event (notably current_money)
                // while still deduping repeated summary messages on the same logical bet.
                db.getCollection("log_taixiu").updateOne(filter, update,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception mongoErr) {
                logger.warn("SUN-669 log_taixiu write failed (MySQL succeeded, ignoring): " + mongoErr.getMessage());
            }
            return true; // success — must return true so RMQConsumer acks instead of nacking + requeueing (caused 3x duplicate inserts)
        }
        catch (Exception e) {
            logger.error((Object)"Handle save transaction error ", (Throwable)e);
            return false;
        }
    }
}

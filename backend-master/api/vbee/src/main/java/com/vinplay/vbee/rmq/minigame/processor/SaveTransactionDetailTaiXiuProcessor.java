/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 */
package com.vinplay.vbee.rmq.minigame.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.dao.impl.TaiXiuDaoImpl;
import com.vinplay.vbee.dao.impl.TaiXiuMD5DaoImpl;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class SaveTransactionDetailTaiXiuProcessor
        implements BaseProcessor<byte[], Boolean> {

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        TransactionTaiXiuDetailMessage message = (TransactionTaiXiuDetailMessage)BaseMessage.fromBytes((byte[])body);
        TaiXiuDaoImpl dao = new TaiXiuDaoImpl();

        TaiXiuMD5DaoImpl daoMD5 = new TaiXiuMD5DaoImpl();
        try {
            if(message.isMD5)
                daoMD5.saveTransactionTaiXiuDetail(message);
            else
                dao.saveTransactionTaiXiuDetail(message);

            // SUN-685: dual-write ALL taixiu bets (win + lose) to MongoDB log_taixiu
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
                        .append("bet_side", message.betSide)
                        .append("prize", message.prize)
                        .append("refund", message.refund)
                        .append("input_time", message.inputTime)
                        .append("is_md5", message.isMD5)
                        .append("current_money", message.currentMoney);
                org.bson.Document update = new org.bson.Document("$set", setFields)
                        .append("$setOnInsert", new org.bson.Document("create_time", new java.util.Date()));
                db.getCollection("log_taixiu").updateOne(filter, update,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception mongoErr) {
                org.apache.log4j.Logger.getLogger("vbee").warn("SUN-685 log_taixiu detail write failed: " + mongoErr.getMessage());
            }
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

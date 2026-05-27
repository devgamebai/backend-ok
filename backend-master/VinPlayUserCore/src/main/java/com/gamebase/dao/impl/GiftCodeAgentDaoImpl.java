/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.GiftCodeAgentDao;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class GiftCodeAgentDaoImpl
implements GiftCodeAgentDao {
    private final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public GiftCodeAgentResponse exportGiftCode(final GiftCodeMessage msg, long curentMoney, String nickName) {
        GiftCodeAgentResponse response = new GiftCodeAgentResponse();
        long moneyExport = msg.Quantity * (Integer.parseInt(msg.getPrice()) * 1000);
        if (moneyExport > curentMoney) {
            response.ErrorCode = 2;
        } else {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final String timeLog = df.format(new Date());
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            final MongoCollection giftCodeDB = db.getCollection("gift_code");
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            conditions.put("count_use", 0);
            conditions.put("price", msg.getPrice());
            conditions.put("type", msg.getType());
            conditions.put("release", msg.getRelease());
            long count = db.getCollection("gift_code_store").count((Bson)new Document(conditions));
            if (count >= (long)msg.getQuantity()) {
                UserServiceImpl service = new UserServiceImpl();
                MoneyResponse money = service.updateMoney(nickName, -moneyExport, "vin", "GcAgentExport", "\u0110\u1ea1i l\u00fd xu\u1ea5t Giftcode", "\u0110\u1ea1i l\u00fd " + nickName + " xu\u1ea5t giftcode m\u1ec7nh gi\u00e1: " + msg.getPrice() + "K, s\u1ed1 l\u01b0\u1ee3ng: " + msg.Quantity + " c\u00e1i.", 0L, null, TransType.NO_VIPPOINT);
                if (money.getErrorCode() == "1002") {
                    response.ErrorCode = 2;
                }
                if (money.isSuccess()) {
                    FindIterable iterable = db.getCollection("gift_code_store").find((Bson)new Document(conditions)).limit(msg.getQuantity());
                    iterable.forEach((Block)new Block<Document>(){

                        public void apply(Document document) {
                            Document doc = new Document();
                            doc.append("giftcode", document.getString("giftcode"));
                            doc.append("price", msg.getPrice());
                            doc.append("quantity", msg.getQuantity());
                            doc.append("source", msg.getSource());
                            doc.append("count_use", 0);
                            doc.append("create_time", timeLog);
                            doc.append("money_type", msg.getMoneyType());
                            doc.append("release", msg.getRelease());
                            doc.append("nick_name", "");
                            doc.append("user_name", "");
                            doc.append("mobile", "");
                            doc.append("block", 0);
                            doc.append("type", msg.getType());
                            doc.append("giftcodefull", (msg.getRelease() + msg.getPrice() + msg.getSource() + document.getString("giftcode")));
                            doc.append("update_time", "");
                            doc.append("agent", "1");
                            giftCodeDB.insertOne(doc);
                            try {
                                GiftCodeMessage message = new GiftCodeMessage();
                                message.setGiftCode(document.getString("giftcode"));
                                MessageBusFactory.get("queue_gift_code").publish("queue_gift_code", message, 1200);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    response.ErrorCode = 0;
                    response.CurrentMoney = money.getCurrentMoney();
                }
            } else {
                response.ErrorCode = 1;
            }
        }
        this.logger.error(response.ErrorCode);
        return response;
    }
}


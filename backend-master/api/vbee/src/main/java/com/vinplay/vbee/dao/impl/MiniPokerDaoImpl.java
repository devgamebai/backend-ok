/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.MiniPokerDao;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bson.Document;

public class MiniPokerDaoImpl
implements MiniPokerDao {
    @Override
    public void logMiniPoker(LogMiniPokerMessage message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_mini_poker");
        Document doc = new Document();
        doc.append("user_name", (Object)message.username);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("result", (Object)message.result);
        doc.append("prize", (Object)message.prize);
        doc.append("cards", (Object)message.cards);
        doc.append("current_pot", (Object)message.currentPot);
        doc.append("current_fund", (Object)message.currentFund);
        doc.append("money_type", (Object)message.moneyType);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.LogGameMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.LogGameDao;
import java.util.Date;
import org.bson.Document;

public class LogGameDaoImpl
implements LogGameDao {
    @Override
    public boolean saveLogGameByNickName(LogGameMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_game");
        Document doc = new Document();
        doc.append("nick_name", (Object)message.nickName);
        doc.append("session_id", (Object)message.sessionId);
        doc.append("game_name", (Object)message.gameName);
        doc.append("time_log", (Object)message.timeLog);
        doc.append("create_time", (Object)new Date());
        doc.append("money_type", (Object)message.moneyType);
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean saveLogGameDetail(LogGameMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_game_detail");
        Document doc = new Document();
        doc.append("log_detail", (Object)message.logDetail);
        doc.append("session_id", (Object)message.sessionId);
        doc.append("game_name", (Object)message.gameName);
        doc.append("time_log", (Object)message.timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }
}


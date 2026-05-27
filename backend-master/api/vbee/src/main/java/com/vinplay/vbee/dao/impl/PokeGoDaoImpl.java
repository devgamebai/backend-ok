/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.PokeGoDao;
import java.util.Date;
import org.bson.Document;

public class PokeGoDaoImpl
implements PokeGoDao {
    @Override
    public void log(LogPokeGoMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_poke_go");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.referenceId);
        doc.append("user_name", (Object)message.username);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("lines_betting", (Object)message.linesBetting);
        doc.append("lines_win", (Object)message.linesWin);
        doc.append("prizes_on_line", (Object)message.prizesOnLine);
        doc.append("prize", (Object)message.totalPrizes);
        doc.append("result", (Object)message.result);
        doc.append("money_type", (Object)message.moneyType);
        doc.append("time_log", (Object)message.time);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}


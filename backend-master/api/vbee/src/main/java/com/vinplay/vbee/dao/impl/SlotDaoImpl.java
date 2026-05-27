/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage
 *  com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.minigame.LogNoHuSlotMessage;
import com.vinplay.vbee.common.messages.slot.LogSlotMachineMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.SlotDao;
import java.util.Date;
import org.bson.Document;

public class SlotDaoImpl
implements SlotDao {
    @Override
    public void log(LogSlotMachineMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_" + message.gameName);
        Document doc = new Document();
        doc.append("reference_id", (Object)message.referenceId);
        doc.append("user_name", (Object)message.username);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("lines_betting", (Object)message.linesBetting);
        doc.append("lines_win", (Object)message.linesWin);
        doc.append("prizes_on_line", (Object)message.prizesOnLine);
        doc.append("prize", (Object)message.totalPrizes);
        doc.append("result", (Object)message.result);
        doc.append("time_log", (Object)message.time);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void logNoHu(LogNoHuSlotMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_no_hu_slot");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.referenceId);
        doc.append("game_name", (Object)message.gameName);
        doc.append("nick_name", (Object)message.nickName);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("lines_betting", (Object)message.linesBetting);
        doc.append("matrix", (Object)message.matrix);
        doc.append("lines_win", (Object)message.linesWin);
        doc.append("prizes_on_line", (Object)message.prizesOnLine);
        doc.append("prize", (Object)message.totalPrizes);
        doc.append("result", (Object)message.result);
        doc.append("time_log", (Object)message.time);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}


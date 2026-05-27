/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.LogCCUMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.LogCCUMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.ServerInfoDao;
import java.util.Date;
import org.bson.Document;

public class ServerInfoDaoImpl
implements ServerInfoDao {
    @Override
    public void logCCU(LogCCUMessage msg) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_ccu");
        Document doc = new Document();
        doc.append("ccu", (Object)msg.ccu);
        doc.append("web", (Object)msg.ccuWeb);
        doc.append("ad", (Object)msg.ccuAD);
        doc.append("ios", (Object)msg.ccuIOS);
        doc.append("wp", (Object)msg.ccuWP);
        doc.append("fb", (Object)msg.ccuFB);
        doc.append("dt", (Object)msg.ccuDT);
        doc.append("ot", (Object)msg.ccuOT);
        doc.append("time_log", (Object)msg.timestamp);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}


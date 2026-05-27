/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.LogCCUModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.ServerInfoDAO;
import com.vinplay.vbee.common.models.LogCCUModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ServerInfoDAOImpl
implements ServerInfoDAO {
    @Override
    public List<LogCCUModel> getLogCCU(String startTime, String endTime) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, BasicDBObject> conditions = new HashMap<String, BasicDBObject>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", startTime);
        obj.put("$lte", endTime);
        conditions.put("time_log", obj);
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", 1);
        iterable = db.getCollection("log_ccu").find((Bson)new Document(conditions)).sort((Bson)objsort);
        final ArrayList<LogCCUModel> results = new ArrayList<LogCCUModel>();
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogCCUModel entry = new LogCCUModel();
                entry.ccu = document.getInteger("ccu", 0);
                entry.total = entry.ccu;
                entry.ad = document.getInteger("ad", 0);
                entry.ios = document.getInteger("ios", 0);
                entry.web = document.getInteger("web", 0);
                entry.phone = entry.ad + entry.ios;
                entry.dt = document.getInteger("dt", 0);
                entry.desktop = entry.dt;
                entry.wp = document.getInteger("wp", 0);
                entry.fb = document.getInteger("fb", 0);
                entry.ot = document.getInteger("ot", 0);
                entry.other = entry.wp + entry.fb + entry.ot;
                entry.ts = document.getString("time_log");
                entry.time_log = entry.ts;
                results.add(entry);
            }
        });
        return results;
    }

}


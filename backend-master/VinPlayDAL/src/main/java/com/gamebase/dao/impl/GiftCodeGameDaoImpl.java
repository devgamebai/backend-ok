/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.GiftCodeGameResponse
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.GiftCodeGameDao;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.GiftCodeGameResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class GiftCodeGameDaoImpl
implements GiftCodeGameDao {
    private final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public boolean exportGiftCodeStore(GiftCodeGameResponse msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("giftcode", msg.giftCode);
        conditions.put("surfing", msg.surfing);
        conditions.put("release", msg.release);
        conditions.put("source", msg.source);
        conditions.put("quantity", msg.quantity);
        conditions.put("count_use", 0);
        conditions.put("time_log", msg.timeLog);
        long count = db.getCollection("gift_code_game_store").count((Bson)new Document(conditions));
        MongoCollection col = db.getCollection("gift_code_game_store");
        if (count > 0L) {
            col.updateOne((Bson)new Document("giftcode", msg.giftCode), (Bson)new Document("$set", new Document("surfing", msg.surfing).append("release", msg.release).append("source", msg.source).append("update_time", timeLog)));
        } else {
            Document doc = new Document();
            doc.append("giftcode",msg.giftCode);
            doc.append("surfing",msg.surfing);
            doc.append("quantity", msg.quantity);
            doc.append("source", msg.source);
            doc.append("count_use", 0);
            doc.append("money_type", 1);
            doc.append("create_time", timeLog);
            doc.append("release", msg.release);
            col.insertOne(doc);
        }
        return true;
    }

    @Override
    public boolean exportGiftCode(final GiftCodeGameResponse msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        final MongoCollection col = db.getCollection("gift_code_game");
        final MongoCollection colstore = db.getCollection("gift_code_game_store");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("count_use", 0);
        conditions.put("surfing", msg.surfing);
        long count = db.getCollection("gift_code_game_store").count((Bson)new Document(conditions));
        if (count >= (long)msg.quantity) {
            FindIterable iterable = db.getCollection("gift_code_game_store").find((Bson)new Document(conditions)).limit(msg.quantity);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Document doc = new Document();
                    int len = 12 - (msg.release + msg.surfing + msg.source).length();
                    int lengiftcode = document.getString("giftcode").length();
                    String giftcode = "";
                    giftcode = lengiftcode > len ? document.getString("giftcode").substring(0, len) : document.getString("giftcode");
                    doc.append("giftcode",document.getString("giftcode"));
                    doc.append("surfing",msg.surfing);
                    doc.append("quantity", msg.quantity);
                    doc.append("source", msg.source);
                    doc.append("count_use", 0);
                    doc.append("create_time", timeLog);
                    doc.append("release", msg.release);
                    doc.append("money_type", 1);
                    doc.append("nick_name", "");
                    doc.append("user_name", "");
                    doc.append("mobile", "");
                    doc.append("ip", "");
                    doc.append("block", 0);
                    doc.append("giftcodefull", (msg.release + msg.surfing + msg.source + giftcode));
                    doc.append("update_time", "");
                    doc.append("expire_time", "");
                    col.insertOne(doc);
                    colstore.updateOne((Bson)new Document("giftcode", document.getString("giftcode")), (Bson)new Document("$set", new Document("count_use", 1).append("update_time", timeLog)));
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public List<GiftCodeGameResponse> searchAllGiftCode(String nickName, String giftcode, String surfing, String source, String timeStart, String timeEnd, String userName, String block, String giftuse, int page, int totalRecord) {
        int num_start = (page - 1) * totalRecord;
        final ArrayList<GiftCodeGameResponse> results = new ArrayList<GiftCodeGameResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        conditions.put("money_type", 1);
        objsort.put("_id", -1);
        if (surfing != null && !surfing.equals("")) {
            conditions.put("surfing", surfing);
        }
        if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("update_time", obj);
        }
        if (source != null && !source.isEmpty()) {
            conditions.put("source", source);
        }
        if (userName != null && !userName.isEmpty()) {
            conditions.put("user_name", userName);
        }
        if (nickName != null && !nickName.isEmpty()) {
            conditions.put("nick_name", nickName);
        }
        if (giftcode != null && !giftcode.isEmpty()) {
            conditions.put("giftcodefull", giftcode);
        }
        if (block != null && !block.isEmpty()) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (giftuse != null && !giftuse.isEmpty()) {
            conditions.put("count_use", Integer.parseInt(giftuse));
        }
        FindIterable iterable = db.getCollection("gift_code_game").find((Bson)new Document(conditions)).skip(num_start).limit(totalRecord).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeGameResponse giftcode = new GiftCodeGameResponse();
                giftcode.surfing = document.getString("surfing");
                giftcode.source = document.getString("source");
                giftcode.quantity = document.getInteger("quantity");
                giftcode.giftCodeFull = document.getString("giftcodefull");
                giftcode.release = document.getString("release");
                giftcode.timeLog = document.getString("create_time");
                giftcode.updateTime = document.getString("update_time");
                giftcode.moneyType = document.getInteger("money_type");
                giftcode.useGiftCode = document.getInteger("count_use");
                giftcode.userName = document.getString("user_name");
                giftcode.nickName = document.getString("nick_name");
                giftcode.block = document.getInteger("block");
                results.add(giftcode);
            }
        });
        return results;
    }

    @Override
    public long countSearchAllGiftCode(String nickName, String giftcode, String surfing, String source, String timeStart, String timeEnd, String userName, String block, String giftuse) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("money_type", 1);
        if (surfing != null && !surfing.equals("")) {
            conditions.put("surfing", surfing);
        }
        if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("update_time", obj);
        }
        if (source != null && !source.isEmpty()) {
            conditions.put("source", source);
        }
        if (nickName != null && !nickName.isEmpty()) {
            conditions.put("nick_name", nickName);
        }
        if (userName != null && !userName.isEmpty()) {
            conditions.put("user_name", userName);
        }
        if (giftcode != null && !giftcode.isEmpty()) {
            conditions.put("giftcodefull", giftcode);
        }
        if (block != null && !block.isEmpty()) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (giftuse != null && !giftuse.isEmpty()) {
            conditions.put("count_use", Integer.parseInt(giftuse));
        }
        long record = db.getCollection("gift_code_game").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public List<GiftCodeGameResponse> searchAllGiftCodeAdmin(String surfing, String source, String timeStart, String timeEnd, String giftuse, int page, int totalRecord) {
        int num_start = (page - 1) * totalRecord;
        final ArrayList<GiftCodeGameResponse> results = new ArrayList<GiftCodeGameResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        conditions.put("money_type", 1);
        if (surfing != null && !surfing.equals("")) {
            conditions.put("surfing", surfing);
        }
        if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("update_time", obj);
        }
        if (source != null && !source.isEmpty()) {
            conditions.put("source", source);
        }
        if (giftuse != null && !giftuse.isEmpty()) {
            conditions.put("count_use", Integer.parseInt(giftuse));
        }
        FindIterable iterable = db.getCollection("gift_code_game_store").find((Bson)new Document(conditions)).skip(num_start).limit(totalRecord).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeGameResponse giftcode = new GiftCodeGameResponse();
                giftcode.surfing = document.getString("surfing");
                giftcode.giftCode = document.getString("release") + document.getString("surfing") + document.getString("source") + document.getString("giftcode");
                giftcode.source = document.getString("source");
                giftcode.quantity = document.getInteger("quantity");
                giftcode.release = document.getString("release");
                giftcode.timeLog = document.getString("create_time");
                giftcode.updateTime = document.getString("update_time");
                giftcode.moneyType = document.getInteger("money_type");
                giftcode.useGiftCode = document.getInteger("count_use");
                results.add(giftcode);
            }
        });
        return results;
    }

    @Override
    public long countSearchAllGiftCodeAdmin(String surfing, String source, String timeStart, String timeEnd, String giftuse) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("money_type", 1);
        if (surfing != null && !surfing.equals("")) {
            conditions.put("surfing", surfing);
        }
        if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("update_time", obj);
        }
        if (source != null && !source.isEmpty()) {
            conditions.put("source", source);
        }
        if (giftuse != null && !giftuse.isEmpty()) {
            conditions.put("count_use", Integer.parseInt(giftuse));
        }
        long record = db.getCollection("gift_code_game_store").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public boolean blockGiftCode(String giftCode, String block) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection colstore = db.getCollection("gift_code_game");
        colstore.updateOne((Bson)new Document("giftcode", giftCode), (Bson)new Document("$set", new Document("block", Integer.parseInt(block))));
        return true;
    }
}


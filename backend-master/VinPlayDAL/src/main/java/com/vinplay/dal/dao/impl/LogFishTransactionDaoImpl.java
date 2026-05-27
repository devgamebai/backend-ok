/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogFishTransactionDao;
import com.vinplay.dal.entities.fish.FishGameRecord;
import com.vinplay.dal.entities.fish.FishTransaction;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogFishTransactionDaoImpl
implements LogFishTransactionDao {
    private static final Logger LOGGER = Logger.getLogger(LogFishTransactionDaoImpl.class);
    private static final String COLLECTION_FISHTRANSACTION = "fishlogtransaction";

    @Override
    public List<Map<String, Object>> search(String nickName, String orderId, String timeStart, String timeEnd, int page) {
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", timeStart);
                obj.put("$lte", timeEnd);
                conditions.put("timeStamp", obj);
            }
            if (nickName != null && !nickName.trim().isEmpty()) {
                conditions.append("nickname", nickName);
            }
            if (orderId != null && !orderId.trim().isEmpty()) {
                conditions.append("orderId", orderId);
            }
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            Document sort = new Document();
            sort = new Document("$sort", new Document("timeStamp", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION_FISHTRANSACTION);
            int num_start = (page - 1) * 50;
            int num_end = 50;
            Document skip = new Document();
            skip = new Document("$skip", num_start);
            Document limit = new Document();
            limit = new Document("$limit", num_end);
            List result = new ArrayList();
            result = (List)collection.aggregate(Arrays.asList(match, skip, limit, project, sort)).allowDiskUse(Boolean.valueOf(true)).into(new ArrayList());
            HashMap<String, Object> objectMap = new HashMap<String, Object>();
            objectMap.put("data", result);
            data.add(objectMap);
            Document count = new Document();
            count = new Document("$count", "nickname");
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object obj2 : aggregateCount) {
                try {
                    Document document = (Document) obj2;
                    objectMap = new HashMap<String, Object>();
                    objectMap.put("totalRecord", Integer.valueOf(document.getInteger("nickname", 0)));
                    data.add(objectMap);
                }
                catch (Exception exception) {}
            }
            Document matchPlayers = new Document();
            matchPlayers = new Document("$match", new Document("endtime", new Document("$gte", timeStart).append("$lte", timeEnd)));
            Document group = new Document();
            group = new Document("$group", new Document("_id", "$nickname"));
            AggregateIterable aggregateCountPlayer = collection.aggregate(Arrays.asList(matchPlayers, group, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object obj3 : aggregateCountPlayer) {
                try {
                    Document document = (Document) obj3;
                    objectMap = new HashMap<String, Object>();
                    objectMap.put("totalPlayer", Integer.valueOf(document.getInteger("nick_name", 0)));
                    data.add(objectMap);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            LOGGER.error(("Search FishGameRecord error: " + e.getMessage()));
        }
        return data;
    }

    @Override
    public FishGameRecord findItem(String orderId) throws Exception {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            Document conditions = new Document();
            conditions.put("orderId", orderId);
            Document document = (Document)db.getCollection(COLLECTION_FISHTRANSACTION).find((Bson)new Document((Map)conditions)).first();
            FishGameRecord entity = new FishGameRecord();
            entity.setAid(document.getString("aid"));
            entity.setBetcoin(document.getDouble("betcoin"));
            entity.setBp(document.getInteger("bp"));
            entity.setChoushui(document.getDouble("choushui"));
            entity.setCodeamount(document.getDouble("codeamount"));
            entity.setCoin(document.getDouble("coin"));
            entity.setCoinenter(document.getDouble("coinenter"));
            entity.setCoinquit(document.getDouble("coinquit"));
            entity.setEndtime(document.getString("endtime"));
            entity.setGameno(document.getString("gameno"));
            entity.setGid(document.getString("gid"));
            entity.setGrade(document.getDouble("grade"));
            entity.setGroups(document.getInteger("groups"));
            entity.setId(document.getInteger("id"));
            entity.setMid(document.getString("mid"));
            entity.setMuid(document.getString("muid"));
            entity.setRoomid(document.getInteger("roomid"));
            entity.setSync(document.getInteger("sync"));
            entity.setUid(document.getInteger("uid"));
            entity.setWlcoin(document.getDouble("wlcoin"));
            return entity;
        }
        catch (Exception e) {
            LOGGER.error(("FindItem FishGameRecord error: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public boolean Save(FishTransaction entity) throws Exception {
        boolean isUpdate = true;
        if (this.findItem(entity.getOrderId()) == null) {
            isUpdate = false;
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection col = db.getCollection(COLLECTION_FISHTRANSACTION);
            Document conditions = new Document();
            conditions.put("orderId", entity.getOrderId());
            BasicDBObject doc = new BasicDBObject();
            doc.append("orderId", entity.getOrderId());
            doc.append("prefix", entity.getPrefix());
            doc.append("nickname", entity.getNickname());
            doc.append("param", entity.getParam());
            doc.append("timeStamp", entity.getTimeStamp());
            doc.append("action", entity.getAction());
            doc.append("money", entity.getMoney());
            doc.append("key", entity.getKey());
            doc.append("status", entity.getStatus());
            doc.append("urlApi", entity.getUrlApi());
            if (isUpdate) {
                col.updateOne((Bson)conditions, (Bson)new Document("$set", doc));
            } else {
                col.insertOne(new Document((Map)doc));
            }
            return true;
        }
        catch (Exception ex) {
            LOGGER.error("Save item FishGameRecord error: ", (Throwable)ex);
            return false;
        }
    }
}


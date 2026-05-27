/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.apache.commons.lang.time.DateUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogFishDao;
import com.vinplay.dal.entities.fish.FishGameRecord;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogFishDaoImpl
implements LogFishDao {
    private static final Logger LOGGER = Logger.getLogger(LogFishDaoImpl.class);
    private static final String COLLECTION_FISHRECORD = "fishgamerecord";

    @Override
    public Map<String, Object> search(String nickName, String timeStart, String timeEnd, int page) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", (timeStart + " 00:00:00"));
                obj.put("$lte", (timeEnd + " 23:59:59"));
                conditions.put("endtime", obj);
            }
            if (nickName != null && !nickName.trim().isEmpty()) {
                conditions.append("muid", nickName);
            }
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            Document sort = new Document();
            sort = new Document("$sort", new Document("endtime", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION_FISHRECORD);
            int num_start = (page - 1) * 50;
            int num_end = 50;
            Document skip = new Document();
            skip = new Document("$skip", num_start);
            Document limit = new Document();
            limit = new Document("$limit", num_end);
            List result = new ArrayList();
            result = (List)collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).into(new ArrayList());
            data.put("fishgamerecords", result);
            Document count = new Document();
            count = new Document("$count", "muid");
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object obj2 : aggregateCount) {
                try {
                    Document document = (Document) obj2;
                    data.put("totalRecord", document.getInteger("muid", 0));
                }
                catch (Exception exception) {}
            }
            Document matchPlayers = new Document();
            matchPlayers = timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty() ? new Document("$match", new Document("endtime", new Document("$gte", (timeStart + " 00:00:00")).append("$lte", (timeEnd + " 23:59:59")))) : new Document("$match", new Document("endtime", new Document("$exists", true)));
            Document group = new Document();
            group = new Document("$group", new Document("_id", "$muid"));
            AggregateIterable aggregateCountPlayer = collection.aggregate(Arrays.asList(matchPlayers, group, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object obj3 : aggregateCountPlayer) {
                try {
                    Document document = (Document) obj3;
                    data.put("totalPlayer", document.getInteger("muid", 0));
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
    public FishGameRecord findItem(Integer id, Integer roomId, String gId, String mUid, String endTime) throws Exception {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            Document conditions = new Document();
            conditions.put("id", id);
            conditions.put("roomid", roomId);
            conditions.put("gid", gId);
            conditions.put("muid", mUid);
            conditions.put("endtime", endTime);
            Document document = (Document)db.getCollection(COLLECTION_FISHRECORD).find((Bson)new Document((Map)conditions)).first();
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
    public boolean Save(FishGameRecord entity) throws Exception {
        boolean isUpdate = true;
        if (this.findItem(entity.getId(), entity.getRoomid(), entity.getGid(), entity.getMuid(), entity.getEndtime()) == null) {
            isUpdate = false;
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection col = db.getCollection(COLLECTION_FISHRECORD);
            Document conditions = new Document();
            conditions.put("endtime", entity.getEndtime());
            conditions.put("gid", entity.getGid());
            conditions.put("muid", entity.getMuid());
            BasicDBObject doc = new BasicDBObject();
            doc.append("aid", entity.getAid());
            doc.append("betcoin", entity.getBetcoin());
            doc.append("bp", entity.getBp());
            doc.append("choushui", entity.getChoushui());
            doc.append("codeamount", entity.getCodeamount());
            doc.append("coin", entity.getCoin());
            doc.append("coinenter", entity.getCoinenter());
            doc.append("coinquit", entity.getCoinquit());
            doc.append("endtime", entity.getEndtime());
            doc.append("gameno", entity.getGameno());
            doc.append("gid", entity.getGid());
            doc.append("grade", entity.getGrade());
            doc.append("groups", entity.getGroups());
            doc.append("id", entity.getId());
            doc.append("mid", entity.getMid());
            doc.append("muid", entity.getMuid());
            doc.append("roomid", entity.getRoomid());
            doc.append("sync", entity.getSync());
            doc.append("uid", entity.getUid());
            doc.append("wlcoin", entity.getWlcoin());
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

    @Override
    public boolean insert(FishGameRecord entity) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection col = db.getCollection(COLLECTION_FISHRECORD);
            Document conditions = new Document();
            conditions.put("endtime", entity.getEndtime());
            conditions.put("gid", entity.getGid());
            conditions.put("muid", entity.getMuid());
            BasicDBObject doc = new BasicDBObject();
            doc.append("aid", entity.getAid());
            doc.append("betcoin", entity.getBetcoin());
            doc.append("bp", entity.getBp());
            doc.append("choushui", entity.getChoushui());
            doc.append("codeamount", entity.getCodeamount());
            doc.append("coin", entity.getCoin());
            doc.append("coinenter", entity.getCoinenter());
            doc.append("coinquit", entity.getCoinquit());
            doc.append("endtime", entity.getEndtime());
            doc.append("gameno", entity.getGameno());
            doc.append("gid", entity.getGid());
            doc.append("grade", entity.getGrade());
            doc.append("groups", entity.getGroups());
            doc.append("id", entity.getId());
            doc.append("mid", entity.getMid());
            doc.append("muid", entity.getMuid());
            doc.append("roomid", entity.getRoomid());
            doc.append("sync", entity.getSync());
            doc.append("uid", entity.getUid());
            doc.append("wlcoin", entity.getWlcoin());
            col.insertOne(new Document((Map)doc));
            return true;
        }
        catch (Exception ex) {
            LOGGER.error("Save item FishGameRecord error: ", (Throwable)ex);
            return false;
        }
    }

    @Override
    public boolean update(FishGameRecord entity) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection col = db.getCollection(COLLECTION_FISHRECORD);
            Document conditions = new Document();
            conditions.put("endtime", entity.getEndtime());
            conditions.put("gid", entity.getGid());
            conditions.put("muid", entity.getMuid());
            BasicDBObject doc = new BasicDBObject();
            doc.append("aid", entity.getAid());
            doc.append("betcoin", entity.getBetcoin());
            doc.append("bp", entity.getBp());
            doc.append("choushui", entity.getChoushui());
            doc.append("codeamount", entity.getCodeamount());
            doc.append("coin", entity.getCoin());
            doc.append("coinenter", entity.getCoinenter());
            doc.append("coinquit", entity.getCoinquit());
            doc.append("endtime", entity.getEndtime());
            doc.append("gameno", entity.getGameno());
            doc.append("gid", entity.getGid());
            doc.append("grade", entity.getGrade());
            doc.append("groups", entity.getGroups());
            doc.append("id", entity.getId());
            doc.append("mid", entity.getMid());
            doc.append("muid", entity.getMuid());
            doc.append("roomid", entity.getRoomid());
            doc.append("sync", entity.getSync());
            doc.append("uid", entity.getUid());
            doc.append("wlcoin", entity.getWlcoin());
            col.updateOne((Bson)conditions, (Bson)new Document("$set", doc));
            return true;
        }
        catch (Exception ex) {
            LOGGER.error("Save item FishGameRecord error: ", (Throwable)ex);
            return false;
        }
    }

    @Override
    public Long getLastUpdateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION_FISHRECORD);
            Document sort = new Document("endtime", -1);
            FindIterable iterable = collection.find().sort((Bson)sort).limit(1);
            Document doc = iterable != null ? (Document)iterable.first() : null;
            String lastUpdateTime = "";
            if (doc != null) {
                lastUpdateTime = doc.getString("endtime");
            }
            return DateUtils.addHours((Date)simpleDateFormat.parse(lastUpdateTime), (int)1).getTime();
        }
        catch (Exception e) {
            return DateUtils.addDays((Date)new Date(), (int)-7).getTime();
        }
    }
}


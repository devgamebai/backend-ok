/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.types.ObjectId
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogEbetDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

public class LogEbetDaoImpl
implements LogEbetDao {
    private static final Logger LOGGER = Logger.getLogger(LogEbetDaoImpl.class);
    private static final String COLLECTION_EBETRECORD = "ebetgamerecord";

    @Override
    public Map<String, Object> search(String nickName, String timeStart, String timeEnd, int flagTime, String ebetId, int page, int limitItem) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", (timeStart + " 00:00:00"));
                obj.put("$lte", (timeEnd + " 23:59:59"));
                switch (flagTime) {
                    case 1: {
                        conditions.put("createtime", obj);
                        break;
                    }
                    case 2: {
                        conditions.put("payouttime", obj);
                    }
                }
            }
            if (!StringUtils.isBlank((String)nickName)) {
                conditions.append("nick_name", nickName);
            }
            if (!StringUtils.isBlank((String)ebetId)) {
                conditions.append("ebetid", ebetId);
            }
            Document match = new Document("$match", conditions);
            Document sort = new Document();
            switch (flagTime) {
                case 1: {
                    sort = new Document("$sort", new Document("createtime", -1));
                    break;
                }
                case 2: {
                    sort = new Document("$sort", new Document("payouttime", -1));
                    break;
                }
                default: {
                    sort = new Document("$sort", new Document("createtime", -1));
                }
            }
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION_EBETRECORD);
            int num_start = limitItem == -1 ? 0 : (page - 1) * limitItem;
            int num_end = limitItem == -1 ? 50 : limitItem;
            Document skip = new Document();
            skip = new Document("$skip", num_start);
            Document limit = new Document();
            limit = new Document("$limit", num_end);
            Document addField = new Document();
            addField = new Document("$addFields", new BasicDBObject("id", new Document("$toString", "$_id")));
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            List result = new ArrayList();
            result = (List)collection.aggregate(Arrays.asList(match, addField, project, sort, skip, limit)).allowDiskUse(Boolean.valueOf(true)).into(new ArrayList());
            data.put("ebetrecords", result);
            if (result.size() == 0 || result.isEmpty()) {
                data.put("totalRecord", 0);
                data.put("totalPlayer", 0);
                return data;
            }
            Document count = new Document();
            count = new Document("$count", "nick_name");
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _rawObj1 : aggregateCount) {
                Document document = (Document) _rawObj1;
                try {
                    data.put("totalRecord", document.getInteger("nick_name", 0));
                }
                catch (Exception exception) {}
            }
            Document sumBet = new Document();
            BasicDBObject totalBetCondis = new BasicDBObject();
            totalBetCondis.put("_id", 0);
            totalBetCondis.put("totalBet", new BasicDBObject("$sum", "$bet"));
            totalBetCondis.put("totalValidbet", new BasicDBObject("$sum", "$validbet"));
            sumBet = new Document("$group", totalBetCondis);
            Long totalBet = 0L;
            Long totalValidbet = 0L;
            AggregateIterable aggregateTotalBet = collection.aggregate(Arrays.asList(match, sumBet)).allowDiskUse(Boolean.valueOf(true));
            for (Object _rawObj2 : aggregateTotalBet) {
                Document document = (Document) _rawObj2;
                try {
                    totalBet = document.getLong("totalBet");
                    totalValidbet = document.getLong("totalBet");
                }
                catch (Exception exception) {}
            }
            data.put("totalBet", totalBet);
            data.put("totalValidbet", totalValidbet);
            Document matchPlayers = new Document();
            if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
                switch (flagTime) {
                    case 1: {
                        matchPlayers = new Document("$match", new Document("createtime", new Document("$gte", (timeStart + " 00:00:00")).append("$lte", (timeEnd + " 23:59:59"))));
                        break;
                    }
                    case 2: {
                        matchPlayers = new Document("$match", new Document("payouttime", new Document("$gte", (timeStart + " 00:00:00")).append("$lte", (timeEnd + " 23:59:59"))));
                        break;
                    }
                    default: {
                        matchPlayers = new Document("$match", new Document("createtime", new Document("$exists", true)));
                        break;
                    }
                }
            } else {
                matchPlayers = new Document("$match", new Document("createtime", new Document("$exists", true)));
            }
            Document group = new Document();
            group = new Document("$group", new Document("_id", "$nick_name"));
            AggregateIterable aggregateCountPlayer = collection.aggregate(Arrays.asList(matchPlayers, group, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _rawObj3 : aggregateCountPlayer) {
                Document document = (Document) _rawObj3;
                try {
                    data.put("totalPlayer", document.getInteger("nick_name", 0));
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            LOGGER.error(("Search EbetGameRecord error: " + e.getMessage()));
            data = new HashMap();
            data.put("ebetrecords", new ArrayList());
            data.put("totalRecord", 0);
            data.put("totalPlayer", 0);
        }
        return data;
    }

    @Override
    public Object detail(String id) {
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (StringUtils.isBlank((String)id)) {
                return null;
            }
            conditions.put("_id", new ObjectId(id));
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION_EBETRECORD);
            return collection.aggregate(Arrays.asList(match, project)).allowDiskUse(Boolean.valueOf(true)).first();
        }
        catch (Exception e) {
            LOGGER.error(("Search EbetGameRecord error: " + e.getMessage()));
            return null;
        }
    }
}


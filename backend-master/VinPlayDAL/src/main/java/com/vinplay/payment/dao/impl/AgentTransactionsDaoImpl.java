/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.payment.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.payment.dao.AgentTransactionsDao;
import com.vinplay.payment.entities.AgentTransaction;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class AgentTransactionsDaoImpl
implements AgentTransactionsDao {
    public static final Logger logger = Logger.getLogger(AgentTransactionsDaoImpl.class);
    private static final String COLLECTION = "agent_transactions";
    private static final String DEPOSIT_COLLECTION = "deposit_paygate";
    private static final String WITHDRAW_COLLECTION = "money_user_pay_to_agent";

    @Override
    public long create(AgentTransaction model) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection col = db.getCollection(COLLECTION);
            Document doc = new Document();
            long Id = VinPlayUtils.generateTransId();
            doc.append("Id", String.valueOf(Id));
            doc.append("CreatedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("IsDeleted", false);
            doc.append("AgentId", model.AgentId);
            doc.append("Username", model.Username);
            doc.append("Nickname", model.Nickname);
            doc.append("AgentCode", model.AgentCode);
            doc.append("RequestTime", VinPlayUtils.getCurrentDateTime());
            doc.append("Point", model.Point);
            doc.append("Money", model.Money);
            doc.append("Fee", model.Fee);
            doc.append("Bonus", model.Bonus);
            doc.append("Status", PayCommon.PAYSTATUS.PENDING.getId());
            doc.append("FromBankNumber", model.FromBankNumber);
            doc.append("ToBankNumber", model.ToBankNumber);
            doc.append("Content", model.Content);
            doc.append("Description", model.Description);
            doc.append("UserApprove", model.UserApprove);
            col.insertOne(doc);
            return Id;
        }
        catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Boolean updateStatus(String id, int status, long fee, String description, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Status", status);
            updateFields.append("Fee", fee);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("Description", description);
            updateFields.append("UserApprove", userApprove);
            db.getCollection(COLLECTION).updateOne((Bson)new Document("Id", id), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean updateStatus(String id, int status, String description, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Status", status);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("Description", description);
            updateFields.append("UserApprove", userApprove);
            db.getCollection(COLLECTION).updateOne((Bson)new Document("Id", id), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean delete(String id, String description, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("IsDeleted", true);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("Description", description);
            updateFields.append("UserApprove", userApprove);
            db.getCollection(COLLECTION).updateOne((Bson)new Document("Id", id), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public AgentTransaction getById(String Id) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document conditions = new Document();
            conditions.put("Id", Id);
            Document document = (Document)db.getCollection(COLLECTION).find((Bson)conditions).first();
            if (document == null) {
                return null;
            }
            AgentTransaction model = new AgentTransaction(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("AgentId"), document.getString("Username"), document.getString("Nickname"), document.getString("AgentCode"), document.getString("RequestTime"), document.getLong("Point"), document.getLong("Money"), document.getLong("Fee"), document.getLong("Bonus"), document.getInteger("Status"), document.getString("ToBankNumber"), document.getString("FromBankNumber"), document.getString("Content"), document.getString("Description"), document.getString("UserApprove"));
            return model;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public Map<String, Object> search(String keyword, int status, String timeStart, String timeEnd, int page) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            ArrayList<BasicDBObject> lstConditions = new ArrayList<BasicDBObject>();
            BasicDBObject condCreatedAt = new BasicDBObject();
            if (!StringUtils.isBlank((String)timeStart) && !StringUtils.isBlank((String)timeEnd)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", (timeStart + " 00:00:00"));
                obj.put("$lte", (timeEnd + " 23:59:59"));
                condCreatedAt.put("RequestTime", obj);
                lstConditions.add(condCreatedAt);
            }
            BasicDBObject condKeyword = new BasicDBObject();
            ArrayList<BasicDBObject> lstKeyword = new ArrayList<BasicDBObject>();
            if (!StringUtils.isBlank((String)keyword)) {
                lstKeyword.add(new BasicDBObject("AgentId", keyword));
                lstKeyword.add(new BasicDBObject("Username", keyword));
                lstKeyword.add(new BasicDBObject("Nickname", keyword));
                lstKeyword.add(new BasicDBObject("AgentCode", keyword));
                condKeyword.put("$or", lstKeyword);
                lstConditions.add(condKeyword);
            }
            BasicDBObject condStatus = new BasicDBObject();
            if (status > -1) {
                condStatus.put("Status", status);
                lstConditions.add(condStatus);
            }
            BasicDBObject condIsDel = new BasicDBObject();
            condIsDel.put("IsDeleted", false);
            lstConditions.add(condIsDel);
            BasicDBObject conditions = new BasicDBObject();
            conditions.put("$and", lstConditions);
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            Document sort = new Document();
            sort = new Document("$sort", new Document("CreatedAt", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION);
            int num_start = (page - 1) * 50;
            int num_end = 50;
            Document skip = new Document();
            skip = new Document("$skip", num_start);
            Document limit = new Document();
            limit = new Document("$limit", num_end);
            List result = new ArrayList();
            result = (List)collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).into(new ArrayList());
            data.put("transactions", result);
            Document count = new Document();
            count = new Document("$count", "Nickname");
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateCount) {
                try { Document document = (Document) _obj;
                    data.put("totalRecord", document.getInteger("Nickname", 0));
                }
                catch (Exception exception) {}
            }
            Document sumMoney = new Document();
            BasicDBObject totalBetCondis = new BasicDBObject();
            totalBetCondis.put("_id", 0);
            totalBetCondis.put("totalMoney", new BasicDBObject("$sum", "$Money"));
            sumMoney = new Document("$group", totalBetCondis);
            Long totalMoney = 0L;
            AggregateIterable aggregateTotalBet = collection.aggregate(Arrays.asList(match, sumMoney)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateTotalBet) {
                try { Document document = (Document) _obj;
                    totalMoney = document.getLong("totalMoney");
                }
                catch (Exception exception) {}
            }
            data.put("totalMoney", totalMoney);
            Document group = new Document();
            group = new Document("$group", new Document("_id", "$Nickname"));
            AggregateIterable aggregateCountPlayer = collection.aggregate(Arrays.asList(match, group, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateCountPlayer) {
                try { Document document = (Document) _obj;
                    data.put("totalPlayer", document.getInteger("Nickname", 0));
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            logger.error(("Search AgentTransactions error: " + e.getMessage()));
            data.put("transactions", new ArrayList());
            data.put("totalRecord", 0);
            data.put("totalPlayer", 0);
            data.put("totalMoney", 0);
        }
        return data;
    }

    @Override
    public Map<String, Object> searchWithAgentCode(String agentCode, int status, String timeStart, String timeEnd, int page) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            ArrayList<BasicDBObject> lstConditions = new ArrayList<BasicDBObject>();
            BasicDBObject condCreatedAt = new BasicDBObject();
            if (!StringUtils.isBlank((String)timeStart) && !StringUtils.isBlank((String)timeEnd)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", (timeStart + " 00:00:00"));
                obj.put("$lte", (timeEnd + " 23:59:59"));
                condCreatedAt.put("RequestTime", obj);
                lstConditions.add(condCreatedAt);
            }
            BasicDBObject condKeyword = new BasicDBObject();
            ArrayList<BasicDBObject> lstKeyword = new ArrayList<BasicDBObject>();
            if (!StringUtils.isBlank((String)agentCode)) {
                lstKeyword.add(new BasicDBObject("AgentCode", agentCode));
                condKeyword.put("$or", lstKeyword);
                lstConditions.add(condKeyword);
            }
            BasicDBObject condStatus = new BasicDBObject();
            if (status > -1) {
                condStatus.put("Status", status);
                lstConditions.add(condStatus);
            }
            BasicDBObject condIsDel = new BasicDBObject();
            condIsDel.put("IsDeleted", false);
            lstConditions.add(condIsDel);
            BasicDBObject conditions = new BasicDBObject();
            conditions.put("$and", lstConditions);
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0));
            Document sort = new Document();
            sort = new Document("$sort", new Document("CreatedAt", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(COLLECTION);
            int num_start = (page - 1) * 50;
            int num_end = 50;
            Document skip = new Document();
            skip = new Document("$skip", num_start);
            Document limit = new Document();
            limit = new Document("$limit", num_end);
            List result = new ArrayList();
            result = (List)collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).into(new ArrayList());
            data.put("transactions", result);
            if (result.size() == 0 || result.isEmpty()) {
                data.put("totalRecord", 0);
                data.put("totalPlayer", 0);
                data.put("totalMoney", 0);
                return data;
            }
            Document count = new Document();
            count = new Document("$count", "Nickname");
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateCount) {
                try { Document document = (Document) _obj;
                    data.put("totalRecord", document.getInteger("Nickname", 0));
                }
                catch (Exception exception) {}
            }
            Document sumMoney = new Document();
            BasicDBObject totalBetCondis = new BasicDBObject();
            totalBetCondis.put("_id", 0);
            totalBetCondis.put("totalMoney", new BasicDBObject("$sum", "$Money"));
            sumMoney = new Document("$group", totalBetCondis);
            Long totalMoney = 0L;
            AggregateIterable aggregateTotalBet = collection.aggregate(Arrays.asList(match, sumMoney)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateTotalBet) {
                try { Document document = (Document) _obj;
                    totalMoney = document.getLong("totalMoney");
                }
                catch (Exception exception) {}
            }
            data.put("totalMoney", totalMoney);
            Document group = new Document();
            group = new Document("$group", new Document("_id", "$Nickname"));
            AggregateIterable aggregateCountPlayer = collection.aggregate(Arrays.asList(match, group, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregateCountPlayer) {
                try { Document document = (Document) _obj;
                    data.put("totalPlayer", document.getInteger("Nickname", 0));
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            logger.error(("Search AgentTransactions error: " + e.getMessage()));
            data = new HashMap();
            data.put("transactions", new ArrayList());
            data.put("totalRecord", 0);
            data.put("totalPlayer", 0);
            data.put("totalMoney", 0);
        }
        return data;
    }

    @Override
    public List<Document> getTotalTransferOutTransaction(List<AgentResponse> agentList, String fromTime, String endTime) {
        ArrayList<Document> data = new ArrayList<Document>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)fromTime) && !StringUtils.isBlank((String)endTime)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("RequestTime", obj);
            }
            List agentNicknameList = agentList.stream().map(item -> item.nickName).collect(Collectors.toList());
            conditions.put("UserApprove", new BasicDBObject().append("$in", agentNicknameList));
            ArrayList<BasicDBObject> criteria = new ArrayList<BasicDBObject>();
            criteria.add(new BasicDBObject("Nickname", new BasicDBObject().append("$nin", agentNicknameList)));
            criteria.add(new BasicDBObject("UserApprove", new BasicDBObject().append("$nin", agentNicknameList)));
            conditions.put("$or", criteria);
            conditions.put("Nickname", new BasicDBObject("$ne", "dailytong").append("$ne", "tongdaily"));
            Document match = new Document("$match", conditions);
            BasicDBObject groupFields = new BasicDBObject("_id", "$UserApprove");
            groupFields.put("total_transfer", new BasicDBObject("$sum", "$Amount"));
            Document group = new Document("$group", groupFields);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 1).append("total_transfer", 1));
            BasicDBObject limit = new BasicDBObject("$limit", 9999);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(DEPOSIT_COLLECTION);
            AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, group, limit, project)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregate) {
                try { Document document = (Document) _obj;
                    data.add(document);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            logger.error(("Get Agent TransferOut error: " + e.getMessage()));
        }
        return data;
    }

    @Override
    public List<Document> getTotalTransferInTransaction(List<AgentResponse> agentList, String fromTime, String endTime) {
        ArrayList<Document> data = new ArrayList<Document>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)fromTime) && !StringUtils.isBlank((String)endTime)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse(fromTime.replace(' ', 'T') + " +0700"));
                obj.put("$lte", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse(endTime.replace(' ', 'T') + " +0700"));
                conditions.put("create_time", obj);
            }
            List agentNicknameList = agentList.stream().map(item -> item.nickName).collect(Collectors.toList());
            agentNicknameList.add("dailytong");
            agentNicknameList.add("tongdaily");
            conditions.put("nick_name", new BasicDBObject().append("$nin", agentNicknameList));
            Document match = new Document("$match", conditions);
            BasicDBObject groupFields = new BasicDBObject("_id", "$agent");
            groupFields.put("total_transfer", new BasicDBObject("$sum", "$money_exchange"));
            Document group = new Document("$group", groupFields);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 1).append("total_transfer", 1));
            BasicDBObject limit = new BasicDBObject("$limit", 9999);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(WITHDRAW_COLLECTION);
            AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, group, limit, project)).allowDiskUse(Boolean.valueOf(true));
            for (Object _obj : aggregate) {
                try { Document document = (Document) _obj;
                    data.add(document);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            logger.error(("Get Agent TransferIn error: " + e.getMessage()));
        }
        return data;
    }
}


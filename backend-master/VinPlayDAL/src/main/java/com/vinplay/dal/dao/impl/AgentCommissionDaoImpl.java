package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.dal.dao.AgentCommissionDao;
import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class AgentCommissionDaoImpl implements AgentCommissionDao {

    private static final String COLLECTION_NAME = "agent_commission_daily";

    @Override
    public void upsert(AgentCommissionDaily record) {
        if (record == null) return;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

        BasicDBObject conditions = new BasicDBObject();
        conditions.append("date", record.getDate());
        conditions.append("userNickname", record.getUserNickname());

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("date", record.getDate());
        updateFields.append("userNickname", record.getUserNickname());
        updateFields.append("userCommissionRate", record.getUserCommissionRate());
        updateFields.append("referralCode", record.getReferralCode());
        updateFields.append("totalBet", record.getTotalBet());
        updateFields.append("totalBetCasino", record.getTotalBetCasino());
        updateFields.append("totalBetSport", record.getTotalBetSport());
        updateFields.append("totalBetGame", record.getTotalBetGame());
        updateFields.append("userCommission", record.getUserCommission());
        updateFields.append("createTime", record.getCreateTime());

        if (record.getDistributions() != null && !record.getDistributions().isEmpty()) {
            List<Document> distDocs = new ArrayList<>();
            for (AgentCommissionDaily.AgentDistribution d : record.getDistributions()) {
                Document distDoc = new Document();
                distDoc.append("agentCode", d.getAgentCode());
                distDoc.append("agentNickname", d.getAgentNickname());
                distDoc.append("agentLevel", d.getAgentLevel());
                distDoc.append("agentRate", d.getAgentRate());
                distDoc.append("earnRate", d.getEarnRate());
                distDoc.append("commission", d.getCommission());
                distDocs.add(distDoc);
            }
            updateFields.append("distributions", distDocs);
        } else {
            updateFields.append("distributions", new ArrayList<>());
        }

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson) conditions, (Bson) new Document("$set", updateFields), options);
    }

    @Override
    public void upsertBatch(List<AgentCommissionDaily> records) {
        if (records == null || records.isEmpty()) return;
        for (AgentCommissionDaily record : records) {
            upsert(record);
        }
    }

    @Override
    public List<AgentCommissionDaily> searchByAgent(String agentCode, String fromDate, String toDate, int page, int limit) {
        List<AgentCommissionDaily> results = new ArrayList<>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

        BasicDBObject conditions = new BasicDBObject();
        conditions.append("distributions.agentCode", agentCode);
        if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
            BasicDBObject dateRange = new BasicDBObject();
            dateRange.put("$gte", fromDate);
            dateRange.put("$lte", toDate);
            conditions.append("date", dateRange);
        }

        BasicDBObject sort = new BasicDBObject("date", -1);
        int skip = (page - 1) * limit;
        skip = skip < 0 ? 0 : skip;

        try (MongoCursor<Document> cursor = col.find((Bson) conditions).sort((Bson) sort).skip(skip).limit(limit).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                results.add(mapDocumentToEntity(doc));
            }
        }
        return results;
    }

    @Override
    public long countByAgent(String agentCode, String fromDate, String toDate) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

        BasicDBObject conditions = new BasicDBObject();
        conditions.append("distributions.agentCode", agentCode);
        if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
            BasicDBObject dateRange = new BasicDBObject();
            dateRange.put("$gte", fromDate);
            dateRange.put("$lte", toDate);
            conditions.append("date", dateRange);
        }
        return col.count((Bson) conditions);
    }

    @Override
    public List<AgentCommissionDaily> searchByUser(String agentCode, String userNickname, String fromDate, String toDate, int page, int limit) {
        List<AgentCommissionDaily> results = new ArrayList<>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

        BasicDBObject conditions = new BasicDBObject();
        conditions.append("distributions.agentCode", agentCode);
        if (userNickname != null && !userNickname.isEmpty()) {
            conditions.append("userNickname", userNickname);
        }
        if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
            BasicDBObject dateRange = new BasicDBObject();
            dateRange.put("$gte", fromDate);
            dateRange.put("$lte", toDate);
            conditions.append("date", dateRange);
        }

        BasicDBObject sort = new BasicDBObject("date", -1);
        int skip = (page - 1) * limit;
        skip = skip < 0 ? 0 : skip;

        try (MongoCursor<Document> cursor = col.find((Bson) conditions).sort((Bson) sort).skip(skip).limit(limit).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                results.add(mapDocumentToEntity(doc));
            }
        }
        return results;
    }

    @Override
    public long countByUser(String agentCode, String userNickname, String fromDate, String toDate) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

        BasicDBObject conditions = new BasicDBObject();
        conditions.append("distributions.agentCode", agentCode);
        if (userNickname != null && !userNickname.isEmpty()) {
            conditions.append("userNickname", userNickname);
        }
        if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
            BasicDBObject dateRange = new BasicDBObject();
            dateRange.put("$gte", fromDate);
            dateRange.put("$lte", toDate);
            conditions.append("date", dateRange);
        }
        return col.count((Bson) conditions);
    }

    private AgentCommissionDaily mapDocumentToEntity(Document doc) {
        AgentCommissionDaily entity = new AgentCommissionDaily();
        entity.setDate(doc.getString("date"));
        entity.setUserNickname(doc.getString("userNickname"));
        entity.setUserCommissionRate(getDouble(doc, "userCommissionRate", 0));
        entity.setReferralCode(doc.getString("referralCode"));
        entity.setTotalBet(getLong(doc, "totalBet", 0));
        entity.setTotalBetCasino(getLong(doc, "totalBetCasino", 0));
        entity.setTotalBetSport(getLong(doc, "totalBetSport", 0));
        entity.setTotalBetGame(getLong(doc, "totalBetGame", 0));
        entity.setUserCommission(getLong(doc, "userCommission", 0));
        entity.setCreateTime(doc.getDate("createTime"));

        @SuppressWarnings("unchecked")
        List<Document> distList = (List<Document>) doc.get("distributions");
        if (distList != null && !distList.isEmpty()) {
            List<AgentCommissionDaily.AgentDistribution> distributions = new ArrayList<>();
            for (Document d : distList) {
                AgentCommissionDaily.AgentDistribution ad = new AgentCommissionDaily.AgentDistribution();
                ad.setAgentCode(d.getString("agentCode"));
                ad.setAgentNickname(d.getString("agentNickname"));
                ad.setAgentLevel(getInt(d, "agentLevel", 0));
                ad.setAgentRate(getDouble(d, "agentRate", 0));
                ad.setEarnRate(getDouble(d, "earnRate", 0));
                ad.setCommission(getLong(d, "commission", 0));
                distributions.add(ad);
            }
            entity.setDistributions(distributions);
        }
        return entity;
    }

    private static long getLong(Document doc, String key, long defaultValue) {
        Object val = doc.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).longValue();
        return defaultValue;
    }

    private static int getInt(Document doc, String key, int defaultValue) {
        Object val = doc.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }

    private static double getDouble(Document doc, String key, double defaultValue) {
        Object val = doc.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultValue;
    }
}

/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.models.MarketingModel
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.usercore.dao.UserMarketingDao;
import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class UserMarketingDaoImpl
implements UserMarketingDao {
    @Override
    public boolean saveUserMarketing(UserMarketingMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String timeLog = df.format(new Date());
        col = db.getCollection("user_marketing");
        Document doc = new Document();
        doc.append("user_name", message.getUserName());
        doc.append("utm_campaign", message.getUtmCampaign());
        doc.append("utm_medium", message.getUtmMedium());
        doc.append("utm_source", message.getUtmSource());
        doc.append("time_login", timeLog);
        col.insertOne(doc);
        return true;
    }

    @Override
    public boolean saveLoginDailyMarketing(UserMarketingMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String timeLog = df.format(new Date());
        col = db.getCollection("login_daily_marketing");
        Document doc = new Document();
        doc.append("user_name", message.getUserName());
        doc.append("num_login", message.getNumLogin());
        doc.append("time_login", timeLog);
        col.insertOne(doc);
        return true;
    }

    @Override
    public boolean updateLoginDailyMarketing(UserMarketingMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        col = db.getCollection("login_daily_marketing");
        col.updateOne((Bson)new Document("user_name", message.getUserName()), (Bson)new Document("$set", new Document("num_login", message.getNumLogin())));
        return true;
    }

    @Override
    public List<UserMarketingMessage> getNickNameUserMarketing(String nickName, String timeLogin) {
        final ArrayList<UserMarketingMessage> results = new ArrayList<UserMarketingMessage>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("user_name", nickName);
        conditions.put("time_login", timeLogin);
        FindIterable iterable = db.getCollection("login_daily_marketing").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                UserMarketingMessage usermarket = new UserMarketingMessage();
                usermarket.nickName = document.getString("nick_name");
                usermarket.numLogin = document.getInteger("num_login");
                results.add(usermarket);
            }
        });
        return results;
    }

    public MarketingModel marketingToolFromDate(String campaign, String source, String medium, String startDate, String endDate) throws SQLException {
        return null;
    }

    private List<String> getMKTList(String tableName, String condition) throws SQLException {
        ArrayList<String> results = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM " + tableName;
            if (condition != null && !condition.equals("")) {
                sql = sql + " WHERE name='" + condition + "'";
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
            rs.close();
            stm.close();
        }
        return results;
    }

    @Override
    public List<String> getCampaignList(String condition) throws SQLException {
        return this.getMKTList("utm_campain", condition);
    }

    @Override
    public List<String> getSourceList(String condition) throws SQLException {
        return this.getMKTList("utm_source", condition);
    }

    @Override
    public List<String> getMediumList(String condition) throws SQLException {
        return this.getMKTList("utm_medium", condition);
    }

    @Override
    public void logMKTInfo(StatisticUserMarketing entry) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("statistic_mkt_daily");
        Document doc = new Document();
        doc.append("utm_campaign", entry.getCampaign());
        doc.append("utm_medium", entry.getMedium());
        doc.append("utm_source", entry.getSource());
        doc.append("nru", entry.getNRU());
        doc.append("pu", entry.getPU());
        doc.append("nap_vin", entry.getTotalNapVin());
        doc.append("tieu_vin", entry.getTotalTieuVin());
        doc.append("time_log", entry.getDate());
        col.insertOne(doc);
    }

    @Override
    public List<MarketingModel> getHistoryMKT(String campaign, String medium, String source, String startDate, String endDate) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        final ArrayList<MarketingModel> results = new ArrayList<MarketingModel>();
        if (campaign != null && !campaign.isEmpty()) {
            conditions.put("utm_campaign", campaign);
        }
        if (source != null && !source.isEmpty()) {
            conditions.put("utm_source", source);
        }
        if (medium != null && !medium.isEmpty()) {
            conditions.put("utm_medium", medium);
        }
        if (startDate != null && endDate != null) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startDate);
            obj.put("$lte", endDate);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = db.getCollection("statistic_mkt_daily").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                MarketingModel model = new MarketingModel();
                model.campaign = document.getString("utm_campaign");
                model.medium = document.getString("utm_medium");
                model.source = document.getString("utm_source");
                model.NRU = document.getInteger("nru");
                model.PU = document.getInteger("pu");
                model.doanhthu = document.getLong("nap_vin");
                model.dateStr = document.getString("time_log");
                results.add(model);
            }
        });
        return results;
    }

    @Override
    public MarketingModel getMKTInfo(String username) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("user_name", username);
        FindIterable iterable = db.getCollection("user_marketing").find((Bson)new Document(conditions));
        final MarketingModel model = new MarketingModel();
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                model.campaign = document.getString("utm_campaign");
                model.medium = document.getString("utm_medium");
                model.source = document.getString("utm_source");
            }
        });
        return model;
    }

}


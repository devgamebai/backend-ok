/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.TranferMoneyResponse
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.ListMoneyTranferDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.TranferMoneyResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ListMoneyTranferDAOImpl
implements ListMoneyTranferDAO {
    @Override
    public int countTotalRecord(String nickName, int isFreezeMoney, int page, int timeSearch) {
        int results = 0;
        if (nickName != null && !nickName.isEmpty()) {
            results = "all".equals(nickName) ? this.countAllMoneyTranfer(isFreezeMoney, page, timeSearch) : this.countMoneyTranferByNickName(nickName, isFreezeMoney, page, timeSearch);
        }
        return results;
    }

    private int countAllMoneyTranfer(int isFreezeMoney, int page, int timeSearch) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        BasicDBObject obj = new BasicDBObject();
        obj.put("$exists", true);
        obj.put("$ne", "");
        conditions.put("agent_level1", obj);
        if (isFreezeMoney != -1) {
            conditions.put("is_freeze_money", isFreezeMoney);
        }
        conditions.put("trans_time", new BasicDBObject("$gte", VinPlayUtils.parseDateTimeToString((Date)VinPlayUtils.subtractDay((Date)new Date(), (int)timeSearch))));
        int record = (int)db.getCollection("log_chuyen_tien_dai_ly").count((Bson)new Document((Map)conditions));
        return record;
    }

    private int countMoneyTranferByNickName(String nickName, int isFreezeMoney, int page, int timeSearch) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        conditions.put("agent_level1", nickName);
        if (isFreezeMoney != -1) {
            conditions.put("is_freeze_money", isFreezeMoney);
        }
        conditions.put("trans_time", new BasicDBObject("$gte", VinPlayUtils.parseDateTimeToString((Date)VinPlayUtils.subtractDay((Date)new Date(), (int)timeSearch))));
        int record = (int)db.getCollection("log_chuyen_tien_dai_ly").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public List<TranferMoneyResponse> listMoneyTranfer(String nickName, int isFreezeMoney, int page, int timeSearch) {
        List<TranferMoneyResponse> results = new ArrayList<TranferMoneyResponse>();
        if (nickName != null && !nickName.isEmpty()) {
            results = "all".equals(nickName) ? this.listAllMoneyTranfer(isFreezeMoney, page, timeSearch) : this.listMoneyTranferByNickName(nickName, isFreezeMoney, page, timeSearch);
        }
        return results;
    }

    private List<TranferMoneyResponse> listAllMoneyTranfer(int isFreezeMoney, int page, int timeSearch) {
        final ArrayList<TranferMoneyResponse> results = new ArrayList<TranferMoneyResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        int num_start = (page - 1) * 50;
        int num_end = 50;
        BasicDBObject obj = new BasicDBObject();
        obj.put("$exists", true);
        obj.put("$ne", "");
        conditions.put("agent_level1", obj);
        if (isFreezeMoney != -1) {
            conditions.put("is_freeze_money", isFreezeMoney);
        }
        conditions.put("trans_time", new BasicDBObject("$gte", VinPlayUtils.parseDateTimeToString((Date)VinPlayUtils.subtractDay((Date)new Date(), (int)timeSearch))));
        iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TranferMoneyResponse message = new TranferMoneyResponse(document.getString("nick_name_send"), document.getString("nick_name_receive"), document.getLong("money_send").longValue(), document.getLong("money_receive").longValue(), document.getLong("fee").longValue(), document.getInteger("status").intValue(), document.getString("trans_time"), document.getString("des_send"), document.getString("des_receive"), document.getString("transaction_no"), document.getInteger("is_freeze_money").intValue(), document.getString("agent_level1"));
                results.add(message);
            }
        });
        return results;
    }

    private List<TranferMoneyResponse> listMoneyTranferByNickName(String nickName, int isFreezeMoney, int page, int timeSearch) {
        final ArrayList<TranferMoneyResponse> results = new ArrayList<TranferMoneyResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        int num_start = (page - 1) * 50;
        int num_end = 50;
        conditions.put("agent_level1", nickName);
        if (isFreezeMoney != -1) {
            conditions.put("is_freeze_money", isFreezeMoney);
        }
        conditions.put("trans_time", new BasicDBObject("$gte", VinPlayUtils.parseDateTimeToString((Date)VinPlayUtils.subtractDay((Date)new Date(), (int)timeSearch))));
        iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TranferMoneyResponse message = new TranferMoneyResponse(document.getString("nick_name_send"), document.getString("nick_name_receive"), document.getLong("money_send").longValue(), document.getLong("money_receive").longValue(), document.getLong("fee").longValue(), document.getInteger("status").intValue(), document.getString("trans_time"), document.getString("des_send"), document.getString("des_receive"), document.getString("transaction_no"), document.getInteger("is_freeze_money").intValue(), document.getString("agent_level1"));
                results.add(message);
            }
        });
        return results;
    }

    @Override
    public TranferMoneyResponse getMoneyTranferByTransNo(String transNo) {
        final ArrayList results = new ArrayList();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        if (transNo != null && !transNo.isEmpty()) {
            conditions.put("transaction_no", transNo);
        }
        iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson)new Document((Map)conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TranferMoneyResponse message = new TranferMoneyResponse(document.getString("nick_name_send"), document.getString("nick_name_receive"), document.getLong("money_send").longValue(), document.getLong("money_receive").longValue(), document.getLong("fee").longValue(), document.getInteger("status").intValue(), document.getString("trans_time"), document.getString("des_send"), document.getString("des_receive"), document.getString("transaction_no"), document.getInteger("is_freeze_money").intValue(), document.getString("agent_level1"));
                results.add(message);
            }
        });
        if (results.size() > 0) {
            return (TranferMoneyResponse)results.get(0);
        }
        return null;
    }

}


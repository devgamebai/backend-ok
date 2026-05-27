/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.FindOneAndUpdateOptions
 *  com.vinplay.vbee.common.messages.BrandnameDLVRMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.PhoneUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.brandname.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.brandname.dao.BrandnameDao;
import com.vinplay.brandname.enties.LogBrandname;
import com.vinplay.dichvuthe.utils.DvtUtils;
import com.vinplay.doisoat.entities.DoisoatBrandname;
import com.vinplay.doisoat.entities.DoisoatBrandnameProvider;
import com.vinplay.usercore.response.LogBrandnameResponse;
import com.vinplay.vbee.common.messages.BrandnameDLVRMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.PhoneUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class BrandnameDaoImpl
implements BrandnameDao {
    @Override
    public int getLastRequestId() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("brandname_transaction").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        Document document = iterable != null ? (Document)iterable.first() : null;
        int requestId = document == null ? 0 : Integer.parseInt(document.getString("request_id"));
        return requestId;
    }

    @Override
    public boolean updateMessageDLVR(BrandnameDLVRMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("brandname_transaction");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("status", message.getSmsStatus());
        updateFields.append("count", message.getCount());
        updateFields.append("status_desc", message.getStatusDesc());
        updateFields.append("sent_date", message.getSentDate());
        updateFields.append("update_time", message.getCreateTime());
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("request_id", message.getRequestId());
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(false);
        Document dc = (Document)col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$set", updateFields), options);
        boolean res = dc != null;
        return res;
    }

    @Override
    public LogBrandnameResponse getLogBrandname(String mobile, String code, String status, String startTime, String endTime, int page, String requestId) {
        final ArrayList<LogBrandname> logBrandname = new ArrayList<LogBrandname>();
        final ArrayList<Integer> num = new ArrayList<Integer>();
        num.add(0, 0);
        num.add(1, 0);
        num.add(2, 0);
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (!mobile.isEmpty()) {
            mobile = DvtUtils.revertMobile0To84(mobile);
            String pattern = ".*" + mobile + ".*";
            conditions.put("receives", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        if (!code.isEmpty()) {
            conditions.put("code", code);
        }
        if (!status.isEmpty()) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        if (requestId != null && !requestId.isEmpty()) {
            conditions.put("request_id", requestId);
        }
        FindIterable iterable = db.getCollection("brandname_transaction").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogBrandname model = new LogBrandname();
                model.requestId = document.getString("request_id");
                model.message = document.getString("message");
                model.receives = DvtUtils.revertMobile84To(document.getString("receives"));
                model.transTime = document.getString("trans_time");
                model.status = document.getInteger("status");
                model.count = document.getInteger("count");
                model.statusDesc = document.getString("status_desc");
                model.updateTime = document.getString("update_time");
                logBrandname.add(model);
            }
        });
        FindIterable iterable2 = db.getCollection("brandname_transaction").find((Bson)new Document(conditions));
        iterable2.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogBrandname model = new LogBrandname();
                model.status = document.getInteger("status");
                model.count = document.getInteger("count");
                int count = (Integer)num.get(0) + 1;
                num.set(0, count);
                int numSend = (Integer)num.get(1) + DvtUtils.calculateMessageBN(model.message);
                num.set(1, numSend);
                if (model.status == 1) {
                    int numSuccess = (Integer)num.get(2) + model.count;
                    num.set(2, numSuccess);
                }
            }
        });
        LogBrandnameResponse res = new LogBrandnameResponse(true, "0", (Integer)num.get(0) / 50 + 1, (Integer)num.get(1), (Integer)num.get(2), logBrandname);
        return res;
    }

    @Override
    public DoisoatBrandname doisoatBrandname(DoisoatBrandname model, String startTime, String endTime) {
        final Map<Integer, DoisoatBrandnameProvider> providers = model.getProviders();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, BasicDBObject> conditions = new HashMap<String, BasicDBObject>();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        FindIterable iterable = db.getCollection("brandname_transaction").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                int status = document.getInteger("status");
                if (status == 1 || status == -1) {
                    String mobile = DvtUtils.revertMobile84To(document.getString("receives"));
                    int id = PhoneUtils.getProviderByMobile((String)mobile, (boolean)true);
                    String message = document.getString("message");
                    int count = PhoneUtils.getNumMessage((String)message, (boolean)true);
                    DoisoatBrandnameProvider pv = (DoisoatBrandnameProvider)providers.get(id);
                    pv.setSms(pv.getSms() + (long)count);
                    providers.put(id, pv);
                }
            }
        });
        model.setProviders(providers);
        return model;
    }

}


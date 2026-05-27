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
 *  com.vinplay.vbee.common.models.OtpModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.PhoneUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.doisoat.entities.DoisoatVmg;
import com.vinplay.doisoat.entities.DoisoatVmgByProvider;
import com.vinplay.usercore.dao.OtpDao;
import com.vinplay.usercore.entities.LogSmsOtp;
import com.vinplay.usercore.response.LogSMSOtpResponse;
import com.vinplay.vbee.common.models.OtpModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.PhoneUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class OtpDaoImpl
implements OtpDao {
    @Override
    public boolean updateOtpSMS(String mobile, String otp, String messageMO) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        if (messageMO.equals("OZZ OTP")) {
            col = db.getCollection("sms_vin_otp");
        } else if (messageMO.equals("OZZ APP")) {
            col = db.getCollection("sms_vin_app");
        } else {
            if (!messageMO.equals("OZZ ODP")) {
                return false;
            }
            col = db.getCollection("sms_vin_odp");
        }
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("otp", otp);
        updateFields.append("otp_time", VinPlayUtils.getCurrentDateTime());
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("mobile", mobile);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$set", updateFields), options);
        return true;
    }

    @Override
    public OtpModel getOtpSMS(String mobile, String commandCode) throws ParseException {
        OtpModel model = null;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        if (commandCode.equals("OZZ OTP")) {
            col = db.getCollection("sms_vin_otp");
        } else if (commandCode.equals("OZZ APP")) {
            col = db.getCollection("sms_vin_app");
        } else {
            if (!commandCode.equals("OZZ ODP")) {
                return null;
            }
            col = db.getCollection("sms_vin_odp");
        }
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("mobile", mobile);
        Document doccument = (Document)col.find((Bson)new Document(conditions)).first();
        if (doccument != null) {
            model = new OtpModel(mobile, doccument.getString("otp"), VinPlayUtils.getDateTime((String)doccument.getString("otp_time")), commandCode);
        }
        return model;
    }

    @Override
    public LogSMSOtpResponse getLogSMSOtp(String mobile, String startTime, String endTime, int page, String requestId) {
        final ArrayList<LogSmsOtp> records = new ArrayList<LogSmsOtp>();
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
//HashMap<String, String> conditions = new HashMap<String, String>();
        if (!mobile.isEmpty()) {
            conditions.put("mobile", mobile);
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
        FindIterable iterable = db.getCollection("vmg_transaction").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogSmsOtp model = new LogSmsOtp();
                model.requestId = document.getString("request_id");
                model.mobile = document.getString("mobile");
                model.commandCode = document.getString("command_code");
                model.messageMO = document.getString("message_MO");
                model.responseMO = document.getString("response_MO");
                model.messageMT = document.getString("message_MT");
                model.responseMT = document.getString("response_MT");
                model.transTime = document.getString("trans_time");
                records.add(model);
            }
        });
        FindIterable iterable2 = db.getCollection("vmg_transaction").find((Bson)new Document(conditions));
        iterable2.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogSmsOtp model = new LogSmsOtp();
                model.responseMO = document.getString("response_MO");
                model.responseMT = document.getString("response_MT");
                int count = (Integer)num.get(0) + 1;
                num.set(0, count);
                if (model.responseMO.equals("1")) {
                    int numReceiveSuccess = (Integer)num.get(1) + 1;
                    num.set(1, numReceiveSuccess);
                }
                if (model.responseMT.equals("1")) {
                    int numSendSuccess = (Integer)num.get(2) + 1;
                    num.set(2, numSendSuccess);
                }
            }
        });
        LogSMSOtpResponse res = new LogSMSOtpResponse(true, "0", (Integer)num.get(0) / 50 + 1, (Integer)num.get(0), (Integer)num.get(1), (Integer)num.get(2), records);
        return res;
    }

    @Override
    public DoisoatVmg doisoatVMG(DoisoatVmg model, String startTime, String endTime) {
        final Map<Integer, DoisoatVmgByProvider> providers = model.getProviders();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, BasicDBObject> conditions = new HashMap<String, BasicDBObject>();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        FindIterable iterable = db.getCollection("vmg_transaction").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                String mobile = document.getString("mobile");
                int id = PhoneUtils.getProviderByMobile((String)mobile, (boolean)false);
                String messageMO = document.getString("message_MO");
                int mo = PhoneUtils.getNumMessage((String)messageMO, (boolean)false);
                String messageMT = document.getString("message_MT");
                int mt = PhoneUtils.getNumMessage((String)messageMT, (boolean)false);
                String responseMO = document.getString("response_MO");
                String responseMT = document.getString("response_MT");
                DoisoatVmgByProvider pv = (DoisoatVmgByProvider)providers.get(id);
                if (responseMO.equals("1")) {
                    pv.setMo(pv.getMo() + (long)mo);
                }
                if (responseMT.equals("1")) {
                    pv.setMt(pv.getMt() + (long)mt);
                }
                providers.put(id, pv);
            }
        });
        model.setProviders(providers);
        return model;
    }

}


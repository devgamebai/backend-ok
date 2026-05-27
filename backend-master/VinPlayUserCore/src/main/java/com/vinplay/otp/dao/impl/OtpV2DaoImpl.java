/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.UpdateOptions
 *  com.mongodb.client.model.Updates
 *  com.vinplay.vbee.common.models.OtpModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.otp.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.vinplay.otp.dao.OtpV2Dao;
import com.vinplay.vbee.common.models.OtpModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.Calendar;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class OtpV2DaoImpl
implements OtpV2Dao {
    private static final String Collection = "otp_logs";
    private static int timeLife = 5;

    @Override
    public int createOtp(String mobile, String code, String type, String sender, String nickname) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection(Collection);
        OtpModel otpModel = this.getOtp(mobile, null, null, null);
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.add(12, -1);
        if (otpModel != null && otpModel.getOtpTime() != null && aCalendar.getTime().getTime() < otpModel.getOtpTime().getTime()) {
            return 2;
        }
        Document updateFields = new Document();
        updateFields.append("otp", code);
        updateFields.append("mobile", mobile);
        updateFields.append("nickname", nickname);
        updateFields.append("type", type);
        updateFields.append("sender", sender);
        updateFields.append("count", 0);
        updateFields.append("finish", false);
        updateFields.put("time", new Date());
        try {
            if (otpModel != null) {
                this.finishOtp(mobile);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        col.insertOne(updateFields);
        return 1;
    }

    @Override
    public synchronized boolean updateOtpCount(String mobile, String code, String type) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection(Collection);
        OtpModel otp = this.getOtp(mobile, code, null, type);
        if (otp == null) {
            return false;
        }
        Document query = new Document().append("mobile", mobile).append("otp", code).append("type", type);
        Bson updates = Updates.combine((Bson[])new Bson[]{Updates.set((String)"count", (otp.getCount() + 1)), Updates.set((String)"lastUpdated", new Date())});
        UpdateOptions options = new UpdateOptions().upsert(true);
        col.updateOne((Bson)query, updates, options);
        return false;
    }

    @Override
    public boolean finishOtp(String mobile) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection(Collection);
        Document query = new Document().append("mobile", mobile).append("finish", false);
        Bson updates = Updates.combine((Bson[])new Bson[]{Updates.set((String)"finish", true), Updates.set((String)"lastUpdated", new Date())});
        UpdateOptions options = new UpdateOptions().upsert(true);
        col.updateOne((Bson)query, updates, options);
        return true;
    }

    @Override
    public OtpModel getOtpAfterTime(String mobile, Date startTime) throws Exception {
        return this.getOtp(mobile, null, startTime);
    }

    @Override
    public OtpModel getOtp(String mobile, String code) throws Exception {
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.add(12, -1 * timeLife);
        return this.getOtp(mobile, code, aCalendar.getTime());
    }

    @Override
    public OtpModel getOtp(String mobile, String code, Date startTime) throws Exception {
        return this.getOtp(mobile, code, startTime, null);
    }

    @Override
    public OtpModel getOtp(String mobile, String code, Date startTime, String type) throws Exception {
        Document document;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection(Collection);
        Document conditions = new Document("finish", false).append("mobile", mobile);
        if (startTime != null) {
            conditions.append("time", new Document("$gte", startTime));
        }
        if (type != null && !type.isEmpty()) {
            conditions.append("type", type);
        }
        if (code != null && !code.isEmpty()) {
            conditions.append("otp", code);
        }
        if ((document = (Document)col.find((Bson)conditions).first()) == null) {
            return null;
        }
        OtpModel otp = new OtpModel();
        otp.setId(document.getObjectId("_id").toHexString());
        otp.setOtp(document.getString("otp"));
        otp.setMobile(document.getString("mobile"));
        otp.setNickname(document.getString("nickname"));
        otp.setType(document.getString("type"));
        otp.setCommandCode(document.getString("code"));
        otp.setSender(document.getString("sender"));
        otp.setOtpTime(document.getDate("time"));
        return otp;
    }
}


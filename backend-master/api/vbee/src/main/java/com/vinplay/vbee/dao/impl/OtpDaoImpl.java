/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.BrandnameDLVRMessage
 *  com.vinplay.vbee.common.messages.BrandnameMessage
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.BrandnameDLVRMessage;
import com.vinplay.vbee.common.messages.BrandnameMessage;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.OtpDao;
import java.util.Date;
import org.bson.Document;

public class OtpDaoImpl
implements OtpDao {
    @Override
    public boolean saveLogOtp(OtpMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("vmg_transaction");
        Document doc = new Document();
        doc.append("request_id", message.getRequestId());
        doc.append("mobile", message.getMobile());
        doc.append("command_code", (Object)message.getCommandCode());
        doc.append("message_MO", (Object)message.getMessageMO());
        doc.append("response_MO", (Object)message.getResponseMO());
        doc.append("message_MT", (Object)message.getMessageMT());
        doc.append("response_MT", (Object)message.getResponseMT());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean saveLogBrandname(BrandnameMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("brandname_transaction");
        Document doc = new Document();
        doc.append("request_id", (Object)message.getRequestId());
        doc.append("brandname", (Object)message.getBrandname());
        doc.append("message", (Object)message.getMessage());
        doc.append("receives", (Object)message.getReceives());
        doc.append("code", (Object)message.getCode());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("status", -1);
        doc.append("count", -1);
        doc.append("status_desc", (Object)"");
        doc.append("sent_date", (Object)"");
        doc.append("update_time", (Object)"");
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean saveLogBrandnameDLVR(BrandnameDLVRMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("brandname_dlvr_transaction");
        Document doc = new Document();
        doc.append("request_id", (Object)message.getRequestId());
        doc.append("status", (Object)message.getSmsStatus());
        doc.append("count", (Object)message.getCount());
        doc.append("status_desc", (Object)message.getStatusDesc());
        doc.append("sent_date", (Object)message.getSentDate());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }
}


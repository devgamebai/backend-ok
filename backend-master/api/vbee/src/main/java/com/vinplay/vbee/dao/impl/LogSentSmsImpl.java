/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.sentsms.LogSentSmsMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.sentsms.LogSentSmsMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.LogSentSms;
import java.util.Date;
import org.bson.Document;

public class LogSentSmsImpl
implements LogSentSms {
    @Override
    public void saveLogSentSms(LogSentSmsMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_sent_sms");
        Document doc = new Document();
        doc.append("trans_id", (Object)message.getTransId());
        doc.append("sms_content", (Object)message.getSms());
        doc.append("phone_number", (Object)message.getTel());
        doc.append("partner_trans_id", (Object)message.getPartnerTransId());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("trans_time", (Object)message.getTransTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vtcpay.dao.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vtcpay.dao.VTCPayDao;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class VTCPayDaoImpl
implements VTCPayDao {
    @Override
    public boolean logVTCPayTopup(LogVTCPayTopupMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_topup_vtcpay");
        Document doc = new Document();
        doc.append("reference_id", message.getReferenceId());
        doc.append("partner_trans_id", message.getPartnerTransId());
        doc.append("user_id", message.getUserId());
        doc.append("user_name", message.getUserName());
        doc.append("nick_name", message.getNickname());
        doc.append("price", message.getPrice());
        doc.append("money_user", message.getMoneyUser());
        doc.append("status", message.getStatusRes());
        doc.append("response_code", message.getResponseCode());
        doc.append("description", message.getResponseDes());
        doc.append("time_request", message.getTimeRequest());
        doc.append("time_response", message.getTimeResponse());
        doc.append("create_time", new Date());
        col.insertOne(doc);
        return true;
    }

    @Override
    public LogVTCPayTopupMessage checkTrans(String transId) {
        LogVTCPayTopupMessage response = null;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("partner_trans_id", transId);
        Document dc = (Document)db.getCollection("log_topup_vtcpay").find((Bson)conditions).first();
        if (dc != null) {
            response = new LogVTCPayTopupMessage(dc.getString("reference_id"), dc.getString("partner_trans_id"), dc.getInteger("user_id").intValue(), dc.getString("user_name"), dc.getString("nick_name"), dc.getInteger("price").intValue(), dc.getLong("money_user").longValue(), dc.getInteger("status").intValue(), dc.getString("response_code"), dc.getString("description"), dc.getString("time_request"), dc.getString("time_response"));
        }
        return response;
    }
}


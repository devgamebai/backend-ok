/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.GiftCodeDao;
import java.sql.SQLException;
import org.bson.Document;
import org.bson.conversions.Bson;

public class GiftCodeDaoImpl
implements GiftCodeDao {
    @Override
    public void updateGiftCodeStore(String giftCode) throws SQLException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("count_use", (Object)1);
        db.getCollection("gift_code_store").updateOne((Bson)new Document("giftcode", (Object)giftCode), (Bson)new Document("$set", (Object)updateFields));
    }

    @Override
    public void lockGiftCode(String giftCode) throws SQLException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("block", (Object)1);
        db.getCollection("gift_code").updateOne((Bson)new Document("giftcode", (Object)giftCode), (Bson)new Document("$set", (Object)updateFields));
    }
}


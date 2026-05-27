/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  javax.servlet.http.HttpServletRequest
 *  org.bson.Document
 */
package com.vinplay.api.processors;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import javax.servlet.http.HttpServletRequest;
import org.bson.Document;

public class GetTokenTXLiveProccessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("token_live");
        Document doc = (Document)col.find().first();
        if (doc == null) {
            return "";
        }
        String token = doc.getString("token");
        return token;
    }
}


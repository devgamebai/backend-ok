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
 *  com.vinplay.vbee.common.response.UserNewMobileResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.UserNewMobileDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.UserNewMobileResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class UserNewMobileDAOImpl
implements UserNewMobileDAO {
    @Override
    public List<UserNewMobileResponse> searchUserNewMobile(String nickName, String mobile, String mobileold, int page) {
        final ArrayList<UserNewMobileResponse> result = new ArrayList<UserNewMobileResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        int num_start = (page - 1) * 50;
        int num_end = 50;
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (mobile != null && !mobile.equals("")) {
            conditions.put("mobile", mobile);
        }
        if (mobileold != null && !mobileold.equals("")) {
            conditions.put("mobile_old", mobileold);
        }
        iterable = db.getCollection("user_new_mobile").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                UserNewMobileResponse user = new UserNewMobileResponse();
                user.nickName = document.getString("nick_name");
                user.mobile = document.getString("mobile");
                user.mobileold = document.getString("mobile_old");
                user.updateTime = document.getString("update_time");
                result.add(user);
            }
        });
        return result;
    }

    @Override
    public long countSearchUserNewMobile(String nickName, String mobile, String mobileold) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        long totalrecord = 0L;
        Document conditions = new Document();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (mobile != null && !mobile.equals("")) {
            conditions.put("mobile", mobile);
        }
        if (mobileold != null && !mobileold.equals("")) {
            conditions.put("mobile_old", mobileold);
        }
        totalrecord = db.getCollection("user_new_mobile").count((Bson)new Document((Map)conditions));
        return totalrecord;
    }

}


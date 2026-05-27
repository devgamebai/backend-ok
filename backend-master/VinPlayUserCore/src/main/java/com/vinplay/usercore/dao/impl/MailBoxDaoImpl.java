/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.DeleteResult
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.MailBoxResponse
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
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.usercore.dao.MailBoxDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MailBoxResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MailBoxDaoImpl
implements MailBoxDao {
    private int flag = 0;

    @Override
    public boolean sendMailBoxFromByNickName(List<String> nickName, String title, String content) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("mail_box");
        for (String name : nickName) {
            Document doc = new Document();
            doc.append("mail_id", String.valueOf(System.currentTimeMillis()));
            doc.append("nick_name", name);
            doc.append("title", title);
            doc.append("content", content);
            doc.append("create_time", VinPlayUtils.getCurrentDateTime());
            doc.append("author", "H\u1ec7 th\u1ed1ng");
            doc.append("status", 0);
            col.insertOne(doc);
        }
        return true;
    }

    @Override
    public boolean sendMailBoxFromByNickNameAdmin(String nickName, String title, String content) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("mail_box");
        Document doc = new Document();
        doc.append("mail_id", String.valueOf(System.currentTimeMillis()));
        doc.append("nick_name", nickName);
        doc.append("title", title);
        doc.append("content", content);
        doc.append("create_time", VinPlayUtils.getCurrentDateTime());
        doc.append("author", "H\u1ec7 th\u1ed1ng");
        doc.append("status", 0);
        col.insertOne(doc);
        return true;
    }

    @Override
    public List<MailBoxResponse> listMailBox(String nickName, int page) {
        final ArrayList<MailBoxResponse> results = new ArrayList<MailBoxResponse>();
        int num_start = (page - 1) * 5;
        int num_end = 5;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        BasicDBObject objsort = new BasicDBObject();
        conditions.put("nick_name", nickName);
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("mail_box").find((Bson)new Document(conditions)).skip(num_start).limit(5).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                MailBoxResponse mail = new MailBoxResponse();
                mail.sysMail = document.getString("nick_name").equals("*") ? 1 : 0;
                mail.title = document.getString("title");
                mail.createTime = document.getString("create_time");
                mail.author = document.getString("author");
                mail.content = document.getString("content");
                mail.status = document.getInteger("status");
                mail.mail_id = document.getString("mail_id");
                if (document.getString("mail_gift_code") != null && !document.getString("mail_gift_code").equals("")) {
                    mail.giftCode = document.getString("mail_gift_code");
                }
                results.add(mail);
            }
        });
        return results;
    }

    @Override
    public int updateStatusMailBox(String mailId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection colmail = db.getCollection("mail_box");
        colmail.updateOne((Bson)new Document("mail_id", mailId), (Bson)new Document("$set", new Document("status", 1)));
        return 0;
    }

    @Override
    public int deleteMailBox(String mailId) {
        final MongoDatabase db = MongoDBConnectionFactory.getDB();
        final BasicDBObject obj = new BasicDBObject();
        obj.put("mail_id", mailId);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("mail_id", mailId);
        FindIterable iterable = db.getCollection("mail_box").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                if (!document.getString("nick_name").equals("*")) {
                    db.getCollection("mail_box").deleteOne((Bson)obj);
                    MailBoxDaoImpl.this.flag = 0;
                } else {
                    MailBoxDaoImpl.this.flag = 1;
                }
            }
        });
        return this.flag;
    }

    @Override
    public int countMailBox(String nickName) {
        int record = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("nick_name", nickName);
        record = (int)db.getCollection("mail_box").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public boolean sendmailGiftCode(String nickName, String giftcode, String title, String type, String price) throws SQLException {
        String content = "";
        if (type.equals("1")) {
            content = content + "Ch\u00e0o b\u1ea1n " + nickName + "\n";
            content = content + "Ch\u00fac m\u1eebng b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c t\u1eb7ng Giftcode: " + giftcode + "\n";
            content = content + "\u0110\u1ec3 s\u1eed d\u1ee5ng \u0111\u01b0\u1ee3c Gift code b\u1ea1n h\u00e3y k\u00edch ho\u1ea1t s\u1ed1 \u0111i\u1ec7n tho\u1ea1i b\u1ea3o m\u1eadt\n";
            content = content + "L\u01b0u \u00fd: M\u1ed7i t\u00e0i kho\u1ea3n v\u00e0 s\u1ed1 \u0111i\u1ec7n tho\u1ea1i ch\u1ec9 \u0111\u01b0\u1ee3c nh\u1eadn Giftcode 1 l\u1ea7n \n";
        }
        if (type.equals("2")) {
            content = content + "Ch\u00e0o b\u1ea1n " + nickName + "\n";
            content = content + "Ch\u00fac m\u1eebng b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c t\u1eb7ng Giftcode tri \u00e2n tr\u1ecb gi\u00e1 " + price + " Vin: " + giftcode + "\n";
            content = content + "R\u1ea5t c\u00e1m \u01a1n b\u1ea1n \u0111\u00e3 quan t\u00e2m v\u00e0 \u1ee7ng h\u1ed9 Viplay trong th\u1eddi gian qua. \n";
            content = content + "L\u01b0u \u00fd: M\u1ed7i t\u00e0i kho\u1ea3n v\u00e0 s\u1ed1 \u0111i\u1ec7n tho\u1ea1i ch\u1ec9 \u0111\u01b0\u1ee3c nh\u1eadn Giftcode 1 l\u1ea7n \n";
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("mail_box");
        Document doc = new Document();
        doc.append("mail_id", String.valueOf(System.currentTimeMillis()));
        doc.append("nick_name", nickName);
        doc.append("mail_gift_code", giftcode);
        doc.append("title", title);
        doc.append("content", content);
        doc.append("create_time", VinPlayUtils.getCurrentDateTime());
        doc.append("author", "H\u1ec7 th\u1ed1ng");
        doc.append("status", 0);
        col.insertOne(doc);
        return true;
    }

    @Override
    public int countMailBoxInActive(String nickName) throws SQLException {
        int record = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("nick_name", nickName);
        conditions.put("status", 0);
        record = (int)db.getCollection("mail_box").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public boolean sendMailGiftCode(String nickName, String giftcode, String title, String content) throws SQLException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("mail_box");
        Document doc = new Document();
        doc.append("mail_id", String.valueOf(System.currentTimeMillis()));
        doc.append("nick_name", nickName);
        doc.append("mail_gift_code", giftcode);
        doc.append("title", title);
        doc.append("content", content);
        doc.append("create_time", VinPlayUtils.getCurrentDateTime());
        doc.append("author", "H\u1ec7 th\u1ed1ng");
        doc.append("status", 0);
        col.insertOne(doc);
        return true;
    }

    @Override
    public int deleteMailBoxAdmin(String mailId) {
        final MongoDatabase db = MongoDBConnectionFactory.getDB();
        final BasicDBObject obj = new BasicDBObject();
        obj.put("mail_id", mailId);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("mail_id", mailId);
        FindIterable iterable = db.getCollection("mail_box").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                db.getCollection("mail_box").deleteOne((Bson)obj);
                MailBoxDaoImpl.this.flag = 0;
            }
        });
        return this.flag;
    }

    @Override
    public int deleteMutilMailBox(String nickName, String title) {
        final MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        final BasicDBObject obj = new BasicDBObject();
        if (nickName != null && !nickName.equals("") || title != null && !title.equals("")) {
            BasicDBObject query1 = new BasicDBObject("nick_name", nickName);
            BasicDBObject query2 = new BasicDBObject("title", title);
            ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
            myList.add(query1);
            myList.add(query2);
            conditions.put("$or", myList);
        }
        if (nickName != null && !nickName.equals("")) {
            obj.put("nick_name", nickName);
        }
        if (title != null && !title.equals("")) {
            obj.put("title", title);
        }
        FindIterable iterable = db.getCollection("mail_box").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                db.getCollection("mail_box").deleteOne((Bson)obj);
                MailBoxDaoImpl.this.flag = 0;
            }
        });
        return this.flag;
    }

    @Override
    public boolean sendMailCardMobile(String nickName, String serial, String pin, String title, String content) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("mail_box");
        Document doc = new Document();
        doc.append("mail_id", String.valueOf(System.currentTimeMillis()));
        doc.append("nick_name", nickName);
        doc.append("serial", serial);
        doc.append("pin", pin);
        doc.append("title", title);
        doc.append("content", content);
        doc.append("create_time", VinPlayUtils.getCurrentDateTime());
        doc.append("author", "H\u1ec7 th\u1ed1ng");
        doc.append("status", 0);
        col.insertOne(doc);
        return true;
    }

}


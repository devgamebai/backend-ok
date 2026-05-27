/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package game.third.usecase.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import game.third.usecase.dao.LiveCasinoDao;
import game.third.usecase.gsc.response.LiveCasinoUserResponse;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LiveCasinoDaoImpl
implements LiveCasinoDao {
    @Override
    public boolean insertUserCasino(String user, String pass) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("live_casino_user");
        Document doc = new Document();
        doc.append("user_name", (Object)user);
        doc.append("pass_word", (Object)pass);
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public LiveCasinoUserResponse getUserCasino(String userName) {
        LiveCasinoUserResponse result = null;
        Document conditions = new Document();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        conditions.put("user_name", (Object)userName);
        Document dc = (Document)db.getCollection("live_casino_user").find((Bson)conditions).first();
        if (dc != null) {
            result = new LiveCasinoUserResponse(dc.getString((Object)"user_name"), dc.getString((Object)"pass_word"));
        }
        return result;
    }
}


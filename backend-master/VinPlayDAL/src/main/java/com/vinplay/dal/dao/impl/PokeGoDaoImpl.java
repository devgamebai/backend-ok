/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.PokeGoDAO;
import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class PokeGoDaoImpl
implements PokeGoDAO {
    @Override
    public List<TopPokeGo> getTopPokeGo(int moneyType, int pageNumber) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<TopPokeGo> results = new ArrayList<TopPokeGo>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("$or", Arrays.asList(new Document[]{new Document("result", 3), new Document("result", 4)}));
        conditions.put("money_type", moneyType);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_poke_go").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopPokeGo entry = new TopPokeGo();
                entry.un = document.getString("user_name");
                entry.bv = document.getLong("bet_value");
                entry.pz = document.getLong("prize");
                entry.ts = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public int countLSGD(String username, int moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        long totalRows = db.getCollection("log_poke_go").count((Bson)conditions);
        return (int)totalRows;
    }

    @Override
    public List<LSGDPokeGo> getLSGD(String username, int moneyType, int pageNumber) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<LSGDPokeGo> results = new ArrayList<LSGDPokeGo>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        iterable = db.getCollection("log_poke_go").find((Bson)conditions).sort((Bson)objsort).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LSGDPokeGo entry = new LSGDPokeGo();
                entry.rf = document.getLong("reference_id");
                entry.un = document.getString("user_name");
                entry.bv = document.getLong("bet_value");
                entry.pz = document.getLong("prize");
                entry.lb = document.getString("lines_betting");
                entry.lw = document.getString("lines_win");
                entry.ps = document.getString("prizes_on_line");
                entry.ts = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public long getLastRefenceId() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("log_poke_go").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        Document document = iterable != null ? (Document)iterable.first() : null;
        long referenceId = document == null ? 0L : document.getLong("reference_id");
        return referenceId;
    }

    @Override
    public List<TopPokeGo> getTop(int moneyType, int number) {
        final ArrayList<TopPokeGo> results = new ArrayList<TopPokeGo>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("$or", Arrays.asList(new Document[]{new Document("result", 3), new Document("result", 4)}));
        conditions.put("money_type", moneyType);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_poke_go").find((Bson)conditions).sort((Bson)sortCondtions).limit(number);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopPokeGo entry = new TopPokeGo();
                entry.un = document.getString("user_name");
                entry.bv = document.getLong("bet_value");
                entry.pz = document.getLong("prize");
                entry.ts = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.api.processors.minigame;

import com.mongodb.client.MongoDatabase;
import com.vinplay.api.processors.minigame.response.JackpotTaiXiuResponse;
import com.vinplay.api.processors.minigame.response.ResultTaiXiuJackpotResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ListJackpotTaiXiuProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultTaiXiuJackpotResponse response = new ResultTaiXiuJackpotResponse(false, "1001");
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        List documents = (List)db.getCollection("user_jackpot_tai_xiu").find().sort((Bson)new Document("time", -1)).limit(10).into(new ArrayList());
        ArrayList<JackpotTaiXiuResponse> taiXiuItemResponses = new ArrayList<JackpotTaiXiuResponse>();
        for (Object _doc : documents) {
            Document document = (Document) _doc;
            taiXiuItemResponses.add(new JackpotTaiXiuResponse(document.getLong("referentId"), document.getInteger("result"), document.getString("time"), document.getString("countBet"), document.getString("moneyJackpotAll"), document.getString("data")));
        }
        Collections.sort(taiXiuItemResponses, new Comparator<JackpotTaiXiuResponse>(){

            @Override
            public int compare(JackpotTaiXiuResponse o1, JackpotTaiXiuResponse o2) {
                return Long.compare(o1.referenceId, o2.referenceId);
            }
        });
        try {
            response.setTotal(taiXiuItemResponses.size());
            response.setTotalRecord(taiXiuItemResponses.size());
            response.setTransactions(taiXiuItemResponses);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return response.toJson();
    }
}


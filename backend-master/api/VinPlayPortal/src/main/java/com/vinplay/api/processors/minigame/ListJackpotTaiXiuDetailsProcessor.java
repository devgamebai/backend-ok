/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.Sorts
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
import com.mongodb.client.model.Sorts;
import com.vinplay.api.processors.minigame.response.JackpotTaiXiuDetailsResponse;
import com.vinplay.api.processors.minigame.response.ResultTaiXiuJackpotDetailsResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ListJackpotTaiXiuDetailsProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referentId = request.getParameter("referentid");
        ResultTaiXiuJackpotDetailsResponse response = new ResultTaiXiuJackpotDetailsResponse(false, "1001");
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("referentId", Long.parseLong(referentId));
        List documents = (List)db.getCollection("user_jackpot_tai_xiu_details").find((Bson)conditions).limit(20).sort(Sorts.descending((String[])new String[]{"money"})).into(new ArrayList());
        ArrayList<JackpotTaiXiuDetailsResponse> taiXiuItemResponses = new ArrayList<JackpotTaiXiuDetailsResponse>();
        for (Object _doc : documents) {
            Document document = (Document) _doc;
            taiXiuItemResponses.add(new JackpotTaiXiuDetailsResponse(document.getLong("referentId"), document.getInteger("result"), document.getString("time"), document.getString("countBet"), document.getString("moneyJackpotAll"), document.getString("nickName"), document.getLong("money")));
        }
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


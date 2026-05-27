/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogTaiXiuServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultTaiXiuDetailResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.ResultXocDiaDetailNowResponse;
import com.vinplay.vbee.common.response.XocDiaItemResponse;
import com.vinplay.usercore.service.CacheService;
import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import org.apache.log4j.Logger;
import org.bson.Document;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class ListXocDiaTransactionDetailNowProcessor
        implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultXocDiaDetailNowResponse response = new ResultXocDiaDetailNowResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest) param.get();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        List<Document> documents = db.getCollection("user_bet_xoc_dia").find().into(new ArrayList<>());
        List<XocDiaItemResponse> bauCuaItemResponses = new ArrayList<>();
        for (Document document : documents) {
            bauCuaItemResponses.add(new XocDiaItemResponse(document.getString("nick_name")
                    , document.getLong("potId0").longValue()
                    , document.getLong("potId1").longValue()
                    , document.getLong("potId2").longValue()
                    , document.getLong("potId3").longValue()
                    , document.getLong("potId4").longValue()
                    , document.getLong("potId5").longValue()));
        }

        try {
            response.setTotal(bauCuaItemResponses.size());
            response.setTotalRecord(bauCuaItemResponses.size());
            response.setTransactions(bauCuaItemResponses);
            
            // Lấy countTime và isBetting từ cache
            CacheService cacheService = new CacheServiceImpl();
            try {
                Object countTimeObj = cacheService.getObject("xocdia_countTime");
                if (countTimeObj != null) {
                    int countTime = countTimeObj instanceof Integer ? (Integer)countTimeObj : Integer.parseInt(countTimeObj.toString());
                    response.setTimeEnd(countTime);
                } else {
                    response.setTimeEnd(0);
                }
            } catch (KeyNotFoundException e) {
                response.setTimeEnd(0);
            } catch (Exception e) {
                logger.debug("Error getting countTime from cache: " + e.getMessage());
                response.setTimeEnd(0);
            }
            
            try {
                Object isBettingObj = cacheService.getObject("xocdia_isBetting");
                if (isBettingObj != null) {
                    boolean isBetting = isBettingObj instanceof Boolean ? (Boolean)isBettingObj : Boolean.parseBoolean(isBettingObj.toString());
                    response.setBetting(isBetting);
                } else {
                    response.setBetting(false);
                }
            } catch (KeyNotFoundException e) {
                response.setBetting(false);
            } catch (Exception e) {
                logger.debug("Error getting isBetting from cache: " + e.getMessage());
                response.setBetting(false);
            }
            
            response.setSuccess(true);
            response.setErrorCode("0");
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object) e);
        }
        return response.toJson();
    }
}

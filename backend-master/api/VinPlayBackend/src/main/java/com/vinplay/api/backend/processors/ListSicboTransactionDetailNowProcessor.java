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
import com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.ResultSicboDetailNowResponse;
import com.vinplay.usercore.service.CacheService;
import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class ListSicboTransactionDetailNowProcessor
        implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static TaiXiuSicboServiceImpl service = new TaiXiuSicboServiceImpl();
    public String execute(Param<HttpServletRequest> param) {
        ResultSicboDetailNowResponse response = new ResultSicboDetailNowResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest) param.get();
        MongoDatabase db = MongoDBConnectionFactory.getDB();

        try {
            response.setTransactions(service.getCurrentSessionInfo());

            // Lấy countTime và isBetting từ cache
            CacheService cacheService = new CacheServiceImpl();
            try {
                Object countTimeObj = cacheService.getObject("sicbo_countTime");
                if (countTimeObj != null) {
                    int countTime = countTimeObj instanceof Integer ? (Integer)countTimeObj : 
                                   countTimeObj instanceof Short ? ((Short)countTimeObj).intValue() : 
                                   Integer.parseInt(countTimeObj.toString());
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
                Object isBettingObj = cacheService.getObject("sicbo_isBetting");
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

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
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.LogTaiXiuServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.ResultTaiXiuDetailNowResponse;
import com.vinplay.vbee.common.response.ResultTaiXiuDetailResponse;
import com.vinplay.vbee.common.response.TaiXiuItemResponse;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListTaiXiuTransactionDetailNowProcessor
        implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultTaiXiuDetailNowResponse response = new ResultTaiXiuDetailNowResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest) param.get();
        String md5 = request.getParameter("md5");
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        List<Document> documents = null;
        if (!TextUtils.isEmpty(md5) && md5.equals("1")) {
            documents = db.getCollection("user_bet_tai_xiu_md5").find().into(new ArrayList<>());
        } else {
            documents = db.getCollection("user_bet_tai_xiu").find().into(new ArrayList<>());
        }
        List<TaiXiuItemResponse> taiXiuItemResponses = new ArrayList<>();
        for (Document document : documents) {
            taiXiuItemResponses.add(new TaiXiuItemResponse(document.getLong("referentId").longValue()
                    , document.getString("nick_name")
                    , document.getLong("betValue").longValue()
                    , document.getInteger("betSide").intValue()
                    , document.getInteger("inputTime").intValue()
                    , document.getLong("balance").longValue()));
        }

        try {
            response.setTotal(taiXiuItemResponses.size());
            response.setTotalRecord(taiXiuItemResponses.size());
            response.setTransactions(taiXiuItemResponses);
            response.setSuccess(true);
            response.setErrorCode("0");
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object) e);
        }
        return response.toJson();
    }
}


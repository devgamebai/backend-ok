/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LuckyVipHistoryServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultLuckVipHistoryResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LuckyVipHistoryServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultLuckVipHistoryResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LuckyVipHistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultLuckVipHistoryResponse response = new ResultLuckVipHistoryResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        LuckyVipHistoryServiceImpl service = new LuckyVipHistoryServiceImpl();
        try {
            List trans = service.searchLuckyVipHistory(nickName, timeStart, timeEnd, page);
            long totalRecord = 1000L;
            long totalPages = 0L;
            totalPages = 20L;
            response.setTotal(totalPages);
            response.setTotalRecord(1000L);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


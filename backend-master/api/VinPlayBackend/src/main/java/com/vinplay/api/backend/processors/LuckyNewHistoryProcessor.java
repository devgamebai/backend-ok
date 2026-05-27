/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LuckyNewHistoryServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultLuckNewHistoryResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LuckyNewHistoryServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultLuckNewHistoryResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LuckyNewHistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultLuckNewHistoryResponse response = new ResultLuckNewHistoryResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        LuckyNewHistoryServiceImpl service = new LuckyNewHistoryServiceImpl();
        try {
            List trans = service.searchLuckyNewHistory(nickName, timeStart, timeEnd, page);
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


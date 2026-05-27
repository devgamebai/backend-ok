/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogGameResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogGameResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class SearchLogGameByNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LogGameResponse response = new LogGameResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String sessionId = request.getParameter("sid");
        String nickName = request.getParameter("nn");
        String gameName = request.getParameter("gn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        int page = Integer.parseInt(request.getParameter("p"));
        LogGameServiceImpl service = new LogGameServiceImpl();
        List trans = service.searchLogGameByNickName(sessionId, nickName, gameName, timeStart, timeEnd, moneyType, page);
        int totalRecord = service.countSearchLogGameByNickName(sessionId, nickName, gameName, timeStart, timeEnd, moneyType);
        long totalPages = 0L;
        totalPages = totalRecord % 50 == 0 ? (long)(totalRecord / 50) : (long)(totalRecord / 50 + 1);
        response.setTotal(totalPages);
        response.setTotalRecord((long)totalRecord);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


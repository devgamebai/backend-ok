/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.VipPointEventServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultLogVipPointEventResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.VipPointEventServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultLogVipPointEventResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LogVipPointEventProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultLogVipPointEventResponse response = new ResultLogVipPointEventResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String value = request.getParameter("va");
        String type = request.getParameter("type");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String bot = request.getParameter("bt");
        int page = Integer.parseInt(request.getParameter("p"));
        VipPointEventServiceImpl service = new VipPointEventServiceImpl();
        List trans = service.listLogVipPointEvent(nickName, value, type, timeStart, timeEnd, page, bot);
        long totalRecord = service.countLogVipPointEvent(nickName, value, type, timeStart, timeEnd, bot);
        long totalMoney = service.totalVipPointEvent(nickName, value, type, timeStart, timeEnd, bot);
        long totalPages = 0L;
        totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
        response.setTotalMoney(totalMoney);
        response.setTotal(totalPages);
        response.setTotalRecord(totalRecord);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


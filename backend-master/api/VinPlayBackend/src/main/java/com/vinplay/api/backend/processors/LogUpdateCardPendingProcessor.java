/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogUpdateCardPendingServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultLogUpdateCardPendingResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogUpdateCardPendingServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultLogUpdateCardPendingResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LogUpdateCardPendingProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultLogUpdateCardPendingResponse response = new ResultLogUpdateCardPendingResponse(false, "10001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String provider = request.getParameter("pv");
        String serial = request.getParameter("sr");
        String pin = request.getParameter("pn");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String referenceId = request.getParameter("rid");
        String actor = request.getParameter("act");
        LogUpdateCardPendingServiceImpl service = new LogUpdateCardPendingServiceImpl();
        long totalRecord = service.countTotalRecordLogUpdateCardPending(nickName, provider, serial, pin, code, timeStart, timeEnd, referenceId, actor);
        List trans = service.searchLogUpdateCardPending(nickName, provider, serial, pin, code, timeStart, timeEnd, page, referenceId, actor);
        List moneyReponse = service.moneyTotalUpdateCardPengding(nickName, provider, serial, pin, code, timeStart, timeEnd, referenceId, actor);
        long totalPage = 0L;
        totalPage = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
        response.setTotalPage(totalPage);
        response.setTotalRecord(totalRecord);
        response.setTrans(trans);
        response.setMoneyReponse(moneyReponse);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


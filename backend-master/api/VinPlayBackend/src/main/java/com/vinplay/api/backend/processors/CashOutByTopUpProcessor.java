/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultCashOutByTopUpResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultCashOutByTopUpResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class CashOutByTopUpProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultCashOutByTopUpResponse response = new ResultCashOutByTopUpResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String target = request.getParameter("tg");
        String status = request.getParameter("st");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String transid = request.getParameter("tid");
        String partner = request.getParameter("pa");
        String type = request.getParameter("type");
        long totalMoney = 0L;
        CashOutByTopUpServiceImpl service = new CashOutByTopUpServiceImpl();
        List trans = service.searchCashOutByTopUp(nickName, target, status, code, timeStart, timeEnd, page, transid, partner, type);
        int totalRecord = service.countSearchCashOutByTopUp(nickName, target, status, code, timeStart, timeEnd, transid, partner, type);
        totalMoney = service.moneyTotal(nickName, target, status, code, timeStart, timeEnd, transid, partner, type);
        long totalPages = 0L;
        totalPages = totalRecord % 50 == 0 ? (long)(totalRecord / 50) : (long)(totalRecord / 50 + 1);
        response.setTotalMoney(totalMoney);
        response.setTotal(totalPages);
        response.setTotalRecord((long)totalRecord);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


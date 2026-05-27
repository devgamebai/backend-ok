/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultCashOutByCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.CashOutByCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultCashOutByCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class CashOutByCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultCashOutByCardResponse response = new ResultCashOutByCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String provider = request.getParameter("pv");
        String softpin = request.getParameter("sp");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String transid = request.getParameter("tid");
        String partner = request.getParameter("pa");
        long totalMoney = 0L;
        CashOutByCardServiceImpl service = new CashOutByCardServiceImpl();
        List trans = service.searchCashOutByCard(nickName, provider, softpin, code, timeStart, timeEnd, page, transid, partner);
        int totalRecord = service.countSearchCashOutByCard(nickName, provider, softpin, code, timeStart, timeEnd, transid, partner);
        totalMoney = service.moneyTotal(nickName, provider, softpin, code, timeStart, timeEnd, transid, partner);
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


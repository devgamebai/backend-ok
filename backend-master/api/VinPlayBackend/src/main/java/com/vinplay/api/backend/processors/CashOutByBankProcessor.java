/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByBankServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultCashOutByBankResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.CashOutByBankServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultCashOutByBankResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class CashOutByBankProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultCashOutByBankResponse response = new ResultCashOutByBankResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String bank = request.getParameter("b");
        String status = request.getParameter("st");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String transid = request.getParameter("tid");
        CashOutByBankServiceImpl service = new CashOutByBankServiceImpl();
        List trans = service.searchCashOutByBank(nickName, bank, status, code, timeStart, timeEnd, page, transid);
        int totalRecord = service.countSearchCashOutByBank(nickName, bank, status, code, timeStart, timeEnd, transid);
        long totalMoney = service.moneyTotal(nickName, bank, status, code, timeStart, timeEnd, transid);
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


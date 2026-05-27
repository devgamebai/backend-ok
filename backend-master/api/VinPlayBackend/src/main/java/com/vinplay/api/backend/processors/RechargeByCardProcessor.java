/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.RechargeByCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultRechargeByCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.RechargeByCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultRechargeByCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RechargeByCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultRechargeByCardResponse response = new ResultRechargeByCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String provider = request.getParameter("pv");
        String serial = request.getParameter("sr");
        String pin = request.getParameter("pn");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String transid = request.getParameter("tid");
        RechargeByCardServiceImpl service = new RechargeByCardServiceImpl();
        List trans = service.searchRechargeByCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transid);
        int totalRecord = 1000;
        List moneyTotalRechargeByCard = service.moneyTotalRechargeByCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transid);
        long totalPages = 100L;
        totalPages = 20L;
        response.setMoneyReponse(moneyTotalRechargeByCard);
        response.setTotal(totalPages);
        response.setTotalRecord(1000L);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


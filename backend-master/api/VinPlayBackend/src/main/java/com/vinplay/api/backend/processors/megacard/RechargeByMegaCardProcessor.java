/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.RechargeByMegaCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.megacard.ResultMegaCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.megacard;

import com.vinplay.dal.service.impl.RechargeByMegaCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.megacard.ResultMegaCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RechargeByMegaCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultMegaCardResponse response = new ResultMegaCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String provider = request.getParameter("pv");
        String serial = request.getParameter("sr");
        String pin = request.getParameter("pn");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        String transId = request.getParameter("tid");
        RechargeByMegaCardServiceImpl service = new RechargeByMegaCardServiceImpl();
        long totalMoney = service.moneyTotal(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
        long totalRecord = service.countSearchRechargeByMegaCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
        List transactions = service.searchRechargeByMegaCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transId);
        response.setTotalMoney(totalMoney);
        response.setTotalRecord(totalRecord);
        response.setTransactions(transactions);
        response.setErrorCode("0");
        response.setSuccess(true);
        return response.toJson();
    }
}


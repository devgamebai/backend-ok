/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.RechargeByVinCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultVinCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.RechargeByVinCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultVinCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RechargeByVinCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultVinCardResponse response = new ResultVinCardResponse(false, "1001");
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
        RechargeByVinCardServiceImpl service = new RechargeByVinCardServiceImpl();
        long totalMoney = 0L;
        List trans = service.searchRechargeByVinCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transid);
        long totalRecord = service.countSearchRechargeByVinCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transid);
        totalMoney = service.moneyTotal(nickName, provider, serial, pin, code, timeStart, timeEnd, transid);
        long totalPages = 100L;
        response.setTotalMoney(totalMoney);
        response.setTotal(100L);
        response.setTotalRecord(totalRecord);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


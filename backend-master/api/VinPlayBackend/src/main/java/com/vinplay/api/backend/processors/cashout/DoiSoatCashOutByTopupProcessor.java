/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.cashout.DoiSoatCashOutByTopupResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.cashout;

import com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.cashout.DoiSoatCashOutByTopupResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatCashOutByTopupProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        DoiSoatCashOutByTopupResponse response = new DoiSoatCashOutByTopupResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String partner = request.getParameter("pa");
        String type = request.getParameter("type");
        if (timeStart == null || timeEnd == null || partner == null || timeStart.isEmpty() || timeEnd.isEmpty() || partner.isEmpty()) {
            return "MISSING INPUT PARAMETER";
        }
        CashOutByTopUpServiceImpl service = new CashOutByTopUpServiceImpl();
        List moneyResponse = service.moneyTotalCashOutByTopup(timeStart, timeEnd, partner, String.valueOf(0), type);
        response.setMoneyResponse(moneyResponse);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


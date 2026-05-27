/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.cashout.DoiSoatCashOutByCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.cashout;

import com.vinplay.dal.service.impl.CashOutByCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.cashout.DoiSoatCashOutByCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatCashOutByCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        DoiSoatCashOutByCardResponse response = new DoiSoatCashOutByCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String partner = request.getParameter("pa");
        if (timeStart == null || timeEnd == null || partner == null || timeStart.isEmpty() || timeEnd.isEmpty() || partner.isEmpty()) {
            return "MISSING INPUT PARAMETER";
        }
        CashOutByCardServiceImpl service = new CashOutByCardServiceImpl();
        List moneyResponse = service.moneyTotalCashOutByCard(timeStart, timeEnd, partner, String.valueOf(0));
        response.setMoneyReponse(moneyResponse);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


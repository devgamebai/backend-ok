/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.RechargeByMegaCardServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.megacard.DoiSoatRechargeByMegaCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.megacard;

import com.vinplay.dal.service.impl.RechargeByMegaCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.megacard.DoiSoatRechargeByMegaCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatRechargeByMegaCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        DoiSoatRechargeByMegaCardResponse response = new DoiSoatRechargeByMegaCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        if (timeStart == null || timeEnd == null || timeStart.isEmpty() || timeEnd.isEmpty()) {
            return "MISSING INPUT PARAMETER";
        }
        RechargeByMegaCardServiceImpl service = new RechargeByMegaCardServiceImpl();
        List moneyResponse = service.moneyTotalRechargeByMegaCard(timeStart, timeEnd, String.valueOf(0));
        response.setMoneyReponse(moneyResponse);
        response.setErrorCode("0");
        response.setSuccess(true);
        return response.toJson();
    }
}


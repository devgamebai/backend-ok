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
package com.vinplay.api.backend.processors.doisoat;

import com.vinplay.dal.service.impl.RechargeByCardServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultRechargeByCardResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatRechargeByCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultRechargeByCardResponse response = new ResultRechargeByCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        RechargeByCardServiceImpl service = new RechargeByCardServiceImpl();
        List moneyTotalRechargeByCard = service.doiSoatRechargeByCard(0, timeStart, timeEnd);
        response.setSuccess(true);
        response.setErrorCode("0");
        response.setMoneyReponse(moneyTotalRechargeByCard);
        return response.toJson();
    }
}


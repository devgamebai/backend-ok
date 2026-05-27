/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.ListMoneyTranferServiceImpl
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.FreezeMoneyResponse
 *  com.vinplay.vbee.common.response.TranferMoneyResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.ListMoneyTranferServiceImpl;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.FreezeMoneyResponse;
import com.vinplay.vbee.common.response.TranferMoneyResponse;
import javax.servlet.http.HttpServletRequest;

public class FreezeMoneyTranferAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        FreezeMoneyResponse response = new FreezeMoneyResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String transNo = request.getParameter("tno");
        if (transNo == null || "".equals(transNo)) {
            return "MISSING INPUT PARAMETER";
        }
        ListMoneyTranferServiceImpl service1 = new ListMoneyTranferServiceImpl();
        TranferMoneyResponse trans = service1.getMoneyTranferByTransNo(transNo);
        if (trans != null && trans.getIs_freeze_money() == 1) {
            MoneyInGameServiceImpl service2 = new MoneyInGameServiceImpl();
            response = service2.freezeMoneyTranferAgent(trans.getAgent_level1(), trans.getNick_receive(), "FreezeMoneyTranferAgent", trans.getMoney_receive(), "vin", transNo);
            return response.toJson();
        }
        response.setErrorCode("1043");
        return response.toJson();
    }
}


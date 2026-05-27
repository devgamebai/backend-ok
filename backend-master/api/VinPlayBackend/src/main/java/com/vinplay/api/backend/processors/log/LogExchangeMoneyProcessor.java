/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.payment.service.impl.PaymentServiceImpl
 *  com.vinplay.usercore.response.LogExchangeMoneyResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.payment.service.impl.PaymentServiceImpl;
import com.vinplay.usercore.response.LogExchangeMoneyResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class LogExchangeMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String nickname = request.getParameter("nn");
            String merchantId = request.getParameter("mid");
            String transId = request.getParameter("tid");
            String transNo = request.getParameter("tno");
            String type = request.getParameter("type");
            int code = Integer.parseInt(request.getParameter("co"));
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            int page = Integer.parseInt(request.getParameter("p"));
            if (merchantId != null && !merchantId.isEmpty()) {
                PaymentServiceImpl ser = new PaymentServiceImpl();
                LogExchangeMoneyResponse res = ser.getLogExchangeMoney(nickname, merchantId, transId, transNo, type, code, startTime, endTime, page);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString((Object)res);
            }
            return "";
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}


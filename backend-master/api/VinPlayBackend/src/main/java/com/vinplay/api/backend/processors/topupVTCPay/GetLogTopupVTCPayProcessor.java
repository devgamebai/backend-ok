/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.TopupVTCPayDaoImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPayResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.topupVTCPay;

import com.vinplay.dal.dao.impl.TopupVTCPayDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPayResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetLogTopupVTCPayProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LogTopupVTCPayResponse response = new LogTopupVTCPayResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String price = request.getParameter("pri");
        String nickname = request.getParameter("nn");
        String transId = request.getParameter("tid");
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        String page = request.getParameter("p");
        if (page == null || page.isEmpty()) {
            page = "1";
        }
        TopupVTCPayDaoImpl dao = new TopupVTCPayDaoImpl();
        List trans = dao.getLogTopupVtcPay(nickname, price, transId, startTime, endTime, page);
        response.setSuccess(true);
        response.setErrorCode("0");
        response.setTrans(trans);
        return response.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.TopupVTCPayDaoImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.topupVTCPay.DoiSoatTopupVTCPayResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.topupVTCPay;

import com.vinplay.dal.dao.impl.TopupVTCPayDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.topupVTCPay.DoiSoatTopupVTCPayResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatTopupVTCPayProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        DoiSoatTopupVTCPayResponse response = new DoiSoatTopupVTCPayResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        TopupVTCPayDaoImpl dao = new TopupVTCPayDaoImpl();
        List trans = dao.doiSoatTopupVtcPay(startTime, endTime);
        response.setSuccess(true);
        response.setErrorCode("0");
        response.setTrans(trans);
        return response.toJson();
    }
}


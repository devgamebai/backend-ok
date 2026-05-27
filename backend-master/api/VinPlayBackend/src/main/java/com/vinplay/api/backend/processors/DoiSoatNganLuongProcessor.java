/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.DoiSoatNganLuongServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultNganLuongResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.DoiSoatNganLuongServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultNganLuongResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DoiSoatNganLuongProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultNganLuongResponse response = new ResultNganLuongResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        if (timeStart == null || timeEnd == null) {
            return "MISSING INPUT PARAMETER";
        }
        DoiSoatNganLuongServiceImpl service = new DoiSoatNganLuongServiceImpl();
        List money = service.getDoiSoatData(timeStart, timeEnd);
        response.setMoney(money);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


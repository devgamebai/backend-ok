/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ExportDataCashoutByTopupResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.doisoat;

import com.vinplay.dal.service.impl.CashOutByTopUpServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ExportDataCashoutByTopupResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ExportDataCashoutByTopupProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ExportDataCashoutByTopupResponse response = new ExportDataCashoutByTopupResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String amount = request.getParameter("am");
        String code = request.getParameter("co");
        String partner = request.getParameter("pa");
        String provider = request.getParameter("pv");
        String type = request.getParameter("type");
        CashOutByTopUpServiceImpl service = new CashOutByTopUpServiceImpl();
        List trans = service.exportDataCashOutByTopup(provider, code, timeStart, timeEnd, partner, amount, type);
        response.setSuccess(true);
        response.setErrorCode("0");
        response.setTransactions(trans);
        return response.toJson();
    }
}


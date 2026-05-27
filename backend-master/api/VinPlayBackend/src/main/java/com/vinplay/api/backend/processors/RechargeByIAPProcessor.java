/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.RechargeByIAPServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultRechargeByIAPResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.RechargeByIAPServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultRechargeByIAPResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RechargeByIAPProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultRechargeByIAPResponse response = new ResultRechargeByIAPResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String code = request.getParameter("co");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String amount = request.getParameter("am");
        String orderId = request.getParameter("oid");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        RechargeByIAPServiceImpl service = new RechargeByIAPServiceImpl();
        long totalMoney = 0L;
        List trans = service.ListRechargeIAP(nickName, code, timeStart, timeEnd, amount, orderId, page);
        long totalRecord = service.countListRechargeIAP(nickName, code, timeStart, timeEnd, amount, orderId);
        long totalPages = 0L;
        totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
        totalMoney = service.totalMoney(nickName, code, timeStart, timeEnd, amount, orderId);
        response.setTotalMoney(totalMoney);
        response.setTotal(totalPages);
        response.setTotalRecord(totalRecord);
        response.setTransactions(trans);
        response.setTotalMoney(totalMoney);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


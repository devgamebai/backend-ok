/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultAgentTranferResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultAgentTranferResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class SearchAgentTranferMoneyStatusProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultAgentTranferResponse response = new ResultAgentTranferResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String type = request.getParameter("type");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int total = Integer.parseInt(request.getParameter("tr"));
        int page = Integer.parseInt(request.getParameter("p"));
        AgentServiceImpl service = new AgentServiceImpl();
        List trans = service.searchAgentTranferMoneyVinSale(nickName, timeStart, timeEnd, type, page, total);
        long totalRecord = 1000L;
        long totalVinReceive = 0L;
        long totalVinSend = 0L;
        if (type.equals("1")) {
            totalVinSend = service.totalMoneyVinSendFromAgentByStatus(nickName, type, timeStart, timeEnd);
        }
        if (type.equals("2")) {
            totalVinReceive = service.totalMoneyVinReceiveFromAgentByStatus(nickName, type, timeStart, timeEnd);
        }
        long totalPages = 0L;
        totalPages = 20L;
        response.setTotal(totalPages);
        response.setTotalRecord(1000L);
        response.setTransactions(trans);
        response.setTotalVinReceive(totalVinReceive);
        response.setTotalVinSend(totalVinSend);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


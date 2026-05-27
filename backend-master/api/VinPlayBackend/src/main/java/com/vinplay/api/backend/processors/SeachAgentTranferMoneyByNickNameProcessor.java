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

public class SeachAgentTranferMoneyByNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultAgentTranferResponse response = new ResultAgentTranferResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String status = request.getParameter("st");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String topDS = request.getParameter("tds");
        String type = request.getParameter("type");
        String process = request.getParameter("process");
        int page = Integer.parseInt(request.getParameter("p"));
        AgentServiceImpl service = new AgentServiceImpl();

        List trans = null;
        long totalRecord = 0;
        long totalVinReceive = 0;
        long totalVinSend = 0;
        long totalVinFee = 0;
        if (type !=null && type.equals("1")) {
            trans = service.searchAgentTranferMoney("", nickName, status, timeStart, timeEnd, topDS, page, process);
            totalRecord = service.countsearchAgentTranferMoney("", nickName, status, timeStart, timeEnd, topDS, process);
            totalVinReceive = service.totalMoneyVinReceiveFromAgent("", nickName, status, timeStart, timeEnd, topDS);
            totalVinSend = service.totalMoneyVinSendFromAgent("", nickName, status, timeStart, timeEnd, topDS);
            totalVinFee = service.totalMoneyVinFeeFromAgent("", nickName, status, timeStart, timeEnd, topDS);
        } else if (type !=null && type.equals("2")) {
            trans = service.searchAgentTranferMoney(nickName, "", status, timeStart, timeEnd, topDS, page);
            totalRecord = service.countsearchAgentTranferMoney(nickName, "", status, timeStart, timeEnd, topDS);
            totalVinReceive = service.totalMoneyVinReceiveFromAgent(nickName, "", status, timeStart, timeEnd, topDS);
            totalVinSend = service.totalMoneyVinSendFromAgent(nickName, "", status, timeStart, timeEnd, topDS);
            totalVinFee = service.totalMoneyVinFeeFromAgent(nickName, "", status, timeStart, timeEnd, topDS);
        } else {
            trans = service.searchAgentTranferMoney(nickName, status, timeStart, timeEnd, topDS, page);
            totalRecord = service.countsearchAgentTranferMoney(nickName, status, timeStart, timeEnd, topDS);
            totalVinReceive = service.totalMoneyVinReceiveFromAgent(nickName, status, timeStart, timeEnd, topDS);
            totalVinSend = service.totalMoneyVinSendFromAgent(nickName, status, timeStart, timeEnd, topDS);
            totalVinFee = service.totalMoneyVinFeeFromAgent(nickName, status, timeStart, timeEnd, topDS);
        }

        long totalPages = 0L;
        totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
        response.setTotal(totalPages);
        response.setTotalRecord(totalRecord);
        response.setTransactions(trans);
        response.setTotalVinReceive(totalVinReceive);
        response.setTotalVinSend(totalVinSend);
        response.setTotalFee(totalVinFee);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


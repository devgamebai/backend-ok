/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultAgentTotalTranferResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultAgentTotalTranferResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class SearchAgentTranferAdminProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultAgentTotalTranferResponse response = new ResultAgentTotalTranferResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String status = request.getParameter("st");
        AgentServiceImpl service = new AgentServiceImpl();
        List trans = null;
        try {
            double ratio1 = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_1");
            double ratio2 = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_2");
            double ratio2More = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_2_MORE");
            long minRefundFee2More = GameCommon.getValueLong((String)"REFUND_FEE_2_MORE");
            trans = service.searchAgentTranferAdmin(nickName, status, timeStart, timeEnd, ratio1, ratio2, ratio2More, minRefundFee2More);
            response.setRatio1(ratio1);
            response.setRatio2(ratio2);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


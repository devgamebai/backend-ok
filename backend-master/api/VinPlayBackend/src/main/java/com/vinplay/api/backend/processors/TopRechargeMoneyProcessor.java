/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.TopRechargeMoneyServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultTopRechargeMoneyResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.TopRechargeMoneyServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultTopRechargeMoneyResponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopRechargeMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    ResultTopRechargeMoneyResponse response = new ResultTopRechargeMoneyResponse(false, "1001");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String top = request.getParameter("t");
        String bot = request.getParameter("bt");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return this.response.toJson();
        }
        TopRechargeMoneyServiceImpl service = new TopRechargeMoneyServiceImpl();
        try {
            List trans = service.getTopRechargeMoney(Integer.parseInt(top), nickName, page, Integer.parseInt(bot));
            int totalRecord = Integer.parseInt(top);
            long totalPages = 0L;
            totalPages = totalRecord % 50 == 0 ? (long)(totalRecord / 50) : (long)(totalRecord / 50 + 1);
            this.response.setTotal(totalPages);
            this.response.setTotalRecord((long)totalRecord);
            this.response.setTransactions(trans);
            this.response.setSuccess(true);
            this.response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return this.response.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MarketingServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultMarketingToolResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultMarketingToolResponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class MarketingToolProcessor
implements BaseProcessor<HttpServletRequest, String> {
    ResultMarketingToolResponse response = new ResultMarketingToolResponse(false, "1001");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String campaign = request.getParameter("utm_campaign");
        String source = request.getParameter("utm_source");
        String medium = request.getParameter("utm_medium");
        String startDate = request.getParameter("ts");
        String endDate = request.getParameter("te");
        MarketingServiceImpl service = new MarketingServiceImpl();
        try {
            List trans = service.getMKTInfo(campaign, medium, source, startDate, endDate);
            int totalRecord = trans.size();
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


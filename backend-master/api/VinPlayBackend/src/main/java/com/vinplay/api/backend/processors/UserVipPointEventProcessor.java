/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.VipPointEventServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultUserVipPointEventResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.VipPointEventServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultUserVipPointEventResponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class UserVipPointEventProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultUserVipPointEventResponse response = new ResultUserVipPointEventResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String sort = request.getParameter("srt");
        String filed = request.getParameter("fd");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String bot = request.getParameter("bt");
        int page = Integer.parseInt(request.getParameter("p"));
        VipPointEventServiceImpl service = new VipPointEventServiceImpl();
        List trans = null;
        try {
            trans = service.listuserVipPoint(nickName, sort, filed, timeStart, timeEnd, page, bot);
        }
        catch (NumberFormatException | SQLException ex3) {
            Exception e = ex3;
            e.printStackTrace();
        }
        long totalRecord = 0L;
        try {
            totalRecord = service.countUserVipPoint(nickName, sort, filed, timeStart, timeEnd, bot);
        }
        catch (NumberFormatException | SQLException ex4) {
            Exception e2 = ex4;
            e2.printStackTrace();
        }
        long totalPages = 0L;
        totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
        response.setTotal(totalPages);
        response.setTotalRecord(totalRecord);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


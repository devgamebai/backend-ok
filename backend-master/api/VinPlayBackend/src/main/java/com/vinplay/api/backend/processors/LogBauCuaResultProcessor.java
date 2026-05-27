/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogBauCuaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogBauCuaResultResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogBauCuaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogBauCuaResultResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogBauCuaResultProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        LogBauCuaResultResponse response = new LogBauCuaResultResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referent_id = request.getParameter("rid");
        String room = request.getParameter("r");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        try {
            LogBauCuaServiceImpl service = new LogBauCuaServiceImpl();
            List trans = service.listLogBauCauResult(referent_id, room, timeStart, timeEnd, page);
            long totalRecord = service.countLogBauCauResult(referent_id, room, timeStart, timeEnd);
            long totalPages = 0L;
            totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
            response.setTotal(totalPages);
            response.setTotalRecord(totalRecord);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


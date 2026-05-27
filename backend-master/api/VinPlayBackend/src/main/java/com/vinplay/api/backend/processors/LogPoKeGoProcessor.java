/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogPoKeGoServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultPokegoResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogPoKeGoServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultPokegoResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogPoKeGoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultPokegoResponse response = new ResultPokegoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referentId = request.getParameter("rid");
        String userName = request.getParameter("un");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String betValue = request.getParameter("bv");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        LogPoKeGoServiceImpl service = new LogPoKeGoServiceImpl();
        try {
            List trans = service.listLogPokego(referentId, userName, moneyType, betValue, timeStart, timeEnd, page);
            long totalRecord = 1000L;
            long totalPages = 0L;
            totalPages = 20L;
            response.setTotal(totalPages);
            response.setTotalRecord(1000L);
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


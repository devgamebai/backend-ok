/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogSlotServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultKhoBauResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogSlotServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultKhoBauResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogSlotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultKhoBauResponse response = new ResultKhoBauResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referentId = request.getParameter("rid");
        String userName = request.getParameter("un");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String betValue = request.getParameter("bv");
        String gameName = request.getParameter("gn");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        if (gameName != null && !gameName.equals("")) {
            LogSlotServiceImpl service = new LogSlotServiceImpl();
            try {
                List trans = service.listLogSlot(referentId, userName, betValue, timeStart, timeEnd, page, gameName);
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
        return "MISSING PARAMETTER";
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogMoneyUserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogMoneyUserResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.api.processors.response.LogMoneyResponse;
import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class HistoryTransactionLogProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LogMoneyResponse response = new LogMoneyResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0 || moneyType <= 0 || moneyType > 5 || nickName == null || nickName.isEmpty()) {
            return response.toJson();
        }
        LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
        try {
            List trans = service.getHistoryTransactionLogMoney(nickName, moneyType, page);
            int totalPages = service.countHistoryTransactionLogMoney(nickName, moneyType);
            response.setTotalPages(totalPages);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return response.toJson();
    }
}


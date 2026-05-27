/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogMoneyUserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogUserMoneyResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.api.backend.response.LogMoneyResponse;
import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class SearchLogMoneyUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        LogMoneyResponse response = new LogMoneyResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String userName = request.getParameter("un");
        String timestart = request.getParameter("ts");
        String timeend = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String actionName = request.getParameter("ag");
        String serviceName = request.getParameter("sn");
        int page = 50;
        try {
            if (request.getParameter("p") != null) {
                page = Integer.parseInt(request.getParameter("p"));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        int like = Integer.parseInt(request.getParameter("lk"));
        int totalrecord = Integer.parseInt(request.getParameter("tr"));
        if (page < 0) {
            return response.toJson();
        }
        LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
        try {
            List trans = service.searchLogMoneyUser(nickName, userName, moneyType, serviceName, actionName, timestart, timeend, page, like, totalrecord);
            int totalPages = service.countsearchLogMoneyUser(nickName, moneyType, serviceName, actionName, timestart, timeend, like);
            response.setTotalPages(totalPages);
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


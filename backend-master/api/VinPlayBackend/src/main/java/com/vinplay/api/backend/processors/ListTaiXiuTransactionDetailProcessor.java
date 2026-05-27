/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogTaiXiuServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultTaiXiuDetailResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogTaiXiuServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultTaiXiuDetailResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListTaiXiuTransactionDetailProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultTaiXiuDetailResponse response = new ResultTaiXiuDetailResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referent_id = request.getParameter("rid");
        String betSide = request.getParameter("bs");
        String moneyType = request.getParameter("mt");
        String nickName = request.getParameter("nn");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        LogTaiXiuServiceImpl service = new LogTaiXiuServiceImpl();
        try {
            List trans = service.getLogTaiXiuDetail(referent_id, betSide, moneyType, nickName, page);
            long totalRecord = service.countLogTaiXiuDetail(referent_id, betSide, moneyType, nickName);
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


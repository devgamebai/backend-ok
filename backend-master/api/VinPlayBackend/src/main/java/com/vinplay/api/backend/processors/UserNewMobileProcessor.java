/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.UserNewMobileServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultUserNewMobileResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.UserNewMobileServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultUserNewMobileResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UserNewMobileProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultUserNewMobileResponse response = new ResultUserNewMobileResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String mobile = request.getParameter("m");
        String mobileold = request.getParameter("mo");
        int page = 1;
        try { String s = request.getParameter("p"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        if (page < 0) {
            return response.toJson();
        }
        try {
            UserNewMobileServiceImpl service = new UserNewMobileServiceImpl();
            List trans = service.searchUserNewMobile(nickName, mobile, mobileold, page);
            long totalRecord = service.countSearchUserNewMobile(nickName, mobile, mobileold);
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


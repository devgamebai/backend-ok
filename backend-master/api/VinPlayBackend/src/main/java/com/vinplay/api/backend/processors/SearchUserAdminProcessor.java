/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserForAdminServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultUserReponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.UserForAdminServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultUserReponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class SearchUserAdminProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultUserReponse response = new ResultUserReponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String userName = request.getParameter("un");
        String nickName = request.getParameter("nn");
        String mobile = request.getParameter("m");
        String field = request.getParameter("fd");
        String sort = request.getParameter("srt");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String daily = request.getParameter("dl");
        String bot = request.getParameter("bt");
        String like = request.getParameter("lk");
        String email = request.getParameter("email");
        int page = Integer.parseInt(request.getParameter("p"));
        int total = Integer.parseInt(request.getParameter("tr"));
        if (page < 0) {
            return response.toJson();
        }
        if (total < 0) {
            return response.toJson();
        }
        UserForAdminServiceImpl service = new UserForAdminServiceImpl();
        try {
            List trans = service.searchUserAdmin(userName, nickName, mobile, field, sort, daily, timeStart, timeEnd, page, total, bot, like, email);
            long totalRecord  = service.countSearchUserAdmin(userName, nickName, mobile, field, sort, daily, timeStart, timeEnd, bot);;
            response.setTotal(100L);
            response.setTotalRecord((long)totalRecord);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


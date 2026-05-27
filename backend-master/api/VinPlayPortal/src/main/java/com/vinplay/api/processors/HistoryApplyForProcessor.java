/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.dao.Impl.HistoryApplyForDaoImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.payment.dao.Impl.HistoryApplyForDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class HistoryApplyForProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(HistoryApplyForProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        logger.info(("Request history apply for nickname= " + nickname));
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return BaseResponse.error((String)"5", (String)"nickname l\u00e0 b\u1eaft bu\u1ed9c");
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"accessToken l\u00e0 b\u1eaft bu\u1ed9c");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            HistoryApplyForDaoImpl historyApplyForDao = new HistoryApplyForDaoImpl();
            List list = historyApplyForDao.getAllByNickname(nickname);
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("data", list);
            result.put("total", list.size());
            return new BaseResponse().success(result);
        }
        catch (SQLException e) {
            logger.error("Error getting history apply for", (Throwable)e);
            return BaseResponse.error((String)"99", (String)("L\u1ed7i h\u1ec7 th\u1ed1ng: " + e.getMessage()));
        }
    }
}


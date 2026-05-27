/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.dao.Impl.PaymentHistoryDaoImpl
 *  com.payment.dao.Impl.PaymentHistoryLiveDaoImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.payment.dao.Impl.PaymentHistoryDaoImpl;
import com.payment.dao.Impl.PaymentHistoryLiveDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PaymentHistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(PaymentHistoryProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        int page = 0;
        try {
            page = Integer.parseInt(request.getParameter("p"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        int maxItem = 20;
        try {
            String maxItemStr = request.getParameter("mi");
            if (StringUtils.isNotBlank((CharSequence)maxItemStr)) {
                maxItem = Integer.parseInt(maxItemStr);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        logger.info(("Request payment history nickname= " + nickname + ", page: " + page + ", maxItem: " + maxItem));
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return BaseResponse.error((String)"5", (String)"nickname l\u00e0 b\u1eaft bu\u1ed9c");
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"accessToken l\u00e0 b\u1eaft bu\u1ed9c");
        }
        if (page < 0) {
            return BaseResponse.error((String)"5", (String)"page ph\u1ea3i >= 0");
        }
        if (maxItem <= 0) {
            return BaseResponse.error((String)"5", (String)"maxItem ph\u1ea3i > 0");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        UserModel userModel = null;
        try {
            userModel = userService.getUserByNickName(nickname);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            HashMap<String, Object> result = new HashMap<String, Object>();
            if (userModel == null || !userModel.isLive()) {
                PaymentHistoryDaoImpl paymentHistoryDao = new PaymentHistoryDaoImpl();
                List list = paymentHistoryDao.getAllByNickname(nickname, page, maxItem);
                int totalCount = paymentHistoryDao.getTotalCount(nickname);
                result.put("data", list);
                result.put("total", totalCount);
                result.put("page", page);
                result.put("maxItem", maxItem);
                result.put("totalPages", (int)Math.ceil((double)totalCount / (double)maxItem));
            } else {
                PaymentHistoryLiveDaoImpl paymentHistoryLiveDao = new PaymentHistoryLiveDaoImpl();
                List list = paymentHistoryLiveDao.getAllByNickname(nickname, page, maxItem);
                int totalCount = paymentHistoryLiveDao.getTotalCount(nickname);
                result.put("data", list);
                result.put("total", totalCount);
                result.put("page", page);
                result.put("maxItem", maxItem);
                result.put("totalPages", (int)Math.ceil((double)totalCount / (double)maxItem));
            }
            return new BaseResponse().success(result);
        }
        catch (SQLException e) {
            logger.error("Error getting payment history", (Throwable)e);
            return BaseResponse.error((String)"99", (String)("L\u1ed7i h\u1ec7 th\u1ed1ng: " + e.getMessage()));
        }
    }
}


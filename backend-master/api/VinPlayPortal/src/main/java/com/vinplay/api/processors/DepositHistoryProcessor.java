/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.entities.DepositPaygateReponse
 *  com.vinplay.payment.service.impl.RechargePayWellServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.service.impl.RechargePayWellServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DepositHistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(DepositHistoryProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        int status = 0;
        try {
            status = Integer.parseInt(request.getParameter("st"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        int page = 0;
        try {
            page = Integer.parseInt(request.getParameter("p"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        int maxItem = 0;
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        String fromTime = request.getParameter("ft");
        String endTime = request.getParameter("et");
        String accessToken = request.getParameter("at");
        logger.info(("Request payment history nickname= " + nickname + ", status: " + status + ", page: " + page + ", maxItem: " + maxItem + ", fromTime: " + fromTime + ", endTime: " + endTime + ", accessToken: " + accessToken));
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return BaseResponse.error((String)"5", (String)"nickname is required");
        }
        if (page < 0) {
            return BaseResponse.error((String)"5", (String)"page <0");
        }
        if (maxItem < 0) {
            return BaseResponse.error((String)"5", (String)"maxItem <0");
        }
        if (StringUtils.isBlank((CharSequence)fromTime) || StringUtils.isBlank((CharSequence)endTime)) {
            return BaseResponse.error((String)"5", (String)"endTime , fromtime is required");
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"accessToken is required");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (isToken) {
            RechargePayWellServiceImpl service = new RechargePayWellServiceImpl();
            DepositPaygateReponse response = service.search(nickname, status, page, maxItem, fromTime, endTime, accessToken);
            return new BaseResponse().success(response);
        }
        return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.entities.Response
 *  com.vinplay.usercore.service.impl.UserBankServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.api.processors.payment.InsertOrUpdateBankProcessor;
import com.vinplay.payment.entities.Response;
import com.vinplay.usercore.service.impl.UserBankServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class BankSearchProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(InsertOrUpdateBankProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String bankName = request.getParameter("bn");
        String bankNumber = request.getParameter("bnum");
        String accessToken = request.getParameter("at");
        String pageNumberStr = request.getParameter("pn");
        String limitStr = request.getParameter("l");
        int pageNumber = 0;
        int limit = 0;
        try {
            pageNumber = Integer.parseInt(pageNumberStr);
            limit = Integer.parseInt(limitStr);
        }
        catch (NumberFormatException e) {
            return BaseResponse.error((String)"5", (String)"pageNumber or limit format");
        }
        if (StringUtils.isBlank((CharSequence)nickName)) {
            return BaseResponse.error((String)"5", (String)"nickName is null or empty");
        }
        logger.info(("Request BankSearchProcessor nickName= " + nickName + ", bankName: " + bankName + ", bankNumber: " + bankNumber));
        if (StringUtils.isBlank((CharSequence)nickName)) {
            return BaseResponse.error((String)"5", (String)"nickName is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)bankName)) {
            bankName = "";
        }
        if (StringUtils.isBlank((CharSequence)bankNumber)) {
            bankNumber = "";
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"accessToken is null or empty");
        }
        UserBankServiceImpl bankService = new UserBankServiceImpl();
        UserServiceImpl userService = new UserServiceImpl();
        Response res = new Response(1, "");
        try {
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (!isToken) {
                return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
            }
            res = bankService.search(nickName, bankName, bankNumber, 0, pageNumber, limit);
            if (res.getCode() == 0) {
                return new BaseResponse().success(res.getData());
            }
            return BaseResponse.error((String)(res.getCode() + ""), (String)res.getMessage());
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }
}


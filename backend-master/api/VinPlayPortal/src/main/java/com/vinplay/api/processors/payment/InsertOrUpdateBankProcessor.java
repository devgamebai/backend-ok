/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.entities.Response
 *  com.vinplay.payment.entities.UserBank
 *  com.vinplay.usercore.service.impl.UserBankServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.payment.entities.Response;
import com.vinplay.payment.entities.UserBank;
import com.vinplay.usercore.service.impl.UserBankServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.Timestamp;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class InsertOrUpdateBankProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(InsertOrUpdateBankProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String bankName = request.getParameter("bn");
        String customerName = request.getParameter("cn");
        String bankNumber = request.getParameter("bnum");
        String branch = request.getParameter("br");
        String type = request.getParameter("t");
        String id = request.getParameter("id");
        String accessToken = request.getParameter("at");
        int status = 1;
        Timestamp createDate = null;
        Timestamp updateDate = null;
        if (!"0".equals(type)) {
            updateDate = new Timestamp(System.currentTimeMillis());
            return BaseResponse.error((String)"5", (String)"You can not allow access to update information. Please contact customer care.");
        }
        createDate = new Timestamp(System.currentTimeMillis());
        logger.info(("Request InsertOrUpdateBankProcessor nickName= " + nickName + ", bankName: " + bankName + ", customerName: " + customerName + ", bankNumber: " + bankNumber + ", branch: " + branch));
        if (StringUtils.isBlank((CharSequence)nickName)) {
            return BaseResponse.error((String)"5", (String)"nickName is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)bankName)) {
            return BaseResponse.error((String)"5", (String)"bankName is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)customerName)) {
            return BaseResponse.error((String)"5", (String)"customerName is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)bankNumber)) {
            return BaseResponse.error((String)"5", (String)"bankNumber is null or empty");
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
                return BaseResponse.error((String)"4", (String)"This session is expried or not exist");
            }
            UserCacheModel user = userService.getUser(nickName);
            if (branch.length() > 20) {
                return BaseResponse.error((String)"5", (String)"Chi nh\u00e1nh ng\u00e2n h\u00e0ng c\u00f3 t\u1ed1i \u0111a 20 k\u00ed t\u1ef1");
            }
            if ("0".equals(type)) {
                UserBank userBank = new UserBank(null, Integer.valueOf(user.getId()), nickName, bankName, customerName, bankNumber, Integer.valueOf(status), createDate, branch, updateDate, "");
                res = bankService.add(userBank);
            } else {
                UserBank userBank = new UserBank(Long.valueOf(Long.parseLong(id)), Integer.valueOf(user.getId()), nickName, bankName, customerName, bankNumber, Integer.valueOf(status), createDate, branch, updateDate, nickName);
                updateDate = new Timestamp(System.currentTimeMillis());
                res = bankService.update(userBank);
            }
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


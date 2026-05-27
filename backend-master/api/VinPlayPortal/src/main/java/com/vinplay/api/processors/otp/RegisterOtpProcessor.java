/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.otp.service.AccountSecurityService
 *  com.vinplay.otp.service.impl.AccountSecurityServiceImpl
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.otp;

import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.otp.service.AccountSecurityService;
import com.vinplay.otp.service.impl.AccountSecurityServiceImpl;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class RegisterOtpProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    AccountSecurityService service = new AccountSecurityServiceImpl();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String sender = request.getParameter("type");
        String mobile = request.getParameter("m");
        if (StringUtils.isBlank((CharSequence)sender)) {
            return BaseResponse.error((String)"5", (String)"sender is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)mobile)) {
            return BaseResponse.error((String)"5", (String)"mobile is null or empty");
        }
        UserModel userModel = this.getUser(param);
        if (userModel == null) {
            return notAuth;
        }
        if (userModel.isVerifyMobile() && userModel.getMobile().trim().toLowerCase().contains(mobile.toLowerCase().trim())) {
            return BaseResponse.success((String)"0", (String)"S\u1ed1 \u0111i\u1ec7n tho\u1ea1i \u0111\u00e3 \u0111\u01b0\u1ee3c x\u00e1c th\u1ef1c!", new HashMap());
        }
        try {
            if (this.userService.checkMobile(mobile)) {
                return BaseResponse.error((String)"5", (String)"S\u1ed1 \u0111i\u1ec7n tho\u1ea1i \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng!");
            }
            OtpMessage msg = new OtpMessage();
            msg.setNickname(userModel.getNickname());
            msg.setType("register");
            msg.setMobile(mobile);
            msg.setSender(sender);
            OTPSenderResponse result = this.service.sendMessageOTP(msg);
            if (!result.isSuccess()) {
                return BaseResponse.error((String)"99", (String)result.getMessage());
            }
            return BaseResponse.success((String)"0", (String)"G\u1eedi otp th\u00e0nh c\u00f4ng!", new HashMap());
        }
        catch (Exception e) {
            logger.debug(e);
            return BaseResponse.error((String)"99", (String)"Server Error!");
        }
    }
}


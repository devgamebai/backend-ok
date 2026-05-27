/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.utils.AuthenticationUtils
 *  com.vinplay.usercore.service.impl.UserForAdminServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.user;

import com.vinplay.api.backend.response.UserNormalInfoResponse;
import com.vinplay.dal.utils.AuthenticationUtils;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class CheckAppOtpByNicknameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String otp = request.getParameter("otp");
        BaseResponseModel response = new BaseResponseModel(false, "1001", null);
        if (nickName == null || otp == null) {
            return response.toJson();
        }
        String dataAuthen = "";
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String)headerNames.nextElement();
            if (!"Authorization".equals(headerName)) continue;
            Enumeration headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                dataAuthen = (String)headers.nextElement();
            }
        }
        if (dataAuthen.isEmpty()) {
            return response.toJson();
        }
        boolean baseAuthen = AuthenticationUtils.decodeBaseAuthen((String)dataAuthen);
        if (!baseAuthen) {
            return response.toJson();
        }
        try {
            OtpServiceImpl otpService = new OtpServiceImpl();
            int rs = otpService.checkAppOTP(nickName, otp);
            response = new BaseResponseModel(true, "0", rs);
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


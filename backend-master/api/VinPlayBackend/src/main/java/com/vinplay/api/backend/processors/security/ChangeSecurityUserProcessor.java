/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.security;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ChangeSecurityUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        try {
            String nickname = request.getParameter("nn");
            int action = Integer.parseInt(request.getParameter("ac"));
            String actionType = request.getParameter("tu");
            String otp = request.getParameter("otp");
            String type = request.getParameter("type");
            String ad = request.getParameter("ad");
            if (nickname != null && (action == 4 || action == 5) && (actionType.equals("0") || actionType.equals("1"))) {
                int code = BackendUtils.checkOTPSuperAdmin(otp, type, ad);
                if (code == 0) {
                    SecurityServiceImpl ser = new SecurityServiceImpl();
                    boolean res = ser.changeSecurityUser(nickname, action, actionType);
                    if (res) {
                        response.setSuccess(true);
                        response.setErrorCode("0");
                    }
                } else if (code == 3) {
                    response.setErrorCode("1008");
                } else if (code == 4) {
                    response.setErrorCode("1021");
                }
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


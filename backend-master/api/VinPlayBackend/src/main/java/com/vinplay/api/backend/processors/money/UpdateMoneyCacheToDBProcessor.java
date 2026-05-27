/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateMoneyCacheToDBProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String otp = request.getParameter("otp");
        logger.debug((Object)("Request RestoreMoney: nickname: " + nickname));
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (nickname != null && !nickname.trim().isEmpty() && otp != null && otp.equals("FQmMFjF9AKUrpuen")) {
            try {
                UserServiceImpl ser = new UserServiceImpl();
                response = ser.updateMoneyCacheToDB(nickname);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        logger.debug((Object)("Response RestoreMoney: " + response.toJson()));
        return response.toJson();
    }
}


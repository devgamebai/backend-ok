/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.LoginResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.bot;

import com.vinplay.api.utils.PortalUtils;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.LoginResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LoginBotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String res = "1";
        try {
            LoginResponse loginres;
            String un = request.getParameter("un");
            UserServiceImpl userService = new UserServiceImpl();
            UserModel userModel = userService.getUserByUserName(un);
            if (userModel != null && userModel.isBot() && (loginres = PortalUtils.loginSuccess(userModel, request)).isSuccess()) {
                res = "0";
            }
        }
        catch (Exception e) {
            logger.debug(e);
        }
        return res;
    }
}


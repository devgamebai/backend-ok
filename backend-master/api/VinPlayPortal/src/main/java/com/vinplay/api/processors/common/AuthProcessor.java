/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 */
package com.vinplay.api.processors.common;

import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public abstract class AuthProcessor {
    protected UserService userService = new UserServiceImpl();
    public static final String notAuth = BaseResponse.error((String)"4", (String)"B\u1ea1n ch\u01b0a \u0111\u0103ng nh\u1eadp!");

    public UserModel getUser(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return null;
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return null;
        }
        boolean isToken = this.userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return null;
        }
        UserModel userByNickName = null;
        try {
            userByNickName = this.userService.getUserByNickName(nickname);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return userByNickName;
    }
}


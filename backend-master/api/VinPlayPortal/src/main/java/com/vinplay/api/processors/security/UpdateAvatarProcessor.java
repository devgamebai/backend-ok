/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.security;

import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateAvatarProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel res = new BaseResponseModel(false, "1001");
        String nickname = request.getParameter("nn");
        String avatar = request.getParameter("avatar");
        if (nickname != null && avatar != null) {
            try {
                SecurityServiceImpl ser = new SecurityServiceImpl();
                res = ser.updateAvatar(nickname, avatar);
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return res.toJson();
    }
}


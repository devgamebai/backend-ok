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
package com.vinplay.api.backend.processors;

import com.vinplay.api.backend.response.UserInfoNormalModel;
import com.vinplay.api.backend.response.UserNormalInfoResponse;
import com.vinplay.dal.utils.AuthenticationUtils;
import com.vinplay.usercore.service.impl.UserForAdminServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetUserByNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        UserNormalInfoResponse response = new UserNormalInfoResponse(false, "1001", null);
        if (nickName == null || nickName.isEmpty()) {
            return response.toJson();
        }
        try {
            UserForAdminServiceImpl service = new UserForAdminServiceImpl();
            UserModel userModel = service.getUserNormalByNickName(nickName);
            UserInfoNormalModel userNormal = new UserInfoNormalModel(userModel.getUsername(), userModel.getNickname(), userModel.getMobile(), userModel.getVinTotal());
            response = new UserNormalInfoResponse(true, "0", userNormal);
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


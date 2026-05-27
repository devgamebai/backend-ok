/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.UserLevel
 *  com.vinplay.usercore.service.impl.UserLevelServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.wages;

import com.vinplay.usercore.entities.UserLevel;
import com.vinplay.usercore.service.impl.UserLevelServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class UpdateNicknameInviteProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(UpdateNicknameInviteProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String action = request.getParameter("ac");
        String accessToken = request.getParameter("at");
        String nickname = request.getParameter("nn");
        String parent_user = request.getParameter("inv");
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return BaseResponse.error((String)"5", (String)"Nickname kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"Access token kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if ("update".equalsIgnoreCase(action) && StringUtils.isBlank((CharSequence)parent_user)) {
            return BaseResponse.error((String)"5", (String)"M\u00e3 ng\u01b0\u1eddi gi\u1edbi thi\u1ec7u kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            UserLevelServiceImpl service = new UserLevelServiceImpl();
            String result = "";
            switch (action) {
                case "update": {
                    result = service.create(nickname, parent_user);
                    if ("success".equalsIgnoreCase(result)) {
                        return BaseResponse.success((String)"0", (String)result, result);
                    }
                    return BaseResponse.error((String)"5", (String)result);
                }
            }
            UserLevel userLevel = service.getByNickName(nickname);
            result = userLevel == null ? "" : userLevel.getParent_user();
            return BaseResponse.success((String)"0", (String)"success", result);
        }
        catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error((String)"1001", (String)"L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.service.impl.UserWagesServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.wages;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.service.impl.UserWagesServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReceivedMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(ReceivedMoneyProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String idStr = request.getParameter("id");
        String action = request.getParameter("ac");
        String accessToken = request.getParameter("at");
        String nickname = request.getParameter("nn");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Access token kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            UserWagesServiceImpl service = new UserWagesServiceImpl();
            String result = "";
            if ("all".equalsIgnoreCase(action)) {
                result = service.receivedAllMoney(nickname);
            } else {
                long id = 0L;
                try {
                    id = Long.parseLong(idStr);
                }
                catch (Exception e) {
                    id = 0L;
                }
                if (id == 0L) {
                    return BaseResponse.error((String)"5", (String)"Access token kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
                }
                result = service.receivedMoney(id);
            }
            return "success".equalsIgnoreCase(result) ? BaseResponse.success(result, (long)0L) : BaseResponse.error((String)"1001", (String)result);
        }
        catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error((String)"1001", (String)"L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
        }
    }
}


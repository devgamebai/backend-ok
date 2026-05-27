/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.AgentBankServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.usercore.service.impl.AgentBankServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class SearchAgentBankProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        int maxItem;
        int page;
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String agentCode = request.getParameter("code");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if (StringUtils.isBlank((String)accessToken)) {
            return BaseResponse.error((String)"5", (String)"M\u00e3 phi\u00ean l\u00e0m vi\u1ec7c kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if (StringUtils.isBlank((String)agentCode)) {
            return BaseResponse.error((String)"5", (String)"M\u00e3 \u0111\u1ea1i l\u00fd kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        }
        catch (NumberFormatException e) {
            page = 1;
        }
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        }
        catch (NumberFormatException e) {
            maxItem = 10;
        }
        AgentBankServiceImpl service = new AgentBankServiceImpl();
        try {
            Map rs = service.search(null, agentCode, page, maxItem);
            return BaseResponse.success(rs.get("agentBanks"), (long)Long.parseLong(rs.get("total").toString()));
        }
        catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error((String)"-1", (String)"L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
        }
    }
}


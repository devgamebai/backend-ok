/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.utils.ShotFishUtils
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.shotfish;

import com.vinplay.utils.ShotFishUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;

public class Withdraw
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String moneyStr = request.getParameter("mn");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname is not empty");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Access token is not empty");
        }
        Long money = 0L;
        try {
            money = Long.parseLong(moneyStr);
        }
        catch (Exception e) {
            return BaseResponse.error((String)"1001", (String)"S\u1ed1 ti\u1ec1n nh\u1eadp kh\u00f4ng \u0111\u00fang");
        }
        if (money < 0L) {
            return BaseResponse.error((String)"5", (String)"S\u1ed1 ti\u1ec1n nh\u1eadp kh\u00f4ng \u0111\u00fang");
        }
        return ShotFishUtils.WithDraw((String)nickname, (String)accessToken, (Long)money).toJson();
    }
}


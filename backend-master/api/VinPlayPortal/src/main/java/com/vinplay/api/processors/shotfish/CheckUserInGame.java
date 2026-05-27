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

public class CheckUserInGame
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname is not empty");
        }
        return ShotFishUtils.CheckUserInGame((String)nickname).toJson();
    }
}


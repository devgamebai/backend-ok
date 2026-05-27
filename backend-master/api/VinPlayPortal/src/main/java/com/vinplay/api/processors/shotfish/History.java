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

public class History
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String startTime = request.getParameter("st");
        String endTime = request.getParameter("et");
        if (startTime == null || startTime.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Start time is not empty");
        }
        if (endTime == null || endTime.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"End time is not empty");
        }
        return ShotFishUtils.History((String)startTime, (String)endTime).toJson();
    }
}


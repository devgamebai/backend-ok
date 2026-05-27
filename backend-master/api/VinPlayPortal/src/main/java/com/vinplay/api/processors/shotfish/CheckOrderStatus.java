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

public class CheckOrderStatus
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String orderId = request.getParameter("oid");
        if (orderId == null || orderId.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"OrderId is not empty");
        }
        return ShotFishUtils.CheckOrderStatus((String)orderId).toJson();
    }
}


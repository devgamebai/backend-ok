/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.RechargeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.UpdateAllPendingCardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dichvuthe.service.impl.RechargeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.UpdateAllPendingCardResponse;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class UpdateAllPendingCardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        UpdateAllPendingCardResponse response = new UpdateAllPendingCardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String actor = request.getParameter("act");
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        if (!(startTime == null || startTime.equals("") || endTime == null || endTime.equals("") || actor == null || actor.equals(""))) {
            RechargeServiceImpl service = new RechargeServiceImpl();
            Map mapRes = new HashMap();
            try {
                mapRes = service.updatePendingCardStatus(startTime, endTime, actor);
            }
            catch (Exception e) {
                e.printStackTrace();
                return response.toJson();
            }
            response.setSuccess(true);
            response.setErrorCode("0");
            response.setTotalRecord(((Long)mapRes.get("totalRecord")).longValue());
            response.setSuccessRecord(((Long)mapRes.get("successRecord")).longValue());
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}


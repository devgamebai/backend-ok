/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.GetUserIndexServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.UserIndexResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.GetUserIndexServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.UserIndexResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetUserIndexProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        UserIndexResponse response = new UserIndexResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        GetUserIndexServiceImpl service = new GetUserIndexServiceImpl();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        if (timeStart == null || timeEnd == null) {
            return "MISSING INPUT PARAMETER";
        }
        try {
            String referralCode = request.getParameter("rc");
            int register = service.getRegister(timeStart, timeEnd, referralCode);
            int recharge = service.getRecharge(timeStart, timeEnd, referralCode);
            int secMobile = service.getSecMobile(timeStart, timeEnd, referralCode);
            int both = service.getBoth(timeStart, timeEnd, referralCode);
            response.setSuccess(true);
            response.setErrorCode("0");
            response.setRegister(register);
            response.setRecharge(recharge);
            response.setSecMobile(secMobile);
            response.setBoth(both);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


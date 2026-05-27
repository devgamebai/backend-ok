/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.VippointServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.vippoint;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.usercore.service.impl.VippointServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateVippointEventProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        String res = "1001";
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String typeUpdate = request.getParameter("tu");
        String valueStr = request.getParameter("va");
        String otp = request.getParameter("otp");
        String type = request.getParameter("type");
        String ad = request.getParameter("ad");
        if (nickname != null && !nickname.isEmpty() && typeUpdate != null && valueStr != null && (typeUpdate.equals("0") || typeUpdate.equals("1"))) {
            try {
                int value = Integer.parseInt(valueStr);
                int code = BackendUtils.checkOTPSuperAdmin(otp, type, ad);
                if (code == 0) {
                    VippointServiceImpl service = new VippointServiceImpl();
                    if (service.updateVippointEvent(nickname, value, typeUpdate) == 0) {
                        res = "0";
                    }
                } else if (code == 3) {
                    res = "1008";
                } else if (code == 4) {
                    res = "1021";
                }
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return res;
    }
}


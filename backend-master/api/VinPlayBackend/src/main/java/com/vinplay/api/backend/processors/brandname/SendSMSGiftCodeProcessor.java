/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.brandname;

import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.UserValidaton;
import javax.servlet.http.HttpServletRequest;

public class SendSMSGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        block6 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            try {
                AlertServiceImpl alertService = new AlertServiceImpl();
                OtpServiceImpl otpService = new OtpServiceImpl();
                String mobile = request.getParameter("m");
                String content = request.getParameter("ct");
                String giftcode = request.getParameter("gc");
                if (mobile == null || mobile.isEmpty() || content == null || content.isEmpty() || giftcode == null || giftcode.isEmpty() || !content.contains("%s")) break block6;
                String contentGiftCode = "";
                if (mobile.contains(",") && giftcode.contains(",")) {
                    String[] arrGC;
                    String[] arr = mobile.split(",");
                    if (arr.length == (arrGC = giftcode.split(",")).length) {
                        for (int i = 0; i < arr.length; ++i) {
                            String m = arr[i].trim();
                            if (m.isEmpty() || m.length() < 5 || !UserValidaton.validateMobileVN((String)(m = otpService.revertMobile(m)))) continue;
                            contentGiftCode = String.format(content, arrGC[i]);
                            alertService.sendSMS2One(m, contentGiftCode, false);
                        }
                        return "0";
                    }
                    break block6;
                }
                mobile = mobile.trim();
                mobile = otpService.revertMobile(mobile);
                giftcode = giftcode.trim();
                contentGiftCode = String.format(content, giftcode);
                if (UserValidaton.validateMobileVN((String)mobile)) {
                    alertService.sendSMS2One(mobile, contentGiftCode, false);
                    return "0";
                }
                return "2";
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "1";
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.brandname;

import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.UserValidaton;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class SendSMSProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            AlertServiceImpl alertService = new AlertServiceImpl();
            String mobile = request.getParameter("m");
            String content = request.getParameter("ct");
            if (mobile != null && !mobile.isEmpty() && content != null && !content.isEmpty()) {
                if (mobile.contains(",")) {
                    String[] arr = mobile.split(",");
                    ArrayList<String> mList = new ArrayList<String>();
                    for (String m : arr) {
                        if ((m = m.trim()).isEmpty()) continue;
                        if (!UserValidaton.validateMobileVN((String)m)) {
                            return "2";
                        }
                        mList.add(m);
                    }
                    if (mList.size() > 0) {
                        alertService.sendSMS2List(mList, content, false);
                    }
                } else {
                    if (!UserValidaton.validateMobileVN((String)(mobile = mobile.trim()))) {
                        return "2";
                    }
                    alertService.sendSMS2One(mobile, content, false);
                }
            }
            return "0";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "1";
        }
    }
}


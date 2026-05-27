/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl
 *  com.vinplay.dichvuthe.response.LogSMSPlusCheckMoResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.log;

import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.response.LogSMSPlusCheckMoResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogRechargeSMSPlusCheckMOProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        LogSMSPlusCheckMoResponse res = new LogSMSPlusCheckMoResponse(false, "1001");
        try {
            String mobile = request.getParameter("m");
            String amounts = request.getParameter("am");
            String codes = request.getParameter("co");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pages = request.getParameter("p");
            if (mobile != null && amounts != null && codes != null && startTime != null && endTime != null && pages != null) {
                int page = Integer.parseInt(pages);
                int amount = -1;
                if (amounts != null && !amounts.isEmpty()) {
                    amount = Integer.parseInt(amounts);
                }
                int code = -1;
                if (codes != null && !codes.isEmpty()) {
                    code = Integer.parseInt(codes);
                }
                RechargeDaoImpl dao = new RechargeDaoImpl();
                res = dao.getLogSMSPlusCheckMO(mobile, amount, code, startTime, endTime, page);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.OtpDaoImpl
 *  com.vinplay.usercore.response.LogSMSOtpResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.log;

import com.vinplay.usercore.dao.impl.OtpDaoImpl;
import com.vinplay.usercore.response.LogSMSOtpResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogSmsOtpProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        LogSMSOtpResponse res = new LogSMSOtpResponse(false, "1001");
        try {
            String mobile = request.getParameter("m");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pages = request.getParameter("p");
            String requestId = request.getParameter("rid");
            if (mobile != null && startTime != null && endTime != null && pages != null) {
                int page = Integer.parseInt(pages);
                OtpDaoImpl dao = new OtpDaoImpl();
                res = dao.getLogSMSOtp(mobile, startTime, endTime, page, requestId);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res.toJson();
    }
}


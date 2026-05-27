/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.brandname.dao.impl.BrandnameDaoImpl
 *  com.vinplay.usercore.response.LogBrandnameResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.log;

import com.vinplay.brandname.dao.impl.BrandnameDaoImpl;
import com.vinplay.usercore.response.LogBrandnameResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogBrannameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        LogBrandnameResponse res = new LogBrandnameResponse(false, "1001");
        try {
            String mobile = request.getParameter("m");
            String code = request.getParameter("co");
            String status = request.getParameter("st");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pages = request.getParameter("p");
            String requestId = request.getParameter("rid");
            if (mobile != null && code != null && status != null && startTime != null && endTime != null && pages != null) {
                int page = Integer.parseInt(pages);
                BrandnameDaoImpl dao = new BrandnameDaoImpl();
                res = dao.getLogBrandname(mobile, code, status, startTime, endTime, page, requestId);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res.toJson();
    }
}


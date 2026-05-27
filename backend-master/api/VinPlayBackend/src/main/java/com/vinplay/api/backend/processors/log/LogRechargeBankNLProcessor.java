/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl
 *  com.vinplay.usercore.response.LogRechargeBankNLResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.log;

import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.usercore.response.LogRechargeBankNLResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogRechargeBankNLProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        LogRechargeBankNLResponse res = new LogRechargeBankNLResponse(false, "1001");
        try {
            String nickname = request.getParameter("nn");
            String bank = request.getParameter("b");
            String transId = request.getParameter("tid");
            String ip = request.getParameter("ip");
            String transNo = request.getParameter("tno");
            String status = request.getParameter("st");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pages = request.getParameter("p");
            if (nickname != null && bank != null && transId != null && ip != null && transNo != null && status != null && endTime != null && pages != null) {
                int page = Integer.parseInt(pages);
                RechargeDaoImpl dao = new RechargeDaoImpl();
                res = dao.getLogNL(nickname, bank, transId, ip, transNo, status, startTime, endTime, page);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res.toJson();
    }
}


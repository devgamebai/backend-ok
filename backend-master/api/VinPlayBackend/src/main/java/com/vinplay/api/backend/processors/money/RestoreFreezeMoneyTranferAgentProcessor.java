/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class RestoreFreezeMoneyTranferAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        String response = "1";
        HttpServletRequest request = (HttpServletRequest)param.get();
        String sessionId = request.getParameter("sid");
        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                MoneyInGameServiceImpl service = new MoneyInGameServiceImpl();
                boolean res = false;
                if (sessionId != null) {
                    res = service.restoreFreezeTranferAgent(sessionId);
                }
                if (res) {
                    response = "0";
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
            return response;
        }
        return "MISSING INPUT PARAMERTER";
    }
}


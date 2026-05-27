/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetListFreezeMoneyTranferAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String gamename = "FreezeMoneyTranferAgent";
        String moneyType = "vin";
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        String pages = request.getParameter("p");
        String status = request.getParameter("st");
        if (nickname != null && startTime != null && endTime != null && pages != null && status != null && !pages.isEmpty()) {
            try {
                int page = Integer.parseInt(pages);
                MoneyInGameServiceImpl service = new MoneyInGameServiceImpl();
                List res = service.getListFreezeMoneyAgentTranfer("FreezeMoneyTranferAgent", nickname, "vin", startTime, endTime, page, status);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString((Object)res);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
        }
        return "MISSING INPUT PARAMETER";
    }
}


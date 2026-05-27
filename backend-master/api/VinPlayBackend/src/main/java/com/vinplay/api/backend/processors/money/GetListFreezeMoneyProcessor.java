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

public class GetListFreezeMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String gamename = request.getParameter("gn");
        String moneyType = request.getParameter("mt");
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        String pages = request.getParameter("p");
        if (nickname != null && gamename != null && moneyType != null && startTime != null && endTime != null && pages != null) {
            try {
                int page = Integer.parseInt(pages);
                MoneyInGameServiceImpl service = new MoneyInGameServiceImpl();
                List res = service.getListFreeze(gamename, nickname, moneyType, startTime, endTime, page);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString((Object)res);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return "";
    }
}


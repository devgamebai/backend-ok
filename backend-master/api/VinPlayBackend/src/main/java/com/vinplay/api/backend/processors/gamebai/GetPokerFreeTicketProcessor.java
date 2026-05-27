/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.gamebai.entities.PokerFreeTicketStatistic
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.gamebai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.gamebai.entities.PokerFreeTicketStatistic;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class GetPokerFreeTicketProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String nickname = request.getParameter("nn");
            int createType = Integer.parseInt(request.getParameter("cty"));
            int amount = Integer.parseInt(request.getParameter("am"));
            int status = Integer.parseInt(request.getParameter("st"));
            int isBot = Integer.parseInt(request.getParameter("bot"));
            int isUse = Integer.parseInt(request.getParameter("use"));
            int tourId = Integer.parseInt(request.getParameter("tid"));
            int tourType = Integer.parseInt(request.getParameter("tty"));
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            int page = Integer.parseInt(request.getParameter("p"));
            int pageSize = Integer.parseInt(request.getParameter("s"));
            if (page > 0 && pageSize > 0) {
                GameTourServiceImpl ser = new GameTourServiceImpl();
                PokerFreeTicketStatistic model = ser.getPokerFreeTicketStatistic(id, nickname, createType, amount, status, isBot, isUse, tourId, tourType, startTime, endTime, page, pageSize);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString((Object)model);
            }
            return "";
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}


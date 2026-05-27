/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.gamebai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetPokerTicketProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        List listTicket = new ArrayList();
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String nickname = request.getParameter("nn");
            if (nickname != null && !nickname.isEmpty()) {
                GameTourServiceImpl ser = new GameTourServiceImpl();
                listTicket = ser.getPokerFreeTicket(nickname);
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(listTicket);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}


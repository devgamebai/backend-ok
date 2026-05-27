/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.ad;

import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class BotCaoThapProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String cards = request.getParameter("cr");
        long betValue = Long.parseLong(request.getParameter("bv"));
        long prize = Long.parseLong(request.getParameter("pz"));
        CaoThapServiceImpl service = new CaoThapServiceImpl();
        service.insertBotEvent(nickname, betValue, prize, cards);
        return "success";
    }
}


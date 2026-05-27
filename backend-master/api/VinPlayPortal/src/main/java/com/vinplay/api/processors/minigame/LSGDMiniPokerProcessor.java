/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.minipoker.LSGDMiniPoker
 *  com.vinplay.dal.service.impl.MiniPokerServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LSGDMiniPokerResponse;
import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.dal.service.impl.MiniPokerServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LSGDMiniPokerProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LSGDMiniPokerResponse response = new LSGDMiniPokerResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        int page = Integer.parseInt(request.getParameter("p"));
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        MiniPokerServiceImpl service = new MiniPokerServiceImpl();
        try {
            List results = service.getLichSuGiaoDich(username, page, moneyType);
            response.setTotalPages(100);
            response.setResults(results);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return response.toJson();
    }
}


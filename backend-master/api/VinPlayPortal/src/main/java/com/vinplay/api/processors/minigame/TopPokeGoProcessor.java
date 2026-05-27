/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.PokeGoServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.TopPokeGoResponse;
import com.vinplay.dal.service.impl.PokeGoServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopPokeGoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        TopPokeGoResponse response = new TopPokeGoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        int page = Integer.parseInt(request.getParameter("p"));
        PokeGoServiceImpl service = new PokeGoServiceImpl();
        List results = service.getTopPokeGo(moneyType, page);
        response.setTotalPages(100);
        response.setResults(results);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


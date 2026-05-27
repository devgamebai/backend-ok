/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.SlotMachineServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.slot;

import com.vinplay.api.processors.minigame.response.TopPokeGoResponse;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopKhoBauProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        TopPokeGoResponse response = new TopPokeGoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        int page = Integer.parseInt(request.getParameter("p"));
        String gameName = Games.KHO_BAU.getName();
        if (request.getParameter("gn") != null) {
            gameName = request.getParameter("gn");
        }
        SlotMachineServiceImpl service = new SlotMachineServiceImpl();
        List results = service.getTopSlotMachine(gameName, page);
        response.setTotalPages(5);
        response.setResults(results);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


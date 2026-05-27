/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.BauCuaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.TopToiChonCaResponse;
import com.vinplay.dal.service.impl.BauCuaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopToiChonCaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        TopToiChonCaResponse response = new TopToiChonCaResponse(false, "1001");
        String date = request.getParameter("date");
        String startDate = String.valueOf(date) + " 00:00:00";
        String endDate = String.valueOf(date) + " 23:59:59";
        BauCuaServiceImpl service = new BauCuaServiceImpl();
        List results = service.getTopToiChonCa(startDate, endDate);
        response.setResults(results);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


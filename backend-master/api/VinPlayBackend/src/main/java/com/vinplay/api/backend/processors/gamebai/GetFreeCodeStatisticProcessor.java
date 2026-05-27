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
package com.vinplay.api.backend.processors.gamebai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class GetFreeCodeStatisticProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String gamename = request.getParameter("gn");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            int timeType = Integer.parseInt(request.getParameter("type"));
            GameTourServiceImpl ser = new GameTourServiceImpl();
            Map statistics = ser.getFreeCodeStatistic(gamename, startTime, endTime, timeType);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)statistics);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}


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
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetFreeCodePackageProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String gamename = request.getParameter("gn");
            int amount = Integer.parseInt(request.getParameter("am"));
            int codeType = Integer.parseInt(request.getParameter("ct"));
            String actor = request.getParameter("act");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            GameTourServiceImpl ser = new GameTourServiceImpl();
            List packages = ser.getFreeCodePackage(id, gamename, amount, codeType, startTime, endTime, actor);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)packages);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}


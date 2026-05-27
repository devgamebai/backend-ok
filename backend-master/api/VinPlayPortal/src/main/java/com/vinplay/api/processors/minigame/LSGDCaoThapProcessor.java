/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.LSGDCaoThap
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LSGDCaoThapResponse;
import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LSGDCaoThapProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LSGDCaoThapResponse response = new LSGDCaoThapResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        int page = Integer.parseInt(request.getParameter("p"));
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        CaoThapServiceImpl service = new CaoThapServiceImpl();
        try {
            int totalRows = service.countLichSuGiaoDich(nickname, moneyType);
            List results = service.getLichSuGiaoDich(nickname, page, moneyType);
            response.setTotalPages(totalRows / 10 + 1);
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


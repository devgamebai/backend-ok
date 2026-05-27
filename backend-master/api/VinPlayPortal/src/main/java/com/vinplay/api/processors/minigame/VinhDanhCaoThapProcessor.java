/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.VinhDanhCaoThap
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.VinhDanhCaoThapResponse;
import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class VinhDanhCaoThapProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        VinhDanhCaoThapResponse response = new VinhDanhCaoThapResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        int page = Integer.parseInt(request.getParameter("p"));
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        CaoThapServiceImpl service = new CaoThapServiceImpl();
        try {
            int totalRows = 999;
            List results = service.getBangVinhDanh(page, moneyType);
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


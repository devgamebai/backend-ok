/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.PokerTourInfoDetail
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.PokerTourDetailResponse;
import com.vinplay.gamebai.entities.PokerTourInfoDetail;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class GetPokerTourDetailProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        PokerTourDetailResponse response = new PokerTourDetailResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            int tourId = Integer.parseInt(request.getParameter("tid"));
            GameTourServiceImpl ser = new GameTourServiceImpl();
            response.setTour(ser.getPokerTourDetail(tourId));
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return response.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.UserTourModel
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.LogGameTourResponse;
import com.vinplay.gamebai.entities.UserTourModel;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LogGameTourProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LogGameTourResponse res = new LogGameTourResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gamename = request.getParameter("gn");
        String nickname = request.getParameter("nn");
        String page = request.getParameter("p");
        try {
            if (!(gamename == null || gamename.isEmpty() || nickname == null || nickname.isEmpty() || page == null || page.isEmpty())) {
                int pageNumber = Integer.parseInt(page);
                int skip = 10;
                int rowStart = (pageNumber - 1) * 10;
                GameTourServiceImpl ser = new GameTourServiceImpl();
                res.setTotalPages(10);
                res.setTours(ser.getLogUserTour(gamename, nickname, rowStart, 10));
                res.setSuccess(true);
                res.setErrorCode("0");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res.toJson();
    }
}


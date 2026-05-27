/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.TopCaoThap
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
 *  com.vinplay.usercore.service.impl.UserExtraServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.entities.CaoThapPrizes;
import com.vinplay.api.processors.minigame.response.TopCaoThapResponse;
import com.vinplay.api.utils.CaoThapUtils;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.usercore.service.impl.UserExtraServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopCaoThapProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        TopCaoThapResponse response = new TopCaoThapResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        int type = Integer.parseInt(request.getParameter("type"));
        String date = request.getParameter("date");
        String month = request.getParameter("month");
        if (type == 0 && date != null || type == 1 && month != null) {
            CaoThapServiceImpl service = new CaoThapServiceImpl();
            String startTime = "";
            String endTime = "";
            try {
                if (type == 0) {
                    startTime = String.valueOf(date) + " 00:00:00";
                    endTime = String.valueOf(date) + " 23:59:59";
                } else {
                    startTime = String.valueOf(month) + "-01 00:00:00";
                    endTime = String.valueOf(month) + "-31 23:59:59";
                }
                List results = service.geTopCaoThap(startTime, endTime);
                ArrayList<TopCaoThap> res = new ArrayList<TopCaoThap>();
                UserExtraServiceImpl ser = new UserExtraServiceImpl();
                String platform = ser.getPlatformFromToken(request.getParameter("at"));
                for (int i = 0; i < results.size(); ++i) {
                    TopCaoThap tcp = (TopCaoThap)results.get(i);
                    tcp.stt = i + 1;
                    tcp.prize = CaoThapUtils.getPrize(type, i + 1, platform);
                    res.add(tcp);
                }
                response.setResults(res);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (Exception e) {
                return e.getMessage();
            }
        }
        return response.toJson();
    }
}


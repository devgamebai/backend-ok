/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.TaiXiuLiveServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LichSuGiaoDichTXResponse;
import com.vinplay.dal.service.impl.TaiXiuLiveServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LichSuGiaoDichTXLiveProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LichSuGiaoDichTXResponse response = new LichSuGiaoDichTXResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        boolean isMD5 = false;
        try {
            String type = request.getParameter("type");
            isMD5 = type.equals("md5");
        }
        catch (Exception type) {
            // empty catch block
        }
        TaiXiuLiveServiceImpl service = new TaiXiuLiveServiceImpl();
        try {
            List trans = service.getLichSuGiaoDich(username, page, moneyType);
            int totalPages = 10;
            response.setTotalPages(10);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


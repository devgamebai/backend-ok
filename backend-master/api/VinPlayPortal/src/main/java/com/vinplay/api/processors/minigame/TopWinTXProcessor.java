/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.TopWinTXResponse;
import com.vinplay.dal.service.impl.TaiXiuServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.TopWin;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopWinTXProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        int moneyType = Integer.parseInt(request.getParameter("mt"));
        TaiXiuServiceImpl service = new TaiXiuServiceImpl();
        TopWinTXResponse response = new TopWinTXResponse(false, "1001");
        try {
            List result = service.getTopWin(moneyType);
            response.setTopTX(result);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.BauCuaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.TopBauCuaResponse;
import com.vinplay.dal.service.impl.BauCuaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.TopWin;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopBauCuaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        TopBauCuaResponse response = new TopBauCuaResponse(false, "1001");
        byte moneyType = Byte.parseByte(request.getParameter("mt"));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = df.format(new Date());
        String startDate = String.valueOf(currentDate) + " 00:00:00";
        String endDate = String.valueOf(currentDate) + " 23:59:59";
        BauCuaServiceImpl service = new BauCuaServiceImpl();
        List result = service.getTopBauCua(moneyType, startDate, endDate);
        response.setTopBC(result);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


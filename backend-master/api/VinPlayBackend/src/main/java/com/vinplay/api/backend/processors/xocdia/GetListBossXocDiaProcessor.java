/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.BossXocDiaModel
 *  com.vinplay.usercore.service.impl.XocDiaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.xocdia;

import com.vinplay.api.backend.response.BossXocDiaResponse;
import com.vinplay.gamebai.entities.BossXocDiaModel;
import com.vinplay.usercore.service.impl.XocDiaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetListBossXocDiaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        BossXocDiaResponse res = new BossXocDiaResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String nickname = request.getParameter("nn");
            int roomId = Integer.parseInt(request.getParameter("r"));
            int status = Integer.parseInt(request.getParameter("st"));
            int moneyBet = Integer.parseInt(request.getParameter("mn"));
            XocDiaServiceImpl ser = new XocDiaServiceImpl();
            res.setBossList(ser.getListRoomBoss(nickname, roomId, status, moneyBet));
            res.setSuccess(true);
            res.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res.toJson();
    }
}


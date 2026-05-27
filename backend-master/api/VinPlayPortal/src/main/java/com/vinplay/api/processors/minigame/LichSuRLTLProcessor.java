/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LichSuRLTLResponse;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.dal.service.impl.TaiXiuServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LichSuRLTLProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        int type = Integer.parseInt(request.getParameter("type"));
        LichSuRLTLResponse response = new LichSuRLTLResponse(false, "1001");
        TaiXiuServiceImpl service = new TaiXiuServiceImpl();
        if (type == 0) {
            List vinhDanh = service.getVinhDanhTanLoc();
            List xepHang = service.getXepHangTanLoc();
            long myMoney = service.getSoTienTanLoc(username);
            response.setVinhDanh(vinhDanh);
            response.setXepHang(xepHang);
            response.setMyMoney(myMoney);
            response.setSuccess(true);
            response.setErrorCode("0");
        } else if (type == 1) {
            List vinhDanh = service.getVinhDanhRutLoc();
            List xepHang = service.getXepHangRutLoc();
            long myMoney = service.getSoTienRutLoc(username);
            response.setVinhDanh(vinhDanh);
            response.setXepHang(xepHang);
            response.setMyMoney(myMoney);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        return response.toJson();
    }
}


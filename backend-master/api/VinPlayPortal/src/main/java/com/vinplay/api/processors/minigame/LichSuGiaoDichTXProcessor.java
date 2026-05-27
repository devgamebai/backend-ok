/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiu
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LichSuGiaoDichTXResponse;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.TaiXiuMD5ServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LichSuGiaoDichTXProcessor
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
        }catch(Exception e){
        }
        TaiXiuService service = new TaiXiuServiceImpl();
        if(isMD5)
            service = new TaiXiuMD5ServiceImpl();
        List trans;
        try {
            trans = service.getLichSuGiaoDich(username, page, moneyType);
        } catch (java.sql.SQLException e) {
            trans = new java.util.ArrayList();
        }
        int totalPages = 10;
        response.setTotalPages(10);
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


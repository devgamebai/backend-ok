/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.BauCuaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.LSGDBauCuaResponse;
import com.vinplay.dal.service.impl.BauCuaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LSGDBauCuaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LSGDBauCuaResponse response = new LSGDBauCuaResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        byte moneyType = Byte.parseByte(request.getParameter("mt"));
        BauCuaServiceImpl service = new BauCuaServiceImpl();
        List transactions = service.getLSGDBauCua(username, page, moneyType);
        int totalPages = service.countLSGDBauCua(username, moneyType);
        if (transactions != null) {
            response.setErrorCode("0");
            response.setSuccess(true);
            response.setTotalPages(totalPages);
            response.setTransactions(transactions);
        }
        return response.toJson();
    }
}


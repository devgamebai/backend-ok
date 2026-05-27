/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.HistorySicboResponse;
import com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class HistorySicBoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public String execute(Param<HttpServletRequest> param) {
        HistorySicboResponse response = new HistorySicboResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        String pageStr = request.getParameter("p");
        String moneyTypeStr = request.getParameter("mt");  // FIX: đọc coin type (1=VND, 0=xu)
        int page = 1;
        if (pageStr != null && (page = Integer.parseInt(pageStr)) < 1) {
            page = 1;
        }
        int moneyType = 1; // default: VND coin
        if (moneyTypeStr != null && !moneyTypeStr.isEmpty()) {
            try { moneyType = Integer.parseInt(moneyTypeStr); } catch (NumberFormatException ignored) {}
        }
        TaiXiuSicboServiceImpl service = new TaiXiuSicboServiceImpl();
        try {
            List result = service.getHistorySicbo(username, page, moneyType);  // FIX: pass moneyType
            response.setHistorySicbos(result);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


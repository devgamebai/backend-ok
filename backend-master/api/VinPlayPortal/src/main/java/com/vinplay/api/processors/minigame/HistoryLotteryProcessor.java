/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LoDeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.HistoryLotteryResponse;
import com.vinplay.dal.service.impl.LoDeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class HistoryLotteryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public String execute(Param<HttpServletRequest> param) {
        HistoryLotteryResponse response = new HistoryLotteryResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        LoDeServiceImpl service = new LoDeServiceImpl();
        List result = service.getLotteryTicketByUserName(username);
        response.setHistoryLottery(result);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}


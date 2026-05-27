/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.vqmm.LuckyVipHistory
 *  com.vinplay.usercore.service.impl.LuckyServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.vqvip;

import com.vinplay.api.processors.minigame.response.LuckyVipHistoryResponse;
import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.usercore.service.impl.LuckyServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LuckyVipHistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LuckyVipHistoryResponse response = new LuckyVipHistoryResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String nickname = request.getParameter("nn");
            int page = Integer.parseInt(request.getParameter("p"));
            logger.debug(("Request LuckyVipHistory: \n - nickname: " + nickname));
            if (nickname != null) {
                int totalRows = 99;
                LuckyServiceImpl luckySer = new LuckyServiceImpl();
                List results = luckySer.getLuckyVipHistory(nickname, page);
                response.setTotalPages(10);
                response.setResults(results);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return response.toJson();
    }
}


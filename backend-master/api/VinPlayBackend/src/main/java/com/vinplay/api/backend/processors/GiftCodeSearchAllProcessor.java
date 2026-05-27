/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeSearchResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeSearchResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeSearchAllProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        GiftCodeSearchResponse response = new GiftCodeSearchResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String giftCode = request.getParameter("gc");
        String price = request.getParameter("gp");
        String source = request.getParameter("gs");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String usegift = request.getParameter("ug");
        String type = request.getParameter("type");
        String release = request.getParameter("rl");
        String timeType = request.getParameter("tt");
        String block = request.getParameter("bl");
        int page = Integer.parseInt(request.getParameter("p"));
        int total = Integer.parseInt(request.getParameter("tr"));
        if (page < 0) {
            return response.toJson();
        }
        if (total < 0) {
            return response.toJson();
        }
        GiftCodeServiceImpl service = new GiftCodeServiceImpl();
        try {
            List trans = service.searchAllGiftCode(nickName, giftCode, price, source, timeStart, timeEnd, moneyType, usegift, page, total, type, release, timeType, block);
            long totalPages = 0L;
            long totalRecord = 10000L;
            totalPages = 200L;
            response.setTotal(totalPages);
            response.setTotalRecord(10000L);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeGameSearchResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeGameSearchResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class searchAllGiftCodeGameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        GiftCodeGameSearchResponse response = new GiftCodeGameSearchResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String userName = request.getParameter("un");
        String block = request.getParameter("bl");
        String giftCode = request.getParameter("gc");
        String surfing = request.getParameter("sf");
        String source = request.getParameter("gs");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String usegift = request.getParameter("ug");
        int page = Integer.parseInt(request.getParameter("p"));
        int total = Integer.parseInt(request.getParameter("tr"));
        if (page < 0) {
            return response.toJson();
        }
        if (total < 0) {
            return response.toJson();
        }
        GiftCodeGameServiceImpl service = new GiftCodeGameServiceImpl();
        try {
            List trans = service.searchAllGiftCode(nickName, giftCode, surfing, source, timeStart, timeEnd, userName, block, usegift, page, total);
            long totalRecord = service.countSearchAllGiftCode(nickName, giftCode, surfing, source, timeStart, timeEnd, userName, block, usegift);
            long totalPages = 0L;
            totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
            response.setTotal(totalPages);
            response.setTotalRecord(totalRecord);
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


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ToolReportBySourceResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ToolReportBySourceResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeToolReportBySourceProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ToolReportBySourceResponse response = new ToolReportBySourceResponse(false, "10001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String source = request.getParameter("gs");
        String type = request.getParameter("type");
        String timeType = request.getParameter("tt");
        String block = request.getParameter("bl");
        int page = Integer.parseInt(request.getParameter("p"));
        if (source != null && moneyType != null) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                List trans = service.ToolReportGiftCodeBySource(source, timeStart, timeEnd, moneyType, type, page, timeType, block);
                response.setTotal(1000L);
                response.setTransactions(trans);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}


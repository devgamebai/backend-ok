/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ReportGiftCodeResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ReportGiftCodeResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeToolReportProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String source = request.getParameter("gs");
        String timeType = request.getParameter("tt");
        String block = request.getParameter("bl");
        String json = "";
        if (moneyType != null && nickName != null) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                List giftcode = service.ToolReportGiftCode(nickName, source, timeStart, timeEnd, moneyType, timeType, block);
                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writerWithType((TypeReference)new TypeReference<List<ReportGiftCodeResponse>>(){}).writeValueAsString((Object)giftcode);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
            return json;
        }
        return "MISSING PARAMETTER";
    }

}


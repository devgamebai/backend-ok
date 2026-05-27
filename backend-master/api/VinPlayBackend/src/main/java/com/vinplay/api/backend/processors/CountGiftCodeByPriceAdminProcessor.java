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
 *  com.vinplay.vbee.common.response.GiftCodeCountResponse
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
import com.vinplay.vbee.common.response.GiftCodeCountResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class CountGiftCodeByPriceAdminProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String price = request.getParameter("gp");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String type = request.getParameter("type");
        String block = request.getParameter("bl");
        String json = "";
        if (moneyType != null && block != null) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                List giftcode = service.countGiftCodeByPriceAdmin(price, timeStart, timeEnd, moneyType, type, block);
                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writerWithType((TypeReference)new TypeReference<List<GiftCodeCountResponse>>(){}).writeValueAsString((Object)giftcode);
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


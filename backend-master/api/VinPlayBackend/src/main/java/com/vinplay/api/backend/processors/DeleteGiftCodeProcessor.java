/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeDeleteResponse
 *  com.vinplay.vbee.common.response.ResultGiftCodeDeleteResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeDeleteResponse;
import com.vinplay.vbee.common.response.ResultGiftCodeDeleteResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class DeleteGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultGiftCodeDeleteResponse response = new ResultGiftCodeDeleteResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String source = request.getParameter("gs");
        String price = request.getParameter("gp");
        String startDate = request.getParameter("ts");
        String endDate = request.getParameter("te");
        if (startDate != null && !startDate.equals("") && endDate != null && !endDate.equals("")) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                GiftCodeDeleteResponse trans = service.DeleteGiftCode(startDate, endDate, source, price);
                response.setErrorCode("0");
                response.setSuccess(true);
                response.setTransactions(trans);
            }
            catch (Exception e) {
                logger.debug((Object)e);
                e.printStackTrace();
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeRestoreResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeRestoreResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeRestoreProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        GiftCodeRestoreResponse response = new GiftCodeRestoreResponse(false, "1001");
        String gia = request.getParameter("gp");
        String nguon = request.getParameter("gs");
        String dotphathanh = request.getParameter("gl");
        String giftcode = request.getParameter("gc");
        if (giftcode != null && !giftcode.equals("")) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                boolean check = service.RestoreGiftCode(gia, nguon, giftcode, dotphathanh);
                if (check) {
                    response.setErrorCode("0");
                    response.setSuccess(true);
                } else {
                    response.setErrorCode("10001");
                    response.setGiftCode(giftcode);
                    response.setSuccess(false);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}


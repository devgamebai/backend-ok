/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.GiftCodeGameResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeGameResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class exportGiftCodeGameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String surfing = request.getParameter("sf");
        String soluong = request.getParameter("gq");
        String nguon = request.getParameter("gs");
        String dotphathanh = request.getParameter("gl");
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (!(surfing == null || surfing.equals("") || soluong == null || soluong.equals("") || nguon == null || soluong.equals("") || dotphathanh == null || dotphathanh.equals(""))) {
            try {
                GiftCodeGameServiceImpl service = new GiftCodeGameServiceImpl();
                GiftCodeGameResponse msg = new GiftCodeGameResponse("", "G" + dotphathanh, nguon, surfing, Integer.parseInt(soluong));
                boolean check = service.exportGiftCode(msg);
                if (check) {
                    response.setErrorCode("0");
                    response.setSuccess(true);
                } else {
                    response.setErrorCode("10003");
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


/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;

public class BlockGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String giftcode = request.getParameter("gc");
        String block = request.getParameter("bl");
        GiftCodeGameServiceImpl service = new GiftCodeGameServiceImpl();
        try {
            if (giftcode != null && !giftcode.equals("")) {
                service.blockGiftCode(giftcode, block);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}


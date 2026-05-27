/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeUploadResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeUploadResponse;
import javax.servlet.http.HttpServletRequest;

public class UploadGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        GiftCodeUploadResponse response = new GiftCodeUploadResponse(false, "1001", "");
        String nickName = request.getParameter("nn");
        String vin = request.getParameter("vin");
        String xu = request.getParameter("xu");
        if (nickName != null) {
            GiftCodeServiceImpl service = new GiftCodeServiceImpl();
            String check = service.uploadFileGiftCode(nickName, Long.parseLong(vin), Long.parseLong(xu));
            if (check.equals("success")) {
                response.setErrorCode("0");
                response.setSuccess(true);
            } else if (check.equals("notexits")) {
                response.setErrorCode("10002");
            } else {
                response.setErrorCode("10001");
                response.setMessage(check);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}


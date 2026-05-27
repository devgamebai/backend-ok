/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class CountGiftCodeByPriceProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        GiftcodeStatisticResponse response = new GiftcodeStatisticResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String source = request.getParameter("gs");
        String timeType = request.getParameter("tt");
        GiftCodeServiceImpl service = new GiftCodeServiceImpl();
        List trans = service.thongKeGiftcodeDaXuat(source, timeStart, timeEnd, moneyType, timeType);
        response.setSuccess(true);
        response.setErrorCode("0");
        response.setTrans(trans);
        return response.toJson();
    }
}


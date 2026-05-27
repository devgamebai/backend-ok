/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String gia = request.getParameter("gp");
        String soluong = request.getParameter("gq");
        String source = request.getParameter("gs");
        String dotphathanh = request.getParameter("gl");
        String moneyType = request.getParameter("mt");
        String type = request.getParameter("type");
        if (!(gia == null || gia.equals("") || soluong == null || soluong.equals("") || dotphathanh == null || dotphathanh.equals("") || source == null || source.equals("") || moneyType == null || moneyType.equals(""))) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                GiftCodeMessage msg = new GiftCodeMessage("", gia, Integer.parseInt(soluong), source, 1, 1, Integer.parseInt(moneyType), dotphathanh, type, "");
                boolean check = service.xuatGiftCode(msg);
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


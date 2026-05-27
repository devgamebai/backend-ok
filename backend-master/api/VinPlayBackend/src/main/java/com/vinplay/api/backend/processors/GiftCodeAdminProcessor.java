/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeAdminProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gia = request.getParameter("gp");
        String soluong = request.getParameter("gq");
        String dotphathanh = request.getParameter("gl");
        String source = request.getParameter("gs");
        String moneyType = request.getParameter("mt");
        String type = request.getParameter("type");
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (!(gia == null || gia.equals("") || soluong == null || soluong.equals("") || dotphathanh == null || dotphathanh.equals("") || source == null || source.equals("") || moneyType == null || moneyType.equals(""))) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                VinPlayUtils.loadGiftcode((List)service.loadAllGiftcode());
                String str = String.valueOf(dotphathanh) + gia + source;
                int leng = 12 - str.length();
                String giftCode = "";
                GiftCodeMessage msg = new GiftCodeMessage();
                for (int i = 0; i < Integer.parseInt(soluong); ++i) {
                    giftCode = VinPlayUtils.genGiftCode((int)leng);
                    msg = new GiftCodeMessage(giftCode.toUpperCase(), gia, Integer.parseInt(soluong), source, 1, 1, Integer.parseInt(moneyType), dotphathanh, type, "");
                    service.genGiftCode(msg);
                }
                response.setErrorCode("0");
                response.setSuccess(true);
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

